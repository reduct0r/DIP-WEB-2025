package com.dip.pingtest.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import java.util.NoSuchElementException
import org.springframework.web.context.request.WebRequest

@Tag(name = "Error Handling", description = "Global exception handlers for API errors")
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Bad Request",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Resource not found",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.FORBIDDEN.value(),
            error = HttpStatus.FORBIDDEN.reasonPhrase,
            message = ex.message ?: "Access denied",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = ex.message ?: "Unauthorized",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = ex.message ?: "An unexpected error occurred",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}