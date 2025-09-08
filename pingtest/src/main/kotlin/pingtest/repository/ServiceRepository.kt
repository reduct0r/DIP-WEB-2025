package com.dip.pingtest.repository

import com.dip.pingtest.domain.ServiceEntity
import org.springframework.stereotype.Repository

@Repository
class ServiceRepository {
    fun getServices(): List<ServiceEntity> {
        return listOf(
            ServiceEntity(1, "Кэш", "Хранение в памяти...Хранение в памяти...Хранение в памяти...", 10, "cache.png"),
            ServiceEntity(2, "Бэкенд", "Серверная логика...Хранение в памяти...Хранение в памяти...Хранение в памяти...", 150, "backend.png"),
            ServiceEntity(3, "Фронтенд", "Клиентская часть...Хранение в памяти...Хранение в памяти...", 100, "frontend.png"),
            ServiceEntity(3, "БД", "...Хранение в памяти...Хранение в памяти...Хранение в памяти...", 1000, "database.png")
        )
    }

    fun getService(id: Int): ServiceEntity? {
        return getServices().find { it.id == id } ?: throw RuntimeException("Service not found")
    }
}