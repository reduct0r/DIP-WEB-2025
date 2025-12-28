package com.dip.pingtest.infrastructure.dto

data class PaginatedResponseDTO<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val queryTimeMs: Long? = null
)

