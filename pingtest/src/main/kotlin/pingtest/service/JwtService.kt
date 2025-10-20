package com.dip.pingtest.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    var expiration: Long = 3600000 // 1 hour

    @Value("\${jwt.refresh-expiration}")
    var refreshExpiration: Long = 86400000 // 24 hours

    fun generateToken(userId: Int, role: String): String {
        return Jwts.builder()
            .claim("userId", userId)
            .claim("role", role)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateRefreshToken(userId: Int): String {
        return Jwts.builder()
            .claim("userId", userId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getClaims(token: String): Claims {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).payload
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secret)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}