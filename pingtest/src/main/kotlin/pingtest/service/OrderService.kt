package com.dip.pingtest.service

import com.dip.pingtest.domain.ServiceEntity
import com.dip.pingtest.repository.ServiceRepository
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class ServiceService(private val repo: ServiceRepository) {

    fun getServices(): List<ServiceEntity> = repo.getServices()

    fun getService(id: Int): ServiceEntity = repo.getService(id) ?: throw RuntimeException("Not found")

    // Добавим время как в теме
    fun getWithTime(): Map<String, Any> {
        return mapOf("time" to LocalTime.now().toString(), "services" to getServices())
    }
}