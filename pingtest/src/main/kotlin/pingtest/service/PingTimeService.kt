package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.PingTimeRepository
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.CartIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeItemDTO
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
        const val FIXED_MODERATOR_ID = 1 // For now, same
    }

    fun getCartIcon(): CartIconDTO {
        val draft = pingTimeRepository.findByCreatorIdAndStatus(FIXED_CREATOR_ID, PingTimeStatus.DRAFT)
        return CartIconDTO(draft?.id, draft?.items?.size ?: 0)
    }

    fun getRequests(status: String?, fromDate: String?, toDate: String?): List<PingTimeDTO> {
        val excludedStatuses = listOf(PingTimeStatus.DELETED, PingTimeStatus.DRAFT)
        val from = fromDate?.let { LocalDateTime.parse(it) }
        val to = toDate?.let { LocalDateTime.parse(it) }
        var requests = pingTimeRepository.findAllByStatusNotInAndFormationDateBetween(excludedStatuses, from, to)
        status?.let { s ->
            val enumStatus = PingTimeStatus.valueOf(s.uppercase())
            requests = requests.filter { it.status == enumStatus }
        }
        return requests.map { toDTO(it) }
    }

    fun getRequest(id: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Request not found") }
        if (request.status == PingTimeStatus.DELETED) throw RuntimeException("Request deleted")
        return toDTO(request)
    }

    fun getRequestDomain(id: Int): PingTime? {
        val pingTime = pingTimeRepository.findById(id).orElse(null)
        pingTime?.items?.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
        return pingTime
    }

    fun getDraftRequestIdForUser(userId: Int): Int? {
        return pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)?.id
    }

    fun getRequestItemCountForUser(userId: Int): Int {
        return pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)?.items?.size ?: 0
    }

    fun formRequest(id: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Request not found") }
        if (request.status != PingTimeStatus.DRAFT || request.creator.id != FIXED_CREATOR_ID) {
            throw RuntimeException("Only creator can form draft")
        }
        if (request.items.isEmpty()) throw RuntimeException("Cannot form empty request")
        request.status = PingTimeStatus.FORMED
        request.formationDate = LocalDateTime.now()
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun completeRequest(id: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Request not found") }
        if (request.status != PingTimeStatus.FORMED) throw RuntimeException("Only formed can be completed")
        // Moderator check skip
        request.totalTime = calculateTotalTime(request)
        request.status = PingTimeStatus.COMPLETED
        request.completionDate = LocalDateTime.now()
        request.moderator = userRepository.findById(FIXED_MODERATOR_ID).orElseThrow()
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun rejectRequest(id: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Request not found") }
        if (request.status != PingTimeStatus.FORMED) throw RuntimeException("Only formed can be rejected")
        request.status = PingTimeStatus.REJECTED
        request.completionDate = LocalDateTime.now()
        request.moderator = userRepository.findById(FIXED_MODERATOR_ID).orElseThrow()
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun deleteRequest(id: Int) {
        val request = pingTimeRepository.findById(id).orElseThrow { RuntimeException("Request not found") }
        if (request.creator.id != FIXED_CREATOR_ID || (request.status != PingTimeStatus.DRAFT && request.status != PingTimeStatus.FORMED)) {
            throw RuntimeException("Only creator can delete draft or formed")
        }
        request.status = PingTimeStatus.DELETED
        pingTimeRepository.save(request)
    }

    fun logicalDeleteRequest(id: Int) {
        deleteRequest(id)
    }

    fun addComponentToDraft(componentId: Int): PingTimeDTO {
        val pingTime = addComponentToRequest(FIXED_CREATOR_ID, componentId)
        return toDTO(pingTime)
    }

    fun addComponentToRequest(userId: Int, componentId: Int): PingTime {
        val component = componentRepository.findById(componentId).orElseThrow { RuntimeException("Component not found") }
        var request = pingTimeRepository.findByCreatorIdAndStatus(userId, PingTimeStatus.DRAFT)
        val creator = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }

        if (request == null) {
            request = PingTime(creator = creator, status = PingTimeStatus.DRAFT)
            request = pingTimeRepository.save(request)
        }

        val existingItem = request.items.find { it.component.id == componentId }
        if (existingItem != null) {
            existingItem.quantity += 1
            existingItem.subtotalTime = calculateSubtotal(existingItem.component, existingItem.quantity, existingItem.componentGroup)
        } else {
            val order = request.items.size + 1
            val subtotal = calculateSubtotal(component, 1, null)
            val item = PingTimeComponent(
                pingTimeId = request.id,
                componentId = component.id,
                pingTime = request,
                component = component,
                quantity = 1,
                orderNumber = order,
                componentGroup = null,
                subtotalTime = subtotal
            )
            request.items.add(item)
        }
        request.totalTime = calculateTotalTime(request)
        return pingTimeRepository.save(request)
    }

    fun updateItem(requestId: Int, componentId: Int, dto: ItemUpdateDTO): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Request not found") }
        if (request.status != PingTimeStatus.DRAFT) throw RuntimeException("Can only update draft items")
        val item = request.items.find { it.component.id == componentId } ?: throw RuntimeException("Item not found")
        dto.quantity?.let { item.quantity = it }
        dto.orderNumber?.let { item.orderNumber = it }
        dto.componentGroup?.let { item.componentGroup = it }
        item.subtotalTime = calculateSubtotal(item.component, item.quantity, item.componentGroup)
        request.totalTime = calculateTotalTime(request)
        val saved = pingTimeRepository.save(request)
        return toDTO(saved)
    }

    fun deleteItem(requestId: Int, componentId: Int): PingTimeDTO {
        val request = pingTimeRepository.findById(requestId).orElseThrow { RuntimeException("Request not found") }
        if (request.status != PingTimeStatus.DRAFT) throw RuntimeException("Can only delete from draft")
        request.items.removeIf { it.component.id == componentId }
        request.items.forEachIndexed { index, item -> item.orderNumber = index + 1 }
        request.totalTime = calculateTotalTime(request)
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

    private fun calculateTotalTime(request: PingTime): Int {
        val sortedItems = request.items.sortedBy { it.orderNumber }
        val grouped = sortedItems.groupBy { it.orderNumber }
        var total = 0
        grouped.keys.sorted().forEach { key ->
            val groupSum = grouped[key]!!.sumOf { it.subtotalTime }
            total += groupSum
        }
        return total
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
                orderNumber = it.orderNumber,
                componentGroup = it.componentGroup,
                subtotalTime = it.subtotalTime
            )
        }
    )
}