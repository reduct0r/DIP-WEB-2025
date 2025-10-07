package com.dip.pingtest.domain.model.enums

enum class LoadLevel(val multiplier: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    EXTREME(5)
}