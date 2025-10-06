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
@Table(name = "server_components")
data class ServerComponent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    var title: String,
    var description: String,
    @Column(columnDefinition = "TEXT")
    var longDescription: String,
    var time: Int,
    var image: String? = null,
    @Enumerated(EnumType.STRING)
    var status: ServerComponentStatus = ServerComponentStatus.ACTIVE,
    @Transient
    var imageUrl: String? = null
)