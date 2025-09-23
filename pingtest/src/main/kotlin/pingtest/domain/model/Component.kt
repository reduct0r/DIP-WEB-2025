package com.dip.pingtest.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "components")
data class Component(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val title: String,
    val description: String,
    @Column(columnDefinition = "TEXT")
    val longDescription: String,
    val time: Int,
    val image: String? = null,
    @Enumerated(EnumType.STRING)
    val status: ComponentStatus = ComponentStatus.ACTIVE,
    @Transient
    var imageUrl: String? = null
)