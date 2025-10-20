package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.LoginDTO
import com.dip.pingtest.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement

@RestController
class AuthController(private val service: UserService) {

    @PostMapping("/api/auth/login")
    @Operation(summary = "Authenticate user and get JWT in cookie")
    fun login(@RequestBody dto: LoginDTO, response: HttpServletResponse): Map<String, String> = service.authenticate(dto, response)

    @PostMapping("/api/auth/logout")
    @Operation(summary = "Logout and blacklist JWT")
    fun logout(response: HttpServletResponse, request: HttpServletRequest) = service.logout(response, request)

    @PostMapping("/api/auth/refresh")
    @Operation(summary = "Refresh JWT using refresh token in cookie")
    fun refresh(response: HttpServletResponse, request: HttpServletRequest): Map<String, String> = service.refresh(response, request)
}