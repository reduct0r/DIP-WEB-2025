package com.dip.pingtest.config

import com.dip.pingtest.controller.ErrorResponse
import com.dip.pingtest.service.JwtService
import com.dip.pingtest.service.RedisBlacklistService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

@Configuration
@EnableMethodSecurity
class SecurityConfig(private val jwtService: JwtService, private val blacklistService: RedisBlacklistService) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager = config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { it.frameOptions { it.disable() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()
                it.requestMatchers(AntPathRequestMatcher("/api/server-components/**", "GET")).permitAll()
                it.requestMatchers("/api/server-components/**").hasAnyRole("USER", "MODERATOR")
                it.requestMatchers("/api/ping-time/moderate/**").hasRole("MODERATOR")
                it.requestMatchers("/api/ping-time/*/form").hasRole("MODERATOR")
                it.requestMatchers("/api/users/register").permitAll()
                it.requestMatchers("/**").authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, authException ->
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                }
                it.accessDeniedHandler { request, response, accessDeniedException ->
                    response.status = HttpServletResponse.SC_FORBIDDEN
                }
            }
            .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun jwtFilter(): OncePerRequestFilter {
        return object : OncePerRequestFilter() {
            override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
                val token = request.cookies?.find { it.name == "jwt" }?.value
                if (token != null && jwtService.validateToken(token) && !blacklistService.isBlacklisted(token)) {
                    val claims = jwtService.getClaims(token)
                    val auth = UsernamePasswordAuthenticationToken(
                        claims["userId"],
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_${claims["role"]}"))
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
                filterChain.doFilter(request, response)
            }
        }
    }
}