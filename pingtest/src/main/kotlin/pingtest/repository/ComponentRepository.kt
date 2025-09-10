package com.dip.pingtest.repository

import com.dip.pingtest.domain.model.Component
import org.springframework.stereotype.Repository

@Repository
class ComponentRepository {
    val dbList = listOf(
        Component(1, "Кэш", "Хранение в памяти...Хранение в памяти...Хранение в памяти...", "Хранение в памяти...Хранение в памяти...Хранение в памяти...", 10, "cache.png"),
        Component(2, "Бэкенд", "Серверная логика...Хранение в памяти...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 150, "backend.png"),
        Component(3, "Фронтенд", "Клиентская часть...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 100, "frontend.png"),
        Component(4, "БД", "...Хранение в памяти...Хранение в памяти...Хранение в памяти...","Хранение в памяти...Хранение в памяти...Хранение в памяти...", 1000, "database.png")
    )

    private val requestMap = mapOf(
        1 to dbList[0],
        3 to dbList[2]
    )

    fun getComponents(filter: String? = null): List<Component> {
        if (filter.isNullOrBlank()) return dbList
        return dbList.filter {
            it.title.contains(filter, ignoreCase = true) ||
                    it.time.toString().contains(filter) ||
                    it.description.contains(filter)
        }
    }

    fun getComponent(id: Int): Component? {
        return dbList.find { it.id == id } ?: throw RuntimeException("Service not found")
    }

    fun getRequestItems(): Map<Int, Component> {
        return requestMap
    }
}