package com.dip.pingtest.repository

import com.dip.pingtest.domain.ServiceEntity
import org.springframework.stereotype.Repository

@Repository
class ServiceRepository {
    fun getServices(): List<ServiceEntity> {
        return listOf(
            ServiceEntity(1, "first service"),
            ServiceEntity(2, "second service"),
            ServiceEntity(3, "third service")
        )
    }

    fun getService(id: Int): ServiceEntity? {
        return getServices().find { it.id == id } ?: throw RuntimeException("Service not found")
    }
}