package com.dip.pingtest.infrastructure.dto

data class ItemUpdateDTO(
    val quantity: Int? = null,
    val orderNumber: Int? = null,
    val componentGroup: String? = null
)