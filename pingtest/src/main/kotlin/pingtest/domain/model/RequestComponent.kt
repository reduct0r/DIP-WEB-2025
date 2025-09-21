package com.dip.pingtest.domain.model

data class RequestComponent(
    val requestId: Int,
    val componentId: Int,
    val component: Component? = null,
    val quantity: Int = 1,
    val order: Int = 0,
    val group: String? = null
)