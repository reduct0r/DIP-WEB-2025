package com.dip.pingtest.infrastructure.dto

data class PingTimeDTO(
    val id: Int? = null,
    val status: String,
    val createdAt: String,
    val creatorUsername: String,
    val formationDate: String? = null,
    val completionDate: String? = null,
    val moderatorUsername: String? = null,
    val totalTime: Int,
    val items: List<PingTimeItemDTO>,
    val loadLevel: String
)