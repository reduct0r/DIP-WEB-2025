package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.Request
import com.dip.pingtest.domain.model.RequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RequestRepository : JpaRepository<Request, Int> {
    fun findByCreatorIdAndStatus(creatorId: Int, status: RequestStatus): Request?
}