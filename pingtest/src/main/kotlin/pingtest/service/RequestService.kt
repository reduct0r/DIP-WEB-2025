package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.RequestRepository
import com.dip.pingtest.domain.repository.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RequestService(
    private val requestRepository: RequestRepository,
    private val componentRepository: ComponentRepository,
    private val userRepository: UserRepository,
    private val componentService: ComponentService,
    private val jdbcTemplate: JdbcTemplate
) {
    fun getRequest(id: Int): Request? {
        return requestRepository.findById(id).orElse(null)?.apply {
            items.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
        }
    }

    fun getDraftRequestForUser(userId: Int): Request? {
        return requestRepository.findByCreatorIdAndStatus(userId, RequestStatus.DRAFT)?.apply {
            items.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
        }
    }

    fun getDraftRequestIdForUser(userId: Int): Int? {
        return requestRepository.findByCreatorIdAndStatus(userId, RequestStatus.DRAFT)?.id
    }

    fun getRequestItemCountForUser(userId: Int): Int {
        return getDraftRequestForUser(userId)?.items?.size ?: 0
    }

    @Transactional
    fun addComponentToRequest(userId: Int, componentId: Int): Request {
        val component = componentRepository.findById(componentId).orElseThrow { RuntimeException("Component not found") }
        var request = requestRepository.findByCreatorIdAndStatus(userId, RequestStatus.DRAFT)

        if (request == null) {
            val creator = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
            request = Request(status = RequestStatus.DRAFT, creator = creator)
            request = requestRepository.save(request)
        }

        val existingItem = request.items.find { it.componentId == componentId }
        if (existingItem != null) {
            existingItem.quantity += 1
            request.totalTime += component.time
        } else {
            val order = request.items.size + 1
            val subtotal = component.time * 1
            val item = RequestComponent(
                requestId = request.id,
                componentId = component.id,
                request = request,
                component = component,
                quantity = 1,
                orderNumber = order,
                componentGroup = null,
                subtotalTime = subtotal
            )
            request.items.add(item)
            request.totalTime += subtotal
        }

        return requestRepository.save(request)
    }

    fun logicalDeleteRequest(id: Int) {
        jdbcTemplate.update(
            "UPDATE requests SET status = ? WHERE id = ?",
            RequestStatus.DELETED.name, id
        )
    }
}