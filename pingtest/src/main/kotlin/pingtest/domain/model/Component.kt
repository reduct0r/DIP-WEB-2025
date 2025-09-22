package com.dip.pingtest.domain.model

data class Component(
    val id: Int,
    val title: String,
    val description: String,
    val longDescription: String,
    val time: Int,
    val image: String
)