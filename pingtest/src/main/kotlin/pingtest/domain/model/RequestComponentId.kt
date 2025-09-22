package com.dip.pingtest.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class RequestComponentId(
    @Column(name = "request_id")
    val requestId: Int = 0,
    @Column(name = "component_id")
    val componentId: Int = 0
) : java.io.Serializable