package com.dip.pingtest.infrastructure.dto

data class PingTimeItemDTO(
    val componentId: Int,
    val title: String,
    val description: String,
    val time: Int,
    val imageUrl: String? = null,
    val quantity: Int,
    val priority: Int,
    val componentGroup: String? = null,
    val subtotalTime: Int
)