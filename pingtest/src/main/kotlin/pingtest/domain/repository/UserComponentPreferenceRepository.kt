package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.UserComponentPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserComponentPreferenceRepository : JpaRepository<UserComponentPreference, Int> {
    fun findByUserIdAndComponentId(userId: Int, componentId: Int): UserComponentPreference?
}