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
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.NoSuchElementException
import java.util.UUID

@Service
class ComponentService(
    private val componentRepository: ComponentRepository,
    private val minioClient: MinioClient,  // Основной клиент для внутренних операций
    private val minioProperties: MinioProperties,
    private val userRepository: UserRepository
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun getComponentsAsDomain(filter: String?, useIp: Boolean = false): List<ServerComponent> {
        val components = if (filter.isNullOrBlank()) {
            componentRepository.findAll().filter { it.status == ServerComponentStatus.ACTIVE }
        } else {
            componentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter, filter)
                .filter { it.status == ServerComponentStatus.ACTIVE }
        }
        components.forEach { component -> component.imageUrl = generatePresignedUrl(component.image, useIp) }
        return components
    }

    fun getComponentAsDomain(id: Int, useIp: Boolean = false): ServerComponent? {
        val component = componentRepository.findById(id).orElse(null) ?: return null
        if (component.status != ServerComponentStatus.ACTIVE) return null
        component.imageUrl = generatePresignedUrl(component.image, useIp)
        return component
    }

    fun getComponents(filter: String?, useIp: Boolean = false): List<ComponentDTO> {
        val components = if (filter.isNullOrBlank()) {
            componentRepository.findAll().filter { it.status == ServerComponentStatus.ACTIVE }
        } else {
            componentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter, filter)
                .filter { it.status == ServerComponentStatus.ACTIVE }
        }
        return components.map { toDTO(it, useIp) }
    }

    fun getComponent(id: Int, useIp: Boolean = false): ComponentDTO {
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        if (component.status != ServerComponentStatus.ACTIVE) throw RuntimeException("Серверный компонент удален")
        return toDTO(component, useIp)
    }

    @Transactional
    fun createComponent(dto: ComponentDTO): ComponentDTO {
        if (!isCurrentUserModerator()) {
            throw RuntimeException("Access denied: Only moderators can create components")
        }
        // Синхронизируем последовательность ID перед созданием (в той же транзакции)
        syncSequence()
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
        if (!isCurrentUserModerator()) {
            throw RuntimeException("Access denied: Only moderators can update components")
        }
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
        if (!isCurrentUserModerator()) {
            throw RuntimeException("Access denied: Only moderators can delete components")
        }
        val component = componentRepository.findById(id).orElseThrow { NoSuchElementException("Component not found") }
        component.image?.let { removeFromMinio(it) }
        component.status = ServerComponentStatus.DELETED
        componentRepository.save(component)
    }

    fun uploadImage(id: Int, file: MultipartFile): String {
        if (!isCurrentUserModerator()) {
            throw RuntimeException("Access denied: Only moderators can upload component images")
        }
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
        return generatePresignedUrl(objectName, false) ?: throw RuntimeException("Failed to generate URL")
    }

    fun generatePresignedUrl(imageName: String?, useIp: Boolean = false): String? {
        if (imageName == null) return null

        // Выбираем URL в зависимости от параметра: IP для Android, localhost для браузера
        val endpointUrl = if (useIp) minioProperties.externalUrl else minioProperties.externalLocalhostUrl

        // Создаем отдельный клиент с ВНЕШНИМ URL для генерации presigned URL
        val externalMinioClient = MinioClient.builder()
            .endpoint(endpointUrl)
            .credentials(minioProperties.accessKey, minioProperties.secretKey)
            .build()

        return externalMinioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioProperties.bucket)
                .`object`(imageName)
                .expiry(60 * 60 * 24) // 24 часа
                .build()
        )
    }

    private fun removeFromMinio(objectName: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder().bucket(minioProperties.bucket).`object`(objectName).build()
        )
    }

    private fun toDTO(component: ServerComponent, useIp: Boolean = false): ComponentDTO = ComponentDTO(
        component.id, component.title, component.description, component.longDescription,
        component.time, generatePresignedUrl(component.image, useIp)
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

    private fun getCurrentUserId(): Int {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Int
    }

    private fun isCurrentUserModerator(): Boolean {
        val userId = getCurrentUserId()
        val user = userRepository.findById(userId).orElse(null)
        return user?.isModerator ?: false
    }

    private fun syncSequence() {
        try {
            // Используем простой и надежный способ синхронизации последовательности
            entityManager.createNativeQuery(
                """
                DO $$
                DECLARE
                    max_id INTEGER;
                    seq_name TEXT;
                BEGIN
                    SELECT COALESCE(MAX(id), 0) INTO max_id FROM server_components;
                    SELECT pg_get_serial_sequence('server_components', 'id') INTO seq_name;
                    
                    IF seq_name IS NOT NULL THEN
                        PERFORM setval(seq_name, max_id, true);
                    END IF;
                END $$;
                """
            ).executeUpdate()
        } catch (e: Exception) {
            // Логируем ошибку для диагностики
            println("Ошибка синхронизации последовательности: ${e.message}")
            e.printStackTrace()
            // Пытаемся альтернативный способ
            try {
                entityManager.createNativeQuery(
                    "SELECT setval('server_components_id_seq', (SELECT COALESCE(MAX(id), 1) FROM server_components), true)"
                ).executeUpdate()
            } catch (e2: Exception) {
                println("Альтернативная синхронизация также не удалась: ${e2.message}")
            }
        }
    }
}