package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.LoginDTO
import com.dip.pingtest.service.UserService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(private val service: UserService) {

    @PostMapping("/api/auth/login")
    fun login(@RequestBody dto: LoginDTO): Map<String, String> = mapOf("token" to service.authenticate(dto))

    @PostMapping("/api/auth/logout")
    fun logout() = service.logout("fake")
}