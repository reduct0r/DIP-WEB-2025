package com.dip.pingtest.service

import com.dip.pingtest.config.MinioProperties
import com.dip.pingtest.domain.model.*
import com.dip.pingtest.domain.repository.ComponentRepository
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.springframework.stereotype.Service

@Service
class ComponentService(
    private val componentRepository: ComponentRepository,
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties
) {
    fun getComponents(filter: String?): List<ServerComponent> {
        val components = if (filter.isNullOrBlank()) {
            componentRepository.findAll()
        } else {
            componentRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter, filter)
        }
        components.forEach { it.imageUrl = generatePresignedUrl(it.image) }
        return components
    }

    fun getComponent(id: Int): ServerComponent? {
        val component = componentRepository.findById(id).orElse(null)
        component?.imageUrl = generatePresignedUrl(component.image)
        return component
    }

    fun getStaticImageUrl(imageName: String): String? {
        return generatePresignedUrl(imageName)
    }

    fun generatePresignedUrl(imageName: String?): String? {
        if (imageName == null) return null
        val link = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioProperties.bucket)
                .`object`(imageName)
                .build()
        )
        println(link)
        return link
}
}