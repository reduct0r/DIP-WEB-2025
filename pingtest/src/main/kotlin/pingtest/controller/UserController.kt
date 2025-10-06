package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.UserDTO
import com.dip.pingtest.infrastructure.dto.UserRegistrationDTO
import com.dip.pingtest.infrastructure.dto.UserUpdateDTO
import com.dip.pingtest.service.PingTimeService
import com.dip.pingtest.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val service: UserService) {

    @PostMapping("/api/users/register")
    fun register(@RequestBody dto: UserRegistrationDTO): UserDTO = service.register(dto)

    @GetMapping("/api/users/me")
    fun getMe(): UserDTO = service.getUser(PingTimeService.FIXED_CREATOR_ID)

    @PutMapping("/api/users/me")
    fun updateMe(@RequestBody dto: UserUpdateDTO): UserDTO = service.updateUser(PingTimeService.FIXED_CREATOR_ID, dto)
}