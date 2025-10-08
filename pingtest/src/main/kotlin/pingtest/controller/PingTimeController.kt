package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.TimePingIconDTO
import com.dip.pingtest.infrastructure.dto.ItemUpdateDTO
import com.dip.pingtest.infrastructure.dto.ModerateActionDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.infrastructure.dto.PingTimeUpdateDTO
import com.dip.pingtest.service.PingTimeService
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

@RestController
@RequestMapping("/api/ping-time")
class PingTimeController(private val service: PingTimeService) {

    @GetMapping("/cart-icon")
    fun getTimePingIcon(): TimePingIconDTO = service.getTimePingIcon()

    @GetMapping
    fun getAll(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?
    ): List<PingTimeDTO> = service.getTimePings(status, fromDate, toDate)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Int): PingTimeDTO = service.getTimePing(id)

    @PutMapping("/{id}/form")
    fun form(@PathVariable id: Int): PingTimeDTO = service.formTimePing(id)

    @PutMapping("/{id}/moderate")
    fun moderate(@PathVariable id: Int, @RequestBody dto: ModerateActionDTO): PingTimeDTO = service.moderateTimePing(id, dto.action)

    @PutMapping("/{id}")
    fun updateTimePing(@PathVariable id: Int, @RequestBody dto: PingTimeUpdateDTO): PingTimeDTO {
        return service.updateTimePing(id, dto)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        service.deleteTimePing(id)
        return ResponseEntity.noContent().build()
    }

    // M-M
    @PutMapping("/{requestId}/items/{componentId}")
    fun updateItem(@PathVariable requestId: Int, @PathVariable componentId: Int, @RequestBody dto: ItemUpdateDTO): PingTimeDTO =
        service.updateItem(requestId, componentId, dto)

    @DeleteMapping("/{requestId}/items/{componentId}")
    fun deleteItem(@PathVariable requestId: Int, @PathVariable componentId: Int): PingTimeDTO = service.deleteItem(requestId, componentId)
}