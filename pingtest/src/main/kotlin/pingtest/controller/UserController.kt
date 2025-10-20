package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.UserDTO
import com.dip.pingtest.infrastructure.dto.UserRegistrationDTO
import com.dip.pingtest.infrastructure.dto.UserUpdateDTO
import com.dip.pingtest.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Users", description = "API for user registration and profile management")
@RestController
class UserController(private val service: UserService) {

    @PostMapping("/api/users/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with username and password")
    fun register(@RequestBody dto: UserRegistrationDTO): UserDTO = service.register(dto)

    @GetMapping("/api/users/me")
    @Operation(summary = "Get current user profile", description = "Returns details of the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    fun getMe(): UserDTO = service.getUser(service.getCurrentUserId())

    @PutMapping("/api/users/me")
    @Operation(summary = "Update current user profile", description = "Updates username or password for the authenticated user")
    @SecurityRequirement(name = "bearerAuth")
    fun updateMe(@RequestBody dto: UserUpdateDTO): UserDTO = service.updateUser(service.getCurrentUserId(), dto)
}