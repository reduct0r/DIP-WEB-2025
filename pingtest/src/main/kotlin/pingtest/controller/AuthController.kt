package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.LoginDTO
import com.dip.pingtest.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Authentication", description = "API for user authentication and session management")
@RestController
class AuthController(private val service: UserService) {

    @PostMapping("/api/auth/login")
    @Operation(summary = "Authenticate user and set JWT in cookie", description = "Logs in the user with credentials and sets JWT and refresh tokens in cookies")
    fun login(@RequestBody dto: LoginDTO, response: HttpServletResponse): Map<String, String> = service.authenticate(dto, response)

    @PostMapping("/api/auth/logout")
    @Operation(summary = "Logout user and blacklist JWT", description = "Invalidates the current JWT by adding it to the blacklist in Redis and clears cookies")
    fun logout(response: HttpServletResponse, request: HttpServletRequest) = service.logout(response, request)

    @PostMapping("/api/auth/refresh")
    @Operation(summary = "Refresh JWT using refresh token", description = "Generates a new JWT and refresh token using the existing refresh token in cookies")
    fun refresh(response: HttpServletResponse, request: HttpServletRequest): Map<String, String> = service.refresh(response, request)
}