package com.dip.pingtest.repository

import com.dip.pingtest.domain.model.Component
import com.dip.pingtest.domain.model.Request
import com.dip.pingtest.domain.model.RequestComponent
import org.springframework.stereotype.Repository

@Repository
class ComponentRepository {
    private val components = listOf(
        Component(1, "Кэш", "Хранение часто используемых данных в памяти для ускорения доступа.", "Кэширование играет ключевую роль в повышении производительности веб-приложений, уменьшая нагрузку на сервер и время отклика. Оно позволяет избежать повторных обращений к медленным источникам данных, таким как база данных или внешние API, храня копии результатов в быстром хранилище, например, в RAM. Типичное время отклика для кэша составляет около 10 мс, что делает его идеальным для оптимизации часто запрашиваемых ресурсов.", 10, "cache.png"),
        Component(2, "Бэкенд", "Серверная логика и обработка запросов от клиентов.", "Бэкенд отвечает за основную логику приложения, включая обработку запросов, бизнес-логику, аутентификацию, интеграцию с базами данных и внешними сервисами. Он генерирует динамический контент и отправляет ответы фронтенду. Время обработки на бэкенде зависит от сложности операций и может варьироваться от 20 до 150 мс для типичных запросов, влияя на общую скорость приложения.", 150, "backend.png"),
        Component(3, "Фронтенд", "Клиентская часть: интерфейс и взаимодействие с пользователем.", "Фронтенд фокусируется на создании визуального интерфейса и обеспечении плавного пользовательского опыта. Он обрабатывает рендеринг страниц, обработку событий и взаимодействие с бэкендом через API. Время отклика фронтенда влияет на воспринимаемую скорость: от загрузки страницы до интерактивности, обычно около 100 мс, с акцентом на оптимизацию JavaScript и CSS для быстрой отрисовки.", 100, "frontend.png"),
        Component(4, "БД", "Хранение, управление и извлечение данных приложения.", "База данных является центральным хранилищем для всех данных веб-приложения, включая пользовательскую информацию, сессии и контент. Она обрабатывает запросы на чтение/запись, SQL-запросы и транзакции. Без оптимизации (индексация, кэширование) время отклика может достигать 1000 мс для сложных операций с большими объемами данных, что делает её потенциальным узким местом в производительности.", 1000, "database.png")
    )

    private val requests = mutableMapOf<Int, Request>(
        1 to Request(1, totalTime = 260)
    )

    // m-m связи
    private val requestItems = mutableMapOf<Int, MutableList<RequestComponent>>(
        1 to mutableListOf(
            RequestComponent(1, 1, quantity = 1, order = 1),
            RequestComponent(1, 2, quantity = 1, order = 2),
            RequestComponent(1, 3, quantity = 1, order = 3)
        )
    )

    fun getComponents(filter: String? = null): List<Component> {
        if (filter.isNullOrBlank()) return components
        return components.filter {
            it.title.contains(filter, ignoreCase = true) ||
                    it.time.toString().contains(filter) ||
                    it.description.contains(filter, ignoreCase = true) ||
                    it.longDescription.contains(filter, ignoreCase = true)
        }
    }

    fun getComponent(id: Int): Component? = components.find { it.id == id }

    fun getRequestItemCount(id: Int): Int = requestItems[id]?.size ?: 0

    fun getRequest(id: Int): Request? {
        val req = requests[id] ?: return null
        val items = requestItems[id] ?: emptyList()
        val filledItems = items.map { it.copy(component = getComponent(it.componentId)) }

        return req.copy(items = filledItems)
    }
}