package com.dip.pingtest.domain.model

data class Request(
    val id: Int,
    val createdAt: String = "2025-09-21",
    val totalTime: Int = 99,
    val items: List<RequestComponent> = emptyList()
)