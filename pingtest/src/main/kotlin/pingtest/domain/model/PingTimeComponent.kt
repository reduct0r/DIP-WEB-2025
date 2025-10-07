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
@Table(name = "ping_time_components")
@IdClass(PingTimeComponentId::class)
data class PingTimeComponent(
    @Id
    val pingTimeId: Int,
    @Id
    val componentId: Int,
    @ManyToOne
    @MapsId("pingTimeId")
    @JoinColumn(name = "ping_time_id")
    val pingTime: PingTime,
    @ManyToOne
    @MapsId("componentId")
    @JoinColumn(name = "component_id")
    val component: ServerComponent,
    var quantity: Int = 1,
    @Column(name = "order_number")
    var priority: Int = 0,
    var componentGroup: String? = null,
    var subtotalTime: Int = 0
)