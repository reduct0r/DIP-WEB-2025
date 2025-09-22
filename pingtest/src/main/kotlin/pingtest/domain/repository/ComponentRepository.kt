package com.dip.pingtest.domain.repository  // Add 'com.dip.' prefix

import com.dip.pingtest.domain.model.Component
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ComponentRepository : JpaRepository<Component, Int> {
    fun findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(title: String, description: String): List<Component>
    // Additional queries if needed
}