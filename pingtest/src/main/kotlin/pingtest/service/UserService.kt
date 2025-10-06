package com.dip.pingtest.service

import com.dip.pingtest.domain.model.User
import com.dip.pingtest.domain.repository.UserRepository
import com.dip.pingtest.infrastructure.dto.LoginDTO
import com.dip.pingtest.infrastructure.dto.UserDTO
import com.dip.pingtest.infrastructure.dto.UserRegistrationDTO
import com.dip.pingtest.infrastructure.dto.UserUpdateDTO
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository) {

    fun register(dto: UserRegistrationDTO): UserDTO {
        if (userRepository.findByUsername(dto.username) != null) {
            throw RuntimeException("Username already exists")
        }
        val user = User(username = dto.username, password = dto.password) // TODO: Hash password
        val saved = userRepository.save(user)
        return UserDTO(saved.id, saved.username, saved.isModerator)
    }

    fun getUser(userId: Int): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        return UserDTO(user.id, user.username, user.isModerator)
    }

    fun updateUser(userId: Int, dto: UserUpdateDTO): UserDTO {
        val user = userRepository.findById(userId).orElseThrow { RuntimeException("User not found") }
        dto.username?.let { user.username = it }
        dto.password?.let { user.password = it } // TODO: Hash
        val saved = userRepository.save(user)
        return UserDTO(saved.id, saved.username, saved.isModerator)
    }

    fun authenticate(dto: LoginDTO): String {
        val user = userRepository.findByUsername(dto.username) ?: throw RuntimeException("Invalid credentials")
        if (user.password != dto.password) throw RuntimeException("Invalid credentials") // TODO: Compare hashed
        return "fake-jwt-token-${user.id}" // TODO: Real JWT
    }

    fun logout(token: String) {
        // TODO: Invalidate token if using sessions/JWT blacklist
    }
}