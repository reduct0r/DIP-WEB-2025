package com.dip.pingtest.service

import com.dip.pingtest.config.MinioProperties
import com.dip.pingtest.domain.model.ServerComponent
import com.dip.pingtest.domain.model.enums.ServerComponentStatus
import com.dip.pingtest.domain.repository.ComponentRepository
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.ComponentDTO
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.NoSuchElementException
import java.util.UUID

@Service
class ComponentService(
    private val componentRepository: ComponentRepository,
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties,
    private val userRepository: UserRepository
) {
    fun getComponentsAsDomain(filter: String?): List<ServerComponent> {
        val components = if (filter.isNullOrBlank()) {
            componentRepository.findAll().filter { it.status == ServerComponentStatus.ACTIVE }
        } else {
            componentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter, filter)
                .filter { it.status == ServerComponentStatus.ACTIVE }
        }
        components.forEach { it.imageUrl = generatePresignedUrl(it.image) }
        return components
    }
    fun getComponentAsDomain(id: Int): ServerComponent? {
        val component = componentRepository.findById(id).orElse(null)
        if (component?.status != ServerComponentStatus.ACTIVE) return null
        component?.imageUrl = generatePresignedUrl(component.image)
        return component
    }
    fun getComponents(filter: String?): List<ComponentDTO> {
        val components = if (filter.isNullOrBlank()) {
            componentRepository.findAll().filter { it.status == ServerComponentStatus.ACTIVE }
        } else {
            componentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter, filter)
                .filter { it.status == ServerComponentStatus.ACTIVE }
        }
        return components.map { toDTO(it) }
    }
    fun getComponent(id: Int): ComponentDTO {
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        if (component.status != ServerComponentStatus.ACTIVE) throw RuntimeException("Серверный компонент удален")
        return toDTO(component)
    }
    fun createComponent(dto: ComponentDTO): ComponentDTO {
        val component = ServerComponent(
            title = dto.title,
            description = dto.description,
            longDescription = dto.longDescription,
            time = dto.time
        )
        val saved = componentRepository.save(component)
        return toDTO(saved)
    }
    fun updateComponent(id: Int, dto: ComponentDTO): ComponentDTO {
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        if (component.status != ServerComponentStatus.ACTIVE) throw RuntimeException("Component deleted")
        component.title = dto.title
        component.description = dto.description
        component.longDescription = dto.longDescription
        component.time = dto.time
        val saved = componentRepository.save(component)
        return toDTO(saved)
    }
    fun deleteComponent(id: Int) {
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        component.image?.let { removeFromMinio(it) }
        component.status = ServerComponentStatus.DELETED
        componentRepository.save(component)
    }
    fun uploadImage(id: Int, file: MultipartFile): String {
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        if (component.status != ServerComponentStatus.ACTIVE) throw RuntimeException("Component deleted")
        val extension = file.originalFilename?.substringAfterLast(".") ?: "jpg"
        val objectName = "${UUID.randomUUID()}.$extension" // Latin name
        component.image?.let { removeFromMinio(it) }
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(minioProperties.bucket)
                .`object`(objectName)
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType)
                .build()
        )
        component.image = objectName
        componentRepository.save(component)
        return generatePresignedUrl(objectName) ?: throw RuntimeException("Failed to generate URL")
    }
    fun generatePresignedUrl(imageName: String?): String? {
        if (imageName == null) return null
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioProperties.bucket)
                .`object`(imageName)
                .expiry(60 * 60 * 24) // 24 hours
                .build()
        )
    }
    private fun removeFromMinio(objectName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder().bucket(minioProperties.bucket).`object`(objectName).build()
        )
    }
    private fun toDTO(component: ServerComponent): ComponentDTO = ComponentDTO(
        component.id, component.title, component.description, component.longDescription,
        component.time, generatePresignedUrl(component.image)
    )
    fun getPreference(userId: Int, componentId: Int): String? {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        val prefs = (user.preferences ?: "").split(";").filter { it.isNotBlank() }.associate {
            val parts = it.split(":", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
        return prefs[componentId.toString()]
    }
    fun updatePreference(userId: Int, componentId: Int, group: String?) {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        val prefs = (user.preferences ?: "").split(";").filter { it.isNotBlank() }.associateTo(mutableMapOf()) {
            val parts = it.split(":", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
        if (group.isNullOrEmpty()) {
            prefs.remove(componentId.toString())
        } else {
            prefs[componentId.toString()] = group
        }
        user.preferences = prefs.entries.joinToString(";") { "${it.key}:${it.value}" }
        userRepository.save(user)
    }
}