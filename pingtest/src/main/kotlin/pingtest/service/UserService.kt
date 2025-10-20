package com.dip.pingtest.service

import com.dip.pingtest.domain.model.User
import com.dip.pingtest.domain.model.enums.Role
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.LoginDTO
import com.dip.pingtest.infrastructure.dto.UserDTO
import com.dip.pingtest.infrastructure.dto.UserRegistrationDTO
import com.dip.pingtest.infrastructure.dto.UserUpdateDTO
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val blacklistService: RedisBlacklistService
) {
    fun register(dto: UserRegistrationDTO): UserDTO {
        if (userRepository.findByUsername(dto.username) != null) {
            throw RuntimeException("Username already exists")
        }
        val user = User(username = dto.username, password = passwordEncoder.encode(dto.password), role = Role.USER)
        val saved = userRepository.save(user)
        return UserDTO(saved.id, saved.username, saved.role == Role.MODERATOR)
    }

    fun getUser(userId: Int): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        return UserDTO(user.id, user.username, user.role == Role.MODERATOR)
    }

    fun updateUser(userId: Int, dto: UserUpdateDTO): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        dto.username?.let { user.username = it }
        dto.password?.let { user.password = passwordEncoder.encode(it) }
        val saved = userRepository.save(user)
        return UserDTO(saved.id, saved.username, saved.role == Role.MODERATOR)
    }

    fun authenticate(dto: LoginDTO, response: HttpServletResponse): Map<String, String> {
        val user = userRepository.findByUsername(dto.username) ?: throw RuntimeException("Invalid credentials")
        if (!passwordEncoder.matches(dto.password, user.password)) throw RuntimeException("Invalid credentials")
        val token = jwtService.generateToken(user.id, user.role.name)
        val refreshToken = jwtService.generateRefreshToken(user.id)
        addCookie(response, "jwt", token, (jwtService.expiration / 1000).toInt())
        addCookie(response, "refresh", refreshToken, (jwtService.refreshExpiration / 1000).toInt())
        return mapOf("message" to "Authenticated")
    }

    fun logout(response: HttpServletResponse, request: HttpServletRequest) {
        val token = request.cookies?.find { it.name == "jwt" }?.value ?: return
        blacklistService.addToBlacklist(token, jwtService.expiration)
        deleteCookie(response, "jwt")
        deleteCookie(response, "refresh")
    }

    fun refresh(response: HttpServletResponse, request: HttpServletRequest): Map<String, String> {
        val refreshToken = request.cookies?.find { it.name == "refresh" }?.value ?: throw RuntimeException("No refresh token")
        if (!jwtService.validateToken(refreshToken) || blacklistService.isBlacklisted(refreshToken)) {
            throw RuntimeException("Invalid refresh token")
        }
        val claims = jwtService.getClaims(refreshToken)
        val userId = claims["userId"] as Int
        val user = userRepository.findById(userId).orElseThrow()
        val newToken = jwtService.generateToken(userId, user.role.name)
        val newRefresh = jwtService.generateRefreshToken(userId)
        addCookie(response, "jwt", newToken, (jwtService.expiration / 1000).toInt())
        addCookie(response, "refresh", newRefresh, (jwtService.refreshExpiration / 1000).toInt())
        return mapOf("message" to "Refreshed")
    }

    fun getCurrentUserId(): Int {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Int
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = Cookie(name, value)
        cookie.isHttpOnly = true
        cookie.secure = true
        cookie.path = "/"
        cookie.maxAge = maxAge
        response.addCookie(cookie)
    }

    private fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = Cookie(name, null)
        cookie.maxAge = 0
        response.addCookie(cookie)
    }
}