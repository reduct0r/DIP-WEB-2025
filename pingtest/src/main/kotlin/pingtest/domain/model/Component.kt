package com.dip.pingtest.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "components")  // Services table
data class Component(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    val title: String,
    val description: String,
    val longDescription: String,
    val time: Int,
    val image: String? = null,  // Nullable object name in MinIO
    @Enumerated(EnumType.STRING)
    val status: ComponentStatus = ComponentStatus.ACTIVE,
    // Additional domain-specific fields
    val category: String? = null,  // e.g., for grouping components

    @Transient
    var imageUrl: String? = null  // Transient for presigned URL
)