package com.dip.pingtest.service

import com.dip.pingtest.domain.Component
import com.dip.pingtest.repository.ComponentRepository
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class ComponentService(private val repo: ComponentRepository) {

    fun getServices(): List<Component> = repo.getComponents()

    fun getService(id: Int): Component = repo.getComponent(id) ?: throw RuntimeException("Not found")

    // Добавим время как в теме
    fun getWithTime(): Map<String, Any> {
        return mapOf("time" to LocalTime.now().toString(), "services" to getServices())
    }
}