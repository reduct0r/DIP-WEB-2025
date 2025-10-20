package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.PingTimeRepository
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.TimePingIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeItemDTO
import com.dip.pingtest.infrastructure.dto.PingTimeUpdateDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class PingTimeService(
    private val pingTimeRepository: PingTimeRepository,
    private val componentRepository: ComponentRepository,
    private val userRepository: UserRepository,
    private val componentService: ComponentService
) {
    companion object {
        const val FIXED_CREATOR_ID = 1
        const val FIXED_MODERATOR_ID = 1
    }
    fun getTimePingIcon(): TimePingIconDTO {
        val draft = pingTimeRepository.findByCreatorIdAndStatus(FIXED_CREATOR_ID, PingTimeStatus.DRAFT)
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
        return pingTimes.map { toDTO(it) }
    }
    fun getTimePing(id: Int): PingTimeDTO {
        val pingTime = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (pingTime.status == PingTimeStatus.DELETED) throw RuntimeException("Ping Time Request deleted")
        return toDTO(pingTime)
    }
    fun getTimePingDomain(id: Int): PingTime? {
        val pingTime = pingTimeRepository.findById(id).orElse(null)
        pingTime?.items?.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
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
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != FIXED_CREATOR_ID) {
            throw RuntimeException("Only creator can form draft")
        }
        if (request.items.isEmpty()) throw RuntimeException("Cannot form empty Ping Time requests")
        request.status = PingTimeStatus.FORMED
        request.formationDate = LocalDateTime.now()
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }
    fun moderateTimePing(id: Int, action: String): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.FORMED) throw RuntimeException("Only formed can be moderated")
        request.completionDate = LocalDateTime.now()
        request.moderator = userRepository.findById(FIXED_MODERATOR_ID).orElseThrow()
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
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.creator.id != FIXED_CREATOR_ID || (request.status != PingTimeStatus.DRAFT && request.status != PingTimeStatus.FORMED)) {
            throw RuntimeException("Only creator can delete draft or formed")
        }
        request.status = PingTimeStatus.DELETED
        pingTimeRepository.save(request)
    }
    fun logicalDeleteTimePing(id: Int) {
        deleteTimePing(id)
    }
    fun addServerComponentToDraft(componentId: Int): PingTimeDTO {
        val pingTime = addServerComponentToTimePing(FIXED_CREATOR_ID, componentId)
        return toDTO(pingTime)
    }
    fun addServerComponentToTimePing(userId: Int, componentId: Int, componentGroup: String? = null): PingTime {
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
            if (componentGroup != null) {
                existingItem.componentGroup = componentGroup
            }
            existingItem.subtotalTime = calculateSubtotal(existingItem.component, existingItem.quantity, existingItem.componentGroup)
        } else {
            val subtotal = calculateSubtotal(component, 1, componentGroup)
            val item = PingTimeComponent(
                pingTimeId = request.id,
                componentId = component.id,
                pingTime = request,
                component = component,
                quantity = 1,
                componentGroup = componentGroup,
                subtotalTime = subtotal
            )
            request.items.add(item)
        }
        recalculateTotal(request)
        return pingTimeRepository.save(request)
    }
    fun updateItem(requestId: Int, componentId: Int, dto: ItemUpdateDTO): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT) throw RuntimeException("Can only update draft items")
        val item = request.items.find { it.component.id == componentId } ?: throw RuntimeException("Item not found")
        dto.quantity?.let { item.quantity = it }
        item.subtotalTime = calculateSubtotal(item.component, item.quantity, item.componentGroup)
        recalculateTotal(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }
    fun deleteItem(requestId: Int, componentId: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Ping Time Request not found") }
        if (request.status != PingTimeStatus.DRAFT) throw RuntimeException("Can only delete from draft")
        request.items.removeIf { it.component.id == componentId }
        recalculateTotal(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }
    private fun calculateSubtotal(component: ServerComponent, quantity: Int, componentGroup: String?): Int {
        var subtotal = component.time * quantity
        val dbTime = componentRepository.findByTitle("БД")?.time ?: 0
        val cacheTime = componentRepository.findByTitle("Кэш")?.time ?: 0
        if (componentGroup == "БД") subtotal += dbTime
        if (componentGroup == "Кэш") subtotal += cacheTime
        return subtotal
    }
    private fun calculateBaseTime(request: PingTime): Int {
        return request.items.sumOf { it.subtotalTime }
    }
    fun updateTimePing(id: Int, dto: PingTimeUpdateDTO): PingTimeDTO {
        val pingTime = pingTimeRepository.findById(id).orElseThrow { RuntimeException("PingTime not found") }
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
                componentGroup = it.componentGroup,
                subtotalTime = it.subtotalTime
            )
        },
        loadCoefficient = request.loadCoefficient
    )
}