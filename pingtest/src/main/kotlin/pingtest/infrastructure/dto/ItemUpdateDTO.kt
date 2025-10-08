package com.dip.pingtest.infrastructure.dto

data class ItemUpdateDTO(
    val quantity: Int? = null,
    val priority: Int? = null,
    val componentGroup: String? = null,
    val serverComponentTime: Int? = null
)