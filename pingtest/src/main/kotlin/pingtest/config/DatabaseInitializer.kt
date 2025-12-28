package com.dip.pingtest.config

import com.dip.pingtest.domain.model.PingTime
import com.dip.pingtest.domain.model.User
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import com.dip.pingtest.domain.model.enums.Role
import com.dip.pingtest.domain.repository.PingTimeRepository
import com.dip.pingtest.domain.repository.UserRepository
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Random

@Component
class DatabaseInitializer(
    private val pingTimeRepository: PingTimeRepository,
    private val userRepository: UserRepository
) : CommandLineRunner {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    @Transactional
    override fun run(vararg args: String?) {
        println("Инициализация базы данных...")
        
        // Создание индекса
        createIndex()
        
        // Добавление услуг
        insertServerComponents()
        
        // Генерация тестовых данных отключена - БД уже заполнена
        // generateTestData()
        
        println("Инициализация базы данных завершена.")
    }
    
    private fun createIndex() {
        try {
            println("Создание оптимизированных индексов...")
            
            // 1. Частичный индекс на formation_date (исключает DELETED и DRAFT)
            // Это оптимально, так как все запросы фильтруют эти статусы
            entityManager.createNativeQuery(
                """
                CREATE INDEX IF NOT EXISTS idx_ping_times_formation_date_partial 
                ON ping_times(formation_date)
                WHERE status NOT IN ('DELETED', 'DRAFT')
                """
            ).executeUpdate()
            println("Частичный индекс idx_ping_times_formation_date_partial создан.")
            
            // 2. Составной индекс (status, formation_date) для запросов с фильтрацией по конкретному статусу
            entityManager.createNativeQuery(
                """
                CREATE INDEX IF NOT EXISTS idx_ping_times_status_formation_date 
                ON ping_times(status, formation_date)
                WHERE status NOT IN ('DELETED', 'DRAFT')
                """
            ).executeUpdate()
            println("Составной индекс idx_ping_times_status_formation_date создан.")
            
            // 3. Оставляем старый индекс для обратной совместимости (можно удалить позже)
            entityManager.createNativeQuery(
                """
                CREATE INDEX IF NOT EXISTS idx_ping_times_formation_date 
                ON ping_times(formation_date)
                """
            ).executeUpdate()
            println("Базовый индекс idx_ping_times_formation_date создан.")
            
            println("Все индексы успешно созданы.")
        } catch (e: Exception) {
            println("Ошибка при создании индексов: ${e.message}")
            e.printStackTrace()
        }
        
        // Добавляем поле is_moderator, если его нет (только создание, без синхронизации)
        try {
            println("Проверка наличия поля is_moderator...")
            entityManager.createNativeQuery(
                """
                ALTER TABLE users ADD COLUMN IF NOT EXISTS is_moderator BOOLEAN DEFAULT false
                """
            ).executeUpdate()
            
            // НЕ синхронизируем значение is_moderator с role - пользователь управляет этим вручную
            // Убрано: UPDATE users SET is_moderator = (role = 'MODERATOR') ...
            
            println("Поле is_moderator проверено (синхронизация отключена - управление вручную).")
        } catch (e: Exception) {
            println("Ошибка при работе с полем is_moderator: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun insertServerComponents() {
        try {
            println("Проверка наличия услуг...")
            val existingCount = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM server_components"
            ).singleResult as Number
            
            if (existingCount.toInt() > 0) {
                println("Услуги уже существуют в базе ($existingCount записей). Пропускаем вставку.")
                return
            }
            
            println("Добавление услуг в базу данных...")
            
            // Проверяем каждую услугу отдельно, чтобы избежать конфликтов
            val components = listOf(
                arrayOf(1, "Кэш", "Хранение часто используемых данных в памяти для ускорения доступа.", "Кэширование играет ключевую роль в повышении производительности веб-приложений, уменьшая нагрузку на сервер и время отклика. Оно позволяет избежать повторных обращений к медленным источникам данных, таким как база данных или внешние API, храня копии результатов в быстром хранилище, например, в RAM. Типичное время отклика для кэша составляет около 10 мс, что делает его идеальным для оптимизации часто запрашиваемых ресурсов.", 10, "cache.png", "ACTIVE"),
                arrayOf(2, "Бэкенд", "Серверная логика и обработка запросов от клиентов.", "Бэкенд отвечает за основную логику приложения, включая обработку запросов, бизнес-логику, аутентификацию, интеграцию с базами данных и внешними сервисами. Он генерирует динамический контент и отправляет ответы фронтенду. Время обработки на бэкенде зависит от сложности операций и может варьироваться от 20 до 150 мс для типичных запросов, влияя на общую скорость приложения.", 150, "backend.png", "ACTIVE"),
                arrayOf(3, "Фронтенд", "Клиентская часть: интерфейс и взаимодействие с пользователем.", "Фронтенд фокусируется на создании визуального интерфейса и обеспечении плавного пользовательского опыта. Он обрабатывает рендеринг страниц, обработку событий и взаимодействие с бэкендом через API. Время отклика фронтенда влияет на воспринимаемую скорость: от загрузки страницы до интерактивности, обычно около 100 мс, с акцентом на оптимизацию JavaScript и CSS для быстрой отрисовки.", 100, "frontend.png", "ACTIVE"),
                arrayOf(4, "БД", "Хранение, управление и извлечение данных приложения.", "База данных является центральным хранилищем для всех данных веб-приложения, включая пользовательскую информацию, сессии и контент. Она обрабатывает запросы на чтение/запись, SQL-запросы и транзакции. Без оптимизации (индексация, кэширование) время отклика может достигать 1000 мс для сложных операций с большими объемами данных, что делает её потенциальным узким местом в производительности.", 1000, "database.png", "ACTIVE")
            )
            
            components.forEach { component ->
                try {
                    entityManager.createNativeQuery(
                        """
                        INSERT INTO server_components (id, title, description, long_description, time, image, status) 
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING
                        """
                    ).setParameter(1, component[0])
                     .setParameter(2, component[1])
                     .setParameter(3, component[2])
                     .setParameter(4, component[3])
                     .setParameter(5, component[4])
                     .setParameter(6, component[5])
                     .setParameter(7, component[6])
                     .executeUpdate()
                } catch (e: Exception) {
                    // Игнорируем ошибки для уже существующих записей
                    println("Предупреждение при добавлении услуги ${component[1]}: ${e.message}")
                }
            }
            
            // Синхронизируем последовательность ID после вставки компонентов
            syncSequence()
            
            println("Услуги успешно добавлены в базу данных.")
        } catch (e: Exception) {
            println("Ошибка при добавлении услуг: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun syncSequence() {
        try {
            println("Синхронизация последовательности ID для server_components...")
            // Используем тот же подход, что и в ComponentService
            entityManager.createNativeQuery(
                """
                DO $$
                DECLARE
                    max_id INTEGER;
                    seq_name TEXT;
                BEGIN
                    SELECT COALESCE(MAX(id), 0) INTO max_id FROM server_components;
                    SELECT pg_get_serial_sequence('server_components', 'id') INTO seq_name;
                    
                    IF seq_name IS NOT NULL THEN
                        PERFORM setval(seq_name, max_id, true);
                    END IF;
                END $$;
                """
            ).executeUpdate()
            println("Последовательность ID синхронизирована.")
        } catch (e: Exception) {
            println("Ошибка при синхронизации последовательности: ${e.message}")
            e.printStackTrace()
            // Пытаемся альтернативный способ
            try {
                entityManager.createNativeQuery(
                    "SELECT setval('server_components_id_seq', (SELECT COALESCE(MAX(id), 1) FROM server_components), true)"
                ).executeUpdate()
                println("Альтернативная синхронизация выполнена успешно.")
            } catch (e2: Exception) {
                println("Альтернативная синхронизация также не удалась: ${e2.message}")
            }
        }
    }
    
    private fun generateTestData() {
        val existingCount = pingTimeRepository.count()
        val targetCount = 150000L
        
        if (existingCount >= targetCount) {
            println("В базе уже есть $existingCount записей. Пропускаем генерацию.")
            return
        }
        
        println("Генерация тестовых данных...")
        println("Текущее количество записей: $existingCount")
        println("Целевое количество записей: $targetCount")
        
        // Получаем или создаем тестового пользователя
        val testUser = userRepository.findAll().firstOrNull() 
            ?: run {
                val newUser = User(username = "test_user", password = "test", role = Role.USER, isModerator = false)
                userRepository.save(newUser)
            }
        
        val random = Random()
        val batchSize = 1000
        val recordsToGenerate = (targetCount - existingCount).toInt()
        
        println("Генерация $recordsToGenerate записей...")
        
        val startTime = System.currentTimeMillis()
        var generated = 0
        
        // Генерируем данные батчами для оптимизации
        repeat((recordsToGenerate / batchSize) + 1) { batch ->
            if (generated >= recordsToGenerate) return@repeat
            
            val currentBatchSize = minOf(batchSize, recordsToGenerate - generated)
            val batchList = mutableListOf<PingTime>()
            
            repeat(currentBatchSize) {
                val formationDate = LocalDateTime.now()
                    .minusDays(random.nextInt(365).toLong())
                    .minusHours(random.nextInt(24).toLong())
                    .minusMinutes(random.nextInt(60).toLong())
                
                val status = when (random.nextInt(4)) {
                    0 -> PingTimeStatus.FORMED
                    1 -> PingTimeStatus.COMPLETED
                    2 -> PingTimeStatus.REJECTED
                    else -> PingTimeStatus.ARCHIVED
                }
                
                val pingTime = PingTime(
                    status = status,
                    creator = testUser,
                    formationDate = formationDate,
                    createdAt = formationDate.minusDays(1),
                    totalTime = if (status == PingTimeStatus.COMPLETED) random.nextInt(1000) + 100 else null,
                    loadCoefficient = random.nextInt(5) + 1
                )
                
                batchList.add(pingTime)
            }
            
            pingTimeRepository.saveAll(batchList)
            generated += currentBatchSize
            
            if (batch % 10 == 0 || generated >= recordsToGenerate) {
                val elapsed = System.currentTimeMillis() - startTime
                val rate = if (elapsed > 0) generated.toDouble() / elapsed * 1000 else 0.0
                println("Сгенерировано: $generated / $recordsToGenerate записей (${String.format("%.2f", rate)} записей/сек)")
            }
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        val finalCount = pingTimeRepository.count()
        
        println("Генерация завершена!")
        println("Всего записей в базе: $finalCount")
        println("Время выполнения: ${elapsed / 1000} секунд")
        if (elapsed > 0) {
            println("Скорость: ${String.format("%.2f", finalCount.toDouble() / elapsed * 1000)} записей/сек")
        }
    }
}

