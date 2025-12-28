package com.dip.pingtest.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.index")
class IndexConfig {
    /**
     * Флаг для управления использованием индексов в запросах к БД.
     * true - использовать индексы (по умолчанию)
     * false - отключить использование индексов для сравнения производительности
     */
    var enabled: Boolean = true
}

