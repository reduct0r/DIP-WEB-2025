package com.dip.pingtest.infrastructure.dto

data class ComponentDTO(
    val id: Int? = null,
    val title: String,
    val description: String,
    val longDescription: String,
    val time: Int,
    val imageUrl: String? = null,
    var isDataBaseUsage: Boolean? = null
)