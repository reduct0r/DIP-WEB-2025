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
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.NoSuchElementException

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
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        return UserDTO(user.id, user.username, user.role == Role.MODERATOR)
    }

    fun updateUser(userId: Int, dto: UserUpdateDTO): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { NoSuchElementException("User not found") }
        dto.username?.let { user.username = it }
        dto.password?.let { user.password = passwordEncoder.encode(it) }
        val saved = userRepository.save(user)
        return UserDTO(saved.id, saved.username, saved.role == Role.MODERATOR)
    }

    fun authenticate(dto: LoginDTO, request: HttpServletRequest, response: HttpServletResponse): Map<String, String> {
        val user = userRepository.findByUsername(dto.username) ?: throw BadCredentialsException("Invalid credentials")
        if (!passwordEncoder.matches(dto.password, user.password)) throw BadCredentialsException("Invalid credentials")
        val token = jwtService.generateToken(user.id, user.role.name)
        val refreshToken = jwtService.generateRefreshToken(user.id)
        addCookie(response, "jwt", token, (jwtService.expiration / 1000).toInt(), request)
        addCookie(response, "refresh", refreshToken, (jwtService.refreshExpiration / 1000).toInt(), request)
        return mapOf("message" to "Authenticated")
    }

    fun logout(response: HttpServletResponse, request: HttpServletRequest) {
        val token = request.cookies?.find { it.name == "jwt" }?.value ?: return
        blacklistService.addToBlacklist(token, jwtService.expiration)
        deleteCookie(response, "jwt")
        deleteCookie(response, "refresh")
    }

    fun refresh(response: HttpServletResponse, request: HttpServletRequest): Map<String, String> {
        val refreshToken = request.cookies?.find { it.name == "refresh" }?.value ?: throw BadCredentialsException("No refresh token")
        if (!jwtService.validateToken(refreshToken) || blacklistService.isBlacklisted(refreshToken)) {
            throw BadCredentialsException("Invalid refresh token")
        }
        val claims = jwtService.getClaims(refreshToken)
        val userId = claims["userId"] as Int
        val user = userRepository.findById(userId).orElseThrow()
        val newToken = jwtService.generateToken(userId, user.role.name)
        val newRefresh = jwtService.generateRefreshToken(userId)
        addCookie(response, "jwt", newToken, (jwtService.expiration / 1000).toInt(), request)
        addCookie(response, "refresh", newRefresh, (jwtService.refreshExpiration / 1000).toInt(), request)
        return mapOf("message" to "Refreshed")
    }

    fun getCurrentUserId(): Int {
        val auth = SecurityContextHolder.getContext().authentication
        return auth.principal as Int
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int, request: HttpServletRequest) {
        print(request.getHeader("Origin"))
        val cookie = ResponseCookie.from(name, value)
            .httpOnly(true)
            .sameSite("None")
            .secure(
                !request.getHeader("Origin").isNullOrEmpty()
            )
            .path("/")
            .maxAge(maxAge.toLong())
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = Cookie(name, null)
        cookie.maxAge = 0
        response.addCookie(cookie)
    }
}