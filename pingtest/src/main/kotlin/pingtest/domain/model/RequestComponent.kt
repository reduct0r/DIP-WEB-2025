package com.dip.pingtest.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "request_components")
@IdClass(RequestComponentId::class)
data class RequestComponent(
    @Id
    val requestId: Int,
    @Id
    val componentId: Int,
    @ManyToOne
    @MapsId("requestId")
    @JoinColumn(name = "request_id")
    val request: Request,
    @ManyToOne
    @MapsId("componentId")
    @JoinColumn(name = "component_id")
    val component: Component,
    var quantity: Int = 1,
    @Column(name = "order_number")
    var orderNumber: Int = 0,
    var componentGroup: String? = null,
    var subtotalTime: Int = 0
)