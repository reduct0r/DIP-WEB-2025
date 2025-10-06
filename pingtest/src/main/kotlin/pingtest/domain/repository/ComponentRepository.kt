package com.dip.pingtest.domain.repository

import com.dip.pingtest.domain.model.ServerComponent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ComponentRepository : JpaRepository<ServerComponent, Int> {
    fun findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(filter: String, filter2: String): List<ServerComponent>
    fun findByTitle(title: String): ServerComponent?
}