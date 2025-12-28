package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.PingTime
import com.dip.pingtest.domain.model.enums.PingTimeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PingTimeRepository : JpaRepository<PingTime, Int> {
    fun findByCreatorIdAndStatus(creatorId: Int, status: PingTimeStatus): PingTime?
    fun findAllByStatusNotInAndFormationDateBetween(
        statuses: List<PingTimeStatus>,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable
    ): Page<PingTime>

    fun findAllByStatusNotIn(
        statuses: List<PingTimeStatus>,
        pageable: Pageable
    ): Page<PingTime>

    fun findAllByStatusNotInAndFormationDateGreaterThanEqual(
        statuses: List<PingTimeStatus>,
        from: LocalDateTime,
        pageable: Pageable
    ): Page<PingTime>

    fun findAllByStatusNotInAndFormationDateLessThanEqual(
        statuses: List<PingTimeStatus>,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<PingTime>
    
    // Методы с фильтрацией по конкретному статусу
    fun findAllByStatusNotInAndStatus(
        excludedStatuses: List<PingTimeStatus>,
        status: PingTimeStatus,
        pageable: Pageable
    ): Page<PingTime>
    
    fun findAllByStatusNotInAndStatusAndFormationDateBetween(
        excludedStatuses: List<PingTimeStatus>,
        status: PingTimeStatus,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<PingTime>
    
    fun findAllByStatusNotInAndStatusAndFormationDateGreaterThanEqual(
        excludedStatuses: List<PingTimeStatus>,
        status: PingTimeStatus,
        from: LocalDateTime,
        pageable: Pageable
    ): Page<PingTime>
    
    fun findAllByStatusNotInAndStatusAndFormationDateLessThanEqual(
        excludedStatuses: List<PingTimeStatus>,
        status: PingTimeStatus,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<PingTime>
    
    // Старые методы без пагинации для обратной совместимости
    fun findAllByStatusNotInAndFormationDateBetween(
        statuses: List<PingTimeStatus>,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): List<PingTime>

    fun findAllByStatusNotIn(statuses: List<PingTimeStatus>): List<PingTime>

    fun findAllByStatusNotInAndFormationDateGreaterThanEqual(
        statuses: List<PingTimeStatus>,
        from: LocalDateTime
    ): List<PingTime>

    fun findAllByStatusNotInAndFormationDateLessThanEqual(
        statuses: List<PingTimeStatus>,
        to: LocalDateTime
    ): List<PingTime>
}