package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import com.dip.pingtest.domain.model.enums.Role
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.PingTimeRepository
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.TimePingIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeItemDTO
import com.dip.pingtest.infrastructure.dto.PingTimeUpdateDTO
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.NoSuchElementException

@Service
@Transactional
class PingTimeService(
    private val pingTimeRepository: PingTimeRepository,
    private val componentRepository: ComponentRepository,
    private val userRepository: UserRepository,
    private val componentService: ComponentService
) {

    private fun getCurrentUserId(): Int {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Int
    }

    private fun getCurrentRole(): Role {
        val auth = SecurityContextHolder.getContext().authentication
        val authority = auth.authorities.first().authority
        return Role.valueOf(authority.replace("ROLE_", ""))
    }

    fun getTimePingIcon(): TimePingIconDTO {
        val draft = pingTimeRepository.findByCreatorIdAndStatus(getCurrentUserId(), PingTimeStatus.DRAFT)
        return TimePingIconDTO(draft?.id, draft?.items?.size ?: 0)
    }

    fun getTimePings(status: String?, fromDate: String?, toDate: String?): List<PingTimeDTO> {
        val excludedStatuses = listOf(PingTimeStatus.DELETED, PingTimeStatus.DRAFT)
        val from = fromDate?.let { LocalDateTime.parse(it) }
        val to = toDate?.let { LocalDateTime.parse(it) }
        var pingTimes = pingTimeRepository.findAllByStatusNotInAndFormationDateBetween(excludedStatuses, from, to)
        status?.let { s ->
            val enumStatus = PingTimeStatus.valueOf(s.uppercase())
            pingTimes = pingTimes.filter { it.status == enumStatus }
        }
        if (getCurrentRole() == Role.USER) {
            pingTimes = pingTimes.filter { it.creator.id == getCurrentUserId() }
        }
        return pingTimes.map { toDTO(it) }
    }

    fun getTimePing(id: Int): PingTimeDTO {
        val pingTime = pingTimeRepository.findById(id).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (pingTime.status == PingTimeStatus.DELETED) throw RuntimeException("Ping Time Request deleted")
        if (getCurrentRole() == Role.USER && pingTime.creator.id != getCurrentUserId()) {
            throw AccessDeniedException("Access denied: Not the creator")
        }
        return toDTO(pingTime)
    }

    fun getTimePingDomain(id: Int): PingTime? {
        val pingTime = pingTimeRepository.findById(id).orElse(null)
        if (pingTime != null) {
            if (getCurrentRole() == Role.USER && pingTime.creator.id != getCurrentUserId()) {
                throw AccessDeniedException("Access denied: Not the creator")
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
        val request = pingTimeRepository.findById(id).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT) {
            throw RuntimeException("Only draft can be formed")
        }
        if (request.items.isEmpty()) throw RuntimeException("Cannot form empty Ping Time requests")
        request.status = PingTimeStatus.FORMED
        request.formationDate = LocalDateTime.now()
        request.moderator = userRepository.findUserById(getCurrentUserId())
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun moderateTimePing(id: Int, action: String): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.FORMED) throw RuntimeException("Only formed can be moderated")
        request.completionDate = LocalDateTime.now()
        request.moderator = userRepository.findById(getCurrentUserId()).orElseThrow()
        when (action.uppercase()) {
            "COMPLETE" -> {
                recalculateTotal(request)
                request.status = PingTimeStatus.COMPLETED
            }
            "REJECT" -> {
                request.status = PingTimeStatus.REJECTED
            }
            else -> throw RuntimeException("Invalid action: $action")
        }
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun deleteTimePing(id: Int) {
        val request = pingTimeRepository.findById(id).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (request.creator.id != getCurrentUserId() || (request.status != PingTimeStatus.DRAFT && request.status != PingTimeStatus.FORMED)) {
            throw AccessDeniedException("Only creator can delete draft or formed")
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
        val component = componentRepository.findById(componentId).orElseThrow { NoSuchElementException("Server Component not found") }
        var request = pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)
        val creator = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
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
        val request = pingTimeRepository.findById(requestId).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != getCurrentUserId()) throw AccessDeniedException("Can only update own draft items")
        val item = request.items.find { it.component.id == componentId } ?: throw NoSuchElementException("Item not found")
        dto.quantity?.let { item.quantity = it }
        recalculateTotal(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun deleteItem(requestId: Int, componentId: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { NoSuchElementException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != getCurrentUserId()) throw AccessDeniedException("Can only delete from own draft")
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
        val pingTime = pingTimeRepository.findById(id).orElseThrow { NoSuchElementException("PingTime not found") }
        if (pingTime.creator.id != getCurrentUserId() && getCurrentRole() != Role.MODERATOR) {
            throw AccessDeniedException("Access denied")
        }
        dto.loadCoefficient?.let {
            pingTime.loadCoefficient = it
        }
        recalculateTotal(pingTime)
        pingTimeRepository.save(pingTime)
        return toDTO(pingTime)
    }

    private fun recalculateTotal(pingTime: PingTime) {
        val baseTime = calculateBaseTime(pingTime)
        val multiplier = pingTime.loadCoefficient ?: 1
        pingTime.totalTime = baseTime * multiplier
    }

    private fun toDTO(request: PingTime): PingTimeDTO = PingTimeDTO(
        id = request.id,
        status = request.status.name,
        createdAt = request.createdAt.toString(),
        creatorUsername = request.creator.username,
        formationDate = request.formationDate?.toString(),
        completionDate = request.completionDate?.toString(),
        moderatorUsername = request.moderator?.username,
        totalTime = request.totalTime,
        items = request.items.map {
            PingTimeItemDTO(
                componentId = it.component.id,
                title = it.component.title,
                description = it.component.description,
                time = it.component.time,
                imageUrl = componentService.generatePresignedUrl(it.component.image),
                quantity = it.quantity,
                subtotalTime = calculateSubtotal(it.component, it.quantity, request.creator.id, it.component.id)
            )
        },
        loadCoefficient = request.loadCoefficient
    )
}