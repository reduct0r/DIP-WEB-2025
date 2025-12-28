package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import com.dip.pingtest.domain.model.enums.Role
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.PingTimeRepository
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.TimePingIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.PaginatedResponseDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeItemDTO
import com.dip.pingtest.infrastructure.dto.PingTimeUpdateDTO
import com.dip.pingtest.config.IndexConfig
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.nio.charset.StandardCharsets

@Service
@Transactional
class PingTimeService(
    private val pingTimeRepository: PingTimeRepository,
    private val componentRepository: ComponentRepository,
    private val userRepository: UserRepository,
    private val componentService: ComponentService,
    private val objectMapper: ObjectMapper,
    private val indexConfig: IndexConfig
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    private val restTemplate: RestTemplate = RestTemplate().apply {
        messageConverters.add(StringHttpMessageConverter(StandardCharsets.UTF_8))
        messageConverters.add(MappingJackson2HttpMessageConverter())
    }
    private val asyncServiceUrl = "http://localhost:8000/api/ping-time/async-calculate"

    private fun getCurrentUserId(): Int {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Int
    }

    private fun getCurrentRole(): Role {
        val auth = SecurityContextHolder.getContext().authentication
        val authority = auth.authorities.first().authority
        return Role.valueOf(authority.replace("ROLE_", ""))
    }
    
    private fun isCurrentUserModerator(): Boolean {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
        return user?.isModerator ?: false
    }

    fun getTimePingIcon(): TimePingIconDTO {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            return TimePingIconDTO(-1, 0)
        }
        val draft = pingTimeRepository.findByCreatorIdAndStatus(getCurrentUserId(), PingTimeStatus.DRAFT)
        return TimePingIconDTO(draft?.id ?: -1, draft?.items?.size ?: 0)
    }

    fun getTimePings(status: String?, fromDate: String?, toDate: String?): List<PingTimeDTO> {
        val excludedStatuses = listOf(PingTimeStatus.DELETED, PingTimeStatus.DRAFT)
        val from = fromDate?.let { LocalDateTime.parse(it) }
        val to = toDate?.let { LocalDateTime.parse(it) }

        var pingTimes: List<PingTime> = when {
            from == null && to == null -> pingTimeRepository.findAllByStatusNotIn(excludedStatuses)
            from == null -> pingTimeRepository.findAllByStatusNotInAndFormationDateLessThanEqual(excludedStatuses, to!!)
            to == null -> pingTimeRepository.findAllByStatusNotInAndFormationDateGreaterThanEqual(excludedStatuses, from)
            else -> pingTimeRepository.findAllByStatusNotInAndFormationDateBetween(excludedStatuses, from, to)
        }

        status?.let { s ->
            val enumStatus = PingTimeStatus.valueOf(s.uppercase())
            pingTimes = pingTimes.filter { it.status == enumStatus }
        }
        if (!isCurrentUserModerator()) {
            pingTimes = pingTimes.filter { it.creator.id == getCurrentUserId() }
        }
        return pingTimes.map { toDTO(it) }
    }

    fun getTimePingsPaginated(
        status: String?,
        fromDate: String?,
        toDate: String?,
        page: Int,
        size: Int,
        sortBy: String = "formationDate",
        sortDir: String = "DESC"
    ): PaginatedResponseDTO<PingTimeDTO> {
        val startTime = System.currentTimeMillis()
        
        // Управление использованием индекса через конфигурацию
        try {
            if (!indexConfig.enabled) {
                // Временно отключаем использование индексов для этого запроса
                entityManager.createNativeQuery("SET LOCAL enable_indexscan = off").executeUpdate()
                entityManager.createNativeQuery("SET LOCAL enable_bitmapscan = off").executeUpdate()
            } else {
                // Включаем использование индексов
                entityManager.createNativeQuery("SET LOCAL enable_indexscan = on").executeUpdate()
                entityManager.createNativeQuery("SET LOCAL enable_bitmapscan = on").executeUpdate()
            }
        } catch (e: Exception) {
            // Игнорируем ошибки, если настройка не поддерживается
            println("Не удалось установить настройки индексов: ${e.message}")
        }
        
        val excludedStatuses = listOf(PingTimeStatus.DELETED, PingTimeStatus.DRAFT)
        val from = fromDate?.let { LocalDateTime.parse(it) }
        val to = toDate?.let { LocalDateTime.parse(it) }

        val sort = Sort.by(
            if (sortDir.uppercase() == "ASC") Sort.Direction.ASC else Sort.Direction.DESC,
            sortBy
        )
        val pageable: Pageable = PageRequest.of(page, size, sort)

        // Определяем статус для фильтрации
        val statusFilter = status?.let { PingTimeStatus.valueOf(it.uppercase()) }
        
        // Получаем заявки с учетом фильтрации по датам и статусу
        // Используем formationDate, если оно есть, иначе createdAt
        var pingTimesPage: Page<PingTime> = when {
            // Без фильтрации по датам - используем методы репозитория для эффективной фильтрации
            from == null && to == null -> {
                if (statusFilter != null) {
                    pingTimeRepository.findAllByStatusNotInAndStatus(excludedStatuses, statusFilter, pageable)
                } else {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses, pageable)
                }
            }
            // С фильтрацией по датам - используем фильтрацию в памяти для учета COALESCE
            else -> {
                // Для фильтрации по датам получаем все записи и фильтруем в памяти
                // Это позволяет учитывать заявки без formationDate (используем createdAt)
                val allRequests = if (statusFilter != null) {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses).filter { it.status == statusFilter }
                } else {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses)
                }
                
                val filteredRequests = allRequests.filter { pingTime ->
                    val dateToCompare = pingTime.formationDate ?: pingTime.createdAt
                    when {
                        from != null && to != null -> {
                            dateToCompare.isAfter(from.minusNanos(1)) && dateToCompare.isBefore(to.plusDays(1))
                        }
                        from != null -> {
                            dateToCompare.isAfter(from.minusNanos(1)) || dateToCompare.isEqual(from)
                        }
                        else -> { // to != null
                            dateToCompare.isBefore(to!!.plusDays(1)) || dateToCompare.isEqual(to)
                        }
                    }
                }
                
                // Применяем сортировку
                val sortedRequests = when (sortDir.uppercase()) {
                    "ASC" -> filteredRequests.sortedBy { 
                        when (sortBy) {
                            "formationDate" -> it.formationDate ?: it.createdAt
                            "createdAt" -> it.createdAt
                            else -> it.formationDate ?: it.createdAt
                        }
                    }
                    else -> filteredRequests.sortedByDescending { 
                        when (sortBy) {
                            "formationDate" -> it.formationDate ?: it.createdAt
                            "createdAt" -> it.createdAt
                            else -> it.formationDate ?: it.createdAt
                        }
                    }
                }
                
                // Применяем пагинацию
                val total = sortedRequests.size.toLong()
                val start = page * size
                val end = minOf(start + size, sortedRequests.size)
                val paginatedContent = if (start < sortedRequests.size) {
                    sortedRequests.subList(start, end)
                } else {
                    emptyList()
                }
                
                org.springframework.data.domain.PageImpl(paginatedContent, pageable, total)
            }
        }

        // Фильтрация по пользователю (только для обычных пользователей, не для модераторов)
        // Проверяем isModerator из БД, а не role
        if (!isCurrentUserModerator()) {
            // Для обычных пользователей показываем только их заявки
            val filteredContent = pingTimesPage.content.filter { it.creator.id == getCurrentUserId() }
            // Пересчитываем totalElements для отфильтрованных результатов
            val allUserRequests = when {
                from != null && to != null -> {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses).filter { pingTime ->
                        pingTime.creator.id == getCurrentUserId() &&
                        (pingTime.formationDate ?: pingTime.createdAt).let { dateToCompare ->
                            dateToCompare.isAfter(from.minusNanos(1)) && dateToCompare.isBefore(to.plusDays(1))
                        }
                    }
                }
                from != null -> {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses).filter { pingTime ->
                        pingTime.creator.id == getCurrentUserId() &&
                        (pingTime.formationDate ?: pingTime.createdAt).let { dateToCompare ->
                            dateToCompare.isAfter(from.minusNanos(1)) || dateToCompare.isEqual(from)
                        }
                    }
                }
                to != null -> {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses).filter { pingTime ->
                        pingTime.creator.id == getCurrentUserId() &&
                        (pingTime.formationDate ?: pingTime.createdAt).let { dateToCompare ->
                            dateToCompare.isBefore(to.plusDays(1)) || dateToCompare.isEqual(to)
                        }
                    }
                }
                else -> {
                    pingTimeRepository.findAllByStatusNotIn(excludedStatuses).filter { 
                        it.creator.id == getCurrentUserId() 
                    }
                }
            }
            val total = allUserRequests.size.toLong()
            pingTimesPage = org.springframework.data.domain.PageImpl(
                filteredContent,
                pageable,
                total
            )
        }
        // Для модераторов (isModerator = true) фильтрация по пользователю не применяется - показываем все заявки

        val queryTime = System.currentTimeMillis() - startTime
        
        // Выводим время запроса в консоль
        println("getPingTimesQuery: ${queryTime} ms")
        
        // Выводим план запроса (EXPLAIN ANALYZE) для демонстрации использования индекса
        try {
            val explainQuery = when {
                // Если фильтруется по конкретному статусу - используем составной индекс
                statusFilter != null && from != null && to != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status = :status
                    AND status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date BETWEEN :from AND :to
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                statusFilter != null && from != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status = :status
                    AND status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date >= :from
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                statusFilter != null && to != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status = :status
                    AND status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date <= :to
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                statusFilter != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status = :status
                    AND status NOT IN ('DELETED', 'DRAFT')
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                from != null && to != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date BETWEEN :from AND :to
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                from != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date >= :from
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                to != null -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status NOT IN ('DELETED', 'DRAFT')
                    AND formation_date <= :to
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
                else -> {
                    """
                    EXPLAIN ANALYZE
                    SELECT * FROM ping_times 
                    WHERE status NOT IN ('DELETED', 'DRAFT')
                    ORDER BY formation_date DESC
                    LIMIT :size OFFSET :offset
                    """
                }
            }
            
            val explainResult = entityManager.createNativeQuery(explainQuery).apply {
                if (statusFilter != null) setParameter("status", statusFilter.name)
                if (from != null) setParameter("from", from)
                if (to != null) setParameter("to", to)
                setParameter("size", size)
                setParameter("offset", page * size)
            }.resultList
            
            println("=== (EXPLAIN ANALYZE) ===")
            println("Index ${if (indexConfig.enabled) "ON" else "OFF"}")
            if (statusFilter != null) {
                println("Status filter: ${statusFilter.name}")
            }
            explainResult.forEach { row ->
                println(row)
            }
            println("=====================================")
        } catch (e: Exception) {
            println("ERROR: ${e.message}")
        }

        return PaginatedResponseDTO(
            content = pingTimesPage.content.map { toDTO(it) },
            totalElements = pingTimesPage.totalElements,
            totalPages = pingTimesPage.totalPages,
            currentPage = pingTimesPage.number,
            pageSize = pingTimesPage.size,
            hasNext = pingTimesPage.hasNext(),
            hasPrevious = pingTimesPage.hasPrevious(),
            queryTimeMs = queryTime
        )
    }

    fun getTimePing(id: Int): PingTimeDTO {
        val pingTime = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (pingTime.status == PingTimeStatus.DELETED) throw RuntimeException("Ping Time Request deleted")
        if (!isCurrentUserModerator() && pingTime.creator.id != getCurrentUserId()) {
            throw RuntimeException("Access denied: Not the creator")
        }
        return toDTO(pingTime)
    }

    fun getTimePingDomain(id: Int): PingTime? {
        val pingTime = pingTimeRepository.findById(id).orElse(null)
        if (pingTime != null) {
            if (!isCurrentUserModerator() && pingTime.creator.id != getCurrentUserId()) {
                throw RuntimeException("Access denied: Not the creator")
            }
            pingTime.items.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
        }
        return pingTime
    }

    fun getDraftTimePingIdForUser(userId: Int): Int? {
        return pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)?.id
    }

    fun getTimePingItemCountForUser(userId: Int): Int {
        return pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)?.items?.size ?: 0
    }

    fun formTimePing(id: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != getCurrentUserId()) {
            throw RuntimeException("Only creator can form draft")
        }
        if (request.items.isEmpty()) throw RuntimeException("Cannot form empty Ping Time requests")
        request.status = PingTimeStatus.FORMED
        request.formationDate = LocalDateTime.now()
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun moderateTimePing(id: Int, action: String): PingTimeDTO {
        // Проверяем, является ли пользователь модератором через isModerator из БД
        if (!isCurrentUserModerator()) {
            throw RuntimeException("Access denied: Only moderators can moderate requests")
        }
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.FORMED) throw RuntimeException("Only formed can be moderated")
        request.completionDate = LocalDateTime.now()
        request.moderator = userRepository.findById(getCurrentUserId()).orElseThrow()
        when (action.uppercase()) {
            "COMPLETE" -> {
                // Рассчитываем базовое время для отправки в асинхронный сервис
                // НО НЕ сохраняем его в БД - оно будет обновлено после получения результата от асинхронного сервиса
                val baseTime = calculateBaseTime(request)
                val multiplier = request.loadCoefficient ?: 1
                val calculatedTotalTime = baseTime * multiplier
                
                // Обнуляем totalTime - оно будет заполнено асинхронным сервисом
                request.totalTime = null
                request.status = PingTimeStatus.COMPLETED
                val saved = pingTimeRepository.save(request)
                
                // Вызываем асинхронный сервис для расчета оптимизированного времени
                // Передаем рассчитанное время, но не сохраняем его в БД
                callAsyncService(saved, calculatedTotalTime)
                
                return toDTO(saved)
            }
            "REJECT" -> {
                request.status = PingTimeStatus.REJECTED
            }
            else -> throw RuntimeException("Invalid action: $action")
        }
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }
    
    private fun callAsyncService(pingTime: PingTime, calculatedTotalTime: Int) {
        try {
            // Копируем значения в локальные переменные для использования в Thread
            val requestId = pingTime.id
            val payload = mapOf(
                "request_id" to requestId,
                "totalTime" to calculatedTotalTime
            )
            
            // Отправляем запрос асинхронно (без ожидания ответа)
            Thread {
                try {
                    // Сериализуем payload в JSON строку
                    val jsonPayload = objectMapper.writeValueAsString(payload)
                    
                    val headers = HttpHeaders()
                    headers.contentType = MediaType.APPLICATION_JSON
                    val httpRequest = HttpEntity(jsonPayload, headers)
                    
                    restTemplate.postForObject(asyncServiceUrl, httpRequest, Map::class.java)
                } catch (e: Exception) {
                    // Логируем ошибки асинхронного сервиса
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            // Логируем ошибки при подготовке запроса
            e.printStackTrace()
        }
    }
    
    fun updateAsyncResults(requestId: Int, token: String, optimizedTotalTime: Int) {
        // Проверка токена
        val expectedToken = "SECRET8B"  // Токен для авторизации (8 байт)
        if (token != expectedToken) {
            throw RuntimeException("Invalid token")
        }
        
        val pingTime = pingTimeRepository.findById(requestId).orElseThrow { 
            RuntimeException("Ping Time Request not found") 
        }
        
        // Обновляем итоговое время заявки оптимизированным значением
        pingTime.totalTime = optimizedTotalTime
        pingTimeRepository.save(pingTime)
    }

    fun deleteTimePing(id: Int) {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.creator.id != getCurrentUserId() || (request.status != PingTimeStatus.DRAFT && request.status != PingTimeStatus.FORMED)) {
            throw RuntimeException("Only creator can delete draft or formed")
        }
        request.status = PingTimeStatus.DELETED
        pingTimeRepository.save(request)
    }

    fun logicalDeleteTimePing(id: Int) {
        deleteTimePing(id)
    }

    fun addServerComponentToDraft(componentId: Int): PingTimeDTO {
        val pingTime = addServerComponentToTimePing(getCurrentUserId(), componentId)
        return toDTO(pingTime)
    }

    fun addServerComponentToTimePing(userId: Int, componentId: Int): PingTime {
        val component = componentRepository.findById(componentId).orElseThrow { RuntimeException("Server Component not found") }
        var request = pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)
        val creator = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        if (request == null) {
            request = PingTime(creator = creator, status = PingTimeStatus.DRAFT)
            request = pingTimeRepository.save(request)
        }
        val existingItem = request.items.find { it.component.id == componentId }
        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            val item = PingTimeComponent(
                pingTimeId = request.id,
                componentId = component.id,
                pingTime = request,
                component = component,
                quantity = 1
            )
            request.items.add(item)
        }
        recalculateTotal(request)
        return pingTimeRepository.save(request)
    }

    fun updateItem(requestId: Int, componentId: Int, dto: ItemUpdateDTO): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != getCurrentUserId()) throw RuntimeException("Can only update own draft items")
        val item = request.items.find { it.component.id == componentId } ?: throw RuntimeException("Item not found")
        dto.quantity?.let { item.quantity = it }
        recalculateTotal(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun deleteItem(requestId: Int, componentId: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != getCurrentUserId()) throw RuntimeException("Can only delete from own draft")
        request.items.removeIf { it.component.id == componentId }
        recalculateTotal(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun calculateSubtotal(component: ServerComponent, quantity: Int, userId: Int, componentId: Int): Int {
        val componentGroup = componentService.getPreference(userId, componentId)
        var subtotal = component.time * quantity
        val dbTime = componentRepository.findByTitle("БД")?.time ?: 0
        val cacheTime = componentRepository.findByTitle("Кэш")?.time ?: 0
        val groups = componentGroup?.split(",") ?: emptyList()
        if ("БД" in groups) subtotal += dbTime
        if ("Кэш" in groups) subtotal += cacheTime
        return subtotal
    }

    private fun calculateBaseTime(request: PingTime): Int {
        return request.items.sumOf { calculateSubtotal(it.component, it.quantity, request.creator.id, it.component.id) }
    }

    fun updateTimePing(id: Int, dto: PingTimeUpdateDTO): PingTimeDTO {
        val pingTime = pingTimeRepository.findById(id).orElseThrow { RuntimeException("PingTime not found") }
        if (pingTime.creator.id != getCurrentUserId() && !isCurrentUserModerator()) {
            throw RuntimeException("Access denied")
        }
        dto.loadCoefficient?.let {
            pingTime.loadCoefficient = it
        }
        recalculateTotal(pingTime)
        pingTimeRepository.save(pingTime)
        return toDTO(pingTime)
    }

    private fun recalculateTotal(pingTime: PingTime) {
        // totalTime рассчитывается только при завершении модератором
        // Для черновиков и сформированных заявок totalTime остается null
        // и будет заполнен асинхронным сервисом после завершения
        if (pingTime.status == PingTimeStatus.COMPLETED && pingTime.totalTime != null) {
            // Если заявка уже завершена и totalTime установлен, пересчитываем только если нужно
            // Но обычно это не нужно, так как totalTime устанавливается асинхронным сервисом
            return
        }
        // Для всех остальных статусов не устанавливаем totalTime
        // Он будет null до завершения модератором
    }

    private fun toDTO(request: PingTime): PingTimeDTO {
        val items = request.items.map {
            PingTimeItemDTO(
                componentId = it.component.id,
                title = it.component.title,
                description = it.component.description,
                time = it.component.time,
                imageUrl = componentService.generatePresignedUrl(it.component.image),
                quantity = it.quantity,
                subtotalTime = calculateSubtotal(it.component, it.quantity, request.creator.id, it.component.id)
            )
        }
        
        val dto = PingTimeDTO(
            id = request.id,
            status = request.status.name,
            createdAt = request.createdAt.toString(),
            creatorUsername = request.creator.username,
            formationDate = request.formationDate?.toString(),
            completionDate = request.completionDate?.toString(),
            moderatorUsername = request.moderator?.username,
            totalTime = request.totalTime,  // Может быть null, если еще не рассчитано асинхронным сервисом
            items = items,
            loadCoefficient = request.loadCoefficient
        )
        return dto
    }
}