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

@Tag(name = "Аутентификация", description = "API для аутентификации пользователей и управления сессиями")
@RestController
class AuthController(private val service: UserService) {

    @PostMapping("/api/auth/login")
    @Operation(summary = "Аутентифицировать пользователя и установить JWT в cookie", description = "Входит в систему пользователя с учетными данными и устанавливает JWT и токены обновления в cookies")
    fun login(@RequestBody dto: LoginDTO, response: HttpServletResponse): Map<String, String> = service.authenticate(dto, response)

    @PostMapping("/api/auth/logout")
    @Operation(summary = "Выйти из системы пользователя и внести JWT в черный список", description = "Инвалидирует текущий JWT, добавляя его в черный список в Redis и очищает cookies")
    fun logout(response: HttpServletResponse, request: HttpServletRequest) = service.logout(response, request)

    @PostMapping("/api/auth/refresh")
    @Operation(summary = "Обновить JWT используя токен обновления", description = "Генерирует новый JWT и токен обновления, используя существующий токен обновления в cookies")
    fun refresh(response: HttpServletResponse, request: HttpServletRequest): Map<String, String> = service.refresh(response, request)
}