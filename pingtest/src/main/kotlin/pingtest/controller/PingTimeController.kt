package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.TimePingIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.ModerateActionDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeUpdateDTO
import com.dip.pingtest.service.PingTimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Ping Time Requests", description = "API for managing ping time requests and calculations")
@RestController
@RequestMapping("/api/ping-time")
class PingTimeController(private val service: PingTimeService) {

    @GetMapping("/cart-icon")
    @Operation(summary = "Get ping time cart icon info", description = "Returns draft ID and item count for the cart icon")
    @SecurityRequirement(name = "bearerAuth")
    fun getTimePingIcon(): TimePingIconDTO = service.getTimePingIcon()

    @GetMapping
    @Operation(summary = "Get all ping time requests", description = "Returns list of ping time requests with optional filters for status and date range")
    @SecurityRequirement(name = "bearerAuth")
    fun getAll(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?
    ): List<PingTimeDTO> = service.getTimePings(status, fromDate, toDate)

    @GetMapping("/{id}")
    @Operation(summary = "Get one ping time request by ID", description = "Returns details of a specific ping time request")
    @SecurityRequirement(name = "bearerAuth")
    fun getOne(@PathVariable id: Int): PingTimeDTO = service.getTimePing(id)

    @PutMapping("/{id}/form")
    @Operation(summary = "Form a ping time request from draft", description = "Changes draft status to formed and sets formation date")
    @SecurityRequirement(name = "bearerAuth")
    fun form(@PathVariable id: Int): PingTimeDTO = service.formTimePing(id)

    @PutMapping("/{id}/moderate")
    @Operation(summary = "Moderate a ping time request", description = "Completes or rejects a formed request (moderator only)")
    @SecurityRequirement(name = "bearerAuth")
    fun moderate(@PathVariable id: Int, @RequestBody dto: ModerateActionDTO): PingTimeDTO = service.moderateTimePing(id, dto.action)

    @PutMapping("/{id}")
    @Operation(summary = "Update ping time request", description = "Updates load coefficient and recalculates total time")
    @SecurityRequirement(name = "bearerAuth")
    fun updateTimePing(@PathVariable id: Int, @RequestBody dto: PingTimeUpdateDTO): PingTimeDTO {
        return service.updateTimePing(id, dto)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ping time request", description = "Logically deletes a draft or formed request")
    @SecurityRequirement(name = "bearerAuth")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        service.deleteTimePing(id)
        return ResponseEntity.noContent().build()
    }

    // M-M
    @PutMapping("/{requestId}/items/{componentId}")
    @Operation(summary = "Update item in ping time request", description = "Updates quantity of a component in the request")
    @SecurityRequirement(name = "bearerAuth")
    fun updateItem(@PathVariable requestId: Int, @PathVariable componentId: Int, @RequestBody dto: ItemUpdateDTO): PingTimeDTO =
        service.updateItem(requestId, componentId, dto)

    @DeleteMapping("/{requestId}/items/{componentId}")
    @Operation(summary = "Delete item from ping time request", description = "Removes a component from the request")
    @SecurityRequirement(name = "bearerAuth")
    fun deleteItem(@PathVariable requestId: Int, @PathVariable componentId: Int): PingTimeDTO = service.deleteItem(requestId, componentId)
}