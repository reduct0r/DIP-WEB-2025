package com.dip.pingtest.service

import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.RequestRepository
import com.dip.pingtest.domain.repository.UserRepository
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class RequestService(
    private val requestRepository: RequestRepository,
    private val componentRepository: ComponentRepository,
    private val userRepository: UserRepository,  // Added
    private val componentService: ComponentService,  // To get components with URLs
    private val jdbcTemplate: JdbcTemplate  // For raw SQL
) {
    fun getRequest(id: Int): Request? {
        return requestRepository.findById(id).orElse(null)?.apply {
            // Load items and set image URLs via componentService
            items.forEach { it.component.imageUrl = componentService.generatePresignedUrl(it.component.image) }
        }
    }

    fun getRequestItemCount(id: Int): Int {
        return getRequest(id)?.items?.size ?: 0
    }

    @Transactional
    fun addComponentToRequest(userId: Int, componentId: Int): Request {
        val component = componentRepository.findById(componentId).orElseThrow { RuntimeException("Component not found") }
        var request = requestRepository.findByCreatorIdAndStatus(userId, RequestStatus.DRAFT)  // Changed to var

        if (request == null) {
            val creator = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
            request = Request(status = RequestStatus.DRAFT, creator = creator)
            requestRepository.save(request)
        }

        // Check for uniqueness (composite key prevents duplicates)
        if (request.items.any { it.componentId == componentId }) {
            throw RuntimeException("Component already in request")
        }

        val order = request.items.size + 1
        val subtotal = component.time * 1  // quantity=1
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
        request = request.copy(totalTime = request.totalTime + subtotal)  // Reassign with copy to update totalTime (since data class is immutable)

        return requestRepository.save(request)
    }

    fun logicalDeleteRequest(id: Int) {
        // Raw SQL UPDATE as per TZ (no ORM for delete)
        jdbcTemplate.update(
            "UPDATE requests SET status = ? WHERE id = ?",
            RequestStatus.DELETED.name, id
        )
    }
}