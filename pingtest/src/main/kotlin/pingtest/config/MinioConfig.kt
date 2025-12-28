package com.dip.pingtest.config

import io.minio.MinioClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig(private val minioProperties: MinioProperties) {

    @Bean
    fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(minioProperties.url)
            .credentials(minioProperties.accessKey, minioProperties.secretKey)
            .build()
    }
}

@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    val url: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val externalUrl: String,
    val externalLocalhostUrl: String
)