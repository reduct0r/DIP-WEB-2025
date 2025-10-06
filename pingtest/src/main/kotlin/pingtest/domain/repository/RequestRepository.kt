package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.PingTime
import com.dip.pingtest.domain.model.PingTimeStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RequestRepository : JpaRepository<PingTime, Int> {
    fun findByCreatorIdAndStatus(creatorId: Int, status: PingTimeStatus): PingTime?
}