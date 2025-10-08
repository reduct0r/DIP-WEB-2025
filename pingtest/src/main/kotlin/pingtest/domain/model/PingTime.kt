package com.dip.pingtest.domain.model

import com.dip.pingtest.domain.model.enums.LoadLevel
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
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
    var status: PingTimeStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    val creator: User,
    var formationDate: LocalDateTime? = null,
    var completionDate: LocalDateTime? = null,
    @ManyToOne
    @JoinColumn(name = "moderator_id")
    var moderator: User? = null,

    var totalTime: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "load_level")
    var loadLevel: LoadLevel = LoadLevel.LOW
) {
    @OneToMany(mappedBy = "pingTime", cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE], orphanRemoval = true)
    val items: MutableList<PingTimeComponent> = mutableListOf()
}