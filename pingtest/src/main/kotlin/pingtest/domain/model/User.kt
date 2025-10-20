package com.dip.pingtest.domain.model

import com.dip.pingtest.domain.model.enums.Role
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    var username: String,
    var password: String,
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER,
    var preferences: String? = null
)