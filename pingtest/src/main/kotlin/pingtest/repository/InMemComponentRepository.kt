package com.dip.pingtest.repository

import com.dip.pingtest.domain.model.Component
import org.springframework.stereotype.Repository
import pingtest.domain.repository.ComponentRepository

@Repository
class InMemComponentRepository: ComponentRepository {
    val dbList = listOf(
        Component(1, "Кэш", "Хранение часто используемых данных в памяти для ускорения доступа.", "Кэширование играет ключевую роль в повышении производительности веб-приложений, уменьшая нагрузку на сервер и время отклика. Оно позволяет избежать повторных обращений к медленным источникам данных, таким как база данных или внешние API, храня копии результатов в быстром хранилище, например, в RAM. Типичное время отклика для кэша составляет около 10 мс, что делает его идеальным для оптимизации часто запрашиваемых ресурсов.", 10, "cache.png"),
        Component(2, "Бэкенд", "Серверная логика и обработка запросов от клиентов.", "Бэкенд отвечает за основную логику приложения, включая обработку запросов, бизнес-логику, аутентификацию, интеграцию с базами данных и внешними сервисами. Он генерирует динамический контент и отправляет ответы фронтенду. Время обработки на бэкенде зависит от сложности операций и может варьироваться от 20 до 150 мс для типичных запросов, влияя на общую скорость приложения.", 150, "backend.png"),
        Component(3, "Фронтенд", "Клиентская часть: интерфейс и взаимодействие с пользователем.", "Фронтенд фокусируется на создании визуального интерфейса и обеспечении плавного пользовательского опыта. Он обрабатывает рендеринг страниц, обработку событий и взаимодействие с бэкендом через API. Время отклика фронтенда влияет на воспринимаемую скорость: от загрузки страницы до интерактивности, обычно около 100 мс, с акцентом на оптимизацию JavaScript и CSS для быстрой отрисовки.", 100, "frontend.png"),
        Component(4, "БД", "Хранение, управление и извлечение данных приложения.", "База данных является центральным хранилищем для всех данных веб-приложения, включая пользовательскую информацию, сессии и контент. Она обрабатывает запросы на чтение/запись, SQL-запросы и транзакции. Без оптимизации (индексация, кэширование) время отклика может достигать 1000 мс для сложных операций с большими объемами данных, что делает её потенциальным узким местом в производительности.", 1000, "database.png")
    )

    private val requestMap = mapOf(
        1 to dbList[0],
        2 to dbList[2]
    )

    val orderStorage: Map<Int, List<Component>> = mapOf(
        1 to listOf(dbList[0], dbList[2])
    )

    override fun getComponents(filter: String?): List<Component> {
        if (filter.isNullOrBlank()) return dbList
        return dbList.filter {
            it.title.contains(filter, ignoreCase = true) ||
                    it.time.toString().contains(filter) ||
                    it.description.contains(filter)
        }
    }

    override fun getComponent(id: Int): Component? {
        return dbList.find { it.id == id } ?: throw RuntimeException("Service not found")
    }

    override fun getRequestItems(key: Int): List<Component> =
        orderStorage[key] ?: emptyList()
}