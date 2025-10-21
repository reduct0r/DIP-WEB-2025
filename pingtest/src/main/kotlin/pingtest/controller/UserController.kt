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

@Tag(name = "Пользователи", description = "API для регистрации пользователей и управления профилем")
@RestController
class UserController(private val service: UserService) {

    @PostMapping("/api/users/register")
    @Operation(summary = "Зарегистрировать нового пользователя", description = "Создает новую учетную запись пользователя с именем пользователя и паролем")
    fun register(@RequestBody dto: UserRegistrationDTO): UserDTO = service.register(dto)

    @GetMapping("/api/users/me")
    @Operation(summary = "Получить профиль текущего пользователя", description = "Возвращает детали аутентифицированного пользователя")
    @SecurityRequirement(name = "bearerAuth")
    fun getMe(): UserDTO = service.getUser(service.getCurrentUserId())

    @PutMapping("/api/users/me")
    @Operation(summary = "Обновить профиль текущего пользователя", description = "Обновляет имя пользователя или пароль для аутентифицированного пользователя")
    @SecurityRequirement(name = "bearerAuth")
    fun updateMe(@RequestBody dto: UserUpdateDTO): UserDTO = service.updateUser(service.getCurrentUserId(), dto)
}