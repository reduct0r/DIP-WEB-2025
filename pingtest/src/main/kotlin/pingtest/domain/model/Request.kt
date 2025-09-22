package com.dip.pingtest.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Request(
    val id: Int,
    val createdAt: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
    val totalTime: Int = 99,
    val items: List<RequestComponent> = emptyList()
)