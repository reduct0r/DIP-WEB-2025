package com.dip.pingtest.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ping_times")
data class PingTime(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,
    @Enumerated(EnumType.STRING)
    val status: PingTimeStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,
    val formationDate: LocalDateTime? = null,
    val completionDate: LocalDateTime? = null,
    @ManyToOne
    @JoinColumn(name = "moderator_id")
    val moderator: User? = null,

    var totalTime: Int = 0,
) {
    @OneToMany(mappedBy = "pingTime", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val items: MutableList<PingTimeComponent> = mutableListOf()
}