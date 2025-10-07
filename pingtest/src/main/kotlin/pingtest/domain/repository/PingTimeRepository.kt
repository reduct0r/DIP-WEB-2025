package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.PingTime
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PingTimeRepository : JpaRepository<PingTime, Int> {
    fun findByCreatorIdAndStatus(creatorId: Int, status: PingTimeStatus): PingTime?
    fun findAllByStatusNotInAndFormationDateBetween(
        statuses: List<PingTimeStatus>,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): List<PingTime>
}