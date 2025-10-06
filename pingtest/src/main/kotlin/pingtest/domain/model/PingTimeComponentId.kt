package com.dip.pingtest.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class PingTimeComponentId(
    @Column(name = "ping_time_id")
    val pingTimeId: Int = 0,
    @Column(name = "component_id")
    val componentId: Int = 0
) : java.io.Serializable