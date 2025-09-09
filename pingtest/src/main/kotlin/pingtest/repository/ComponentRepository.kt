package com.dip.pingtest.repository

import com.dip.pingtest.domain.Component
import org.springframework.stereotype.Repository

@Repository
class ComponentRepository {
    val dbList = listOf(
        Component(1, "Кэш", "Хранение в памяти...Хранение в памяти...Хранение в памяти...", "Хранение в памяти...Хранение в памяти...Хранение в памяти...", 10, "cache.png"),
        Component(2, "Бэкенд", "Серверная логика...Хранение в памяти...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 150, "backend.png"),
        Component(3, "Фронтенд", "Клиентская часть...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 100, "frontend.png"),
        Component(3, "БД", "...Хранение в памяти...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 1000, "database.png")
    )

    fun getComponents(): List<Component> {
        return dbList
    }

    fun getComponent(id: Int): Component? {
        return dbList.find { it.id == id } ?: throw RuntimeException("Service not found")
    }
}