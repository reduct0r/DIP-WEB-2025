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

@Tag(name = "Запросы времени отклика", description = "API для управления запросами времени отклика и расчетами")
@RestController
@RequestMapping("/api/ping-time")
class PingTimeController(private val service: PingTimeService) {

    @GetMapping("/cart-icon")
    @Operation(summary = "Получить информацию иконки корзины времени пинга", description = "Возвращает ID черновика и количество элементов для иконки корзины")
    @SecurityRequirement(name = "bearerAuth")
    fun getTimePingIcon(): TimePingIconDTO = service.getTimePingIcon()

    @GetMapping
    @Operation(summary = "Получить все запросы времени пинга", description = "Возвращает список запросов времени пинга с опциональными фильтрами по статусу и диапазону дат")
    @SecurityRequirement(name = "bearerAuth")
    fun getAll(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) fromDate: String?,
        @RequestParam(required = false) toDate: String?
    ): List<PingTimeDTO> = service.getTimePings(status, fromDate, toDate)

    @GetMapping("/{id}")
    @Operation(summary = "Получить один запрос времени пинга по ID", description = "Возвращает детали конкретного запроса времени пинга")
    @SecurityRequirement(name = "bearerAuth")
    fun getOne(@PathVariable id: Int): PingTimeDTO = service.getTimePing(id)

    @PutMapping("/{id}/form")
    @Operation(summary = "Сформировать запрос времени пинга из черновика", description = "Изменяет статус черновика на сформированный и устанавливает дату формирования")
    @SecurityRequirement(name = "bearerAuth")
    fun form(@PathVariable id: Int): PingTimeDTO = service.formTimePing(id)

    @PutMapping("/{id}/moderate")
    @Operation(summary = "Модерировать запрос времени пинга", description = "Завершает или отклоняет сформированный запрос (только модератор)")
    @SecurityRequirement(name = "bearerAuth")
    fun moderate(@PathVariable id: Int, @RequestBody dto: ModerateActionDTO): PingTimeDTO = service.moderateTimePing(id, dto.action)

    @PutMapping("/{id}")
    @Operation(summary = "Обновить запрос времени пинга", description = "Обновляет коэффициент нагрузки и пересчитывает общее время")
    @SecurityRequirement(name = "bearerAuth")
    fun updateTimePing(@PathVariable id: Int, @RequestBody dto: PingTimeUpdateDTO): PingTimeDTO {
        return service.updateTimePing(id, dto)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запрос времени пинга", description = "Логически удаляет черновик или сформированный запрос")
    @SecurityRequirement(name = "bearerAuth")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        service.deleteTimePing(id)
        return ResponseEntity.noContent().build()
    }

    // M-M
    @PutMapping("/{requestId}/items/{componentId}")
    @Operation(summary = "Обновить элемент в запросе времени пинга", description = "Обновляет количество компонента в запросе")
    @SecurityRequirement(name = "bearerAuth")
    fun updateItem(@PathVariable requestId: Int, @PathVariable componentId: Int, @RequestBody dto: ItemUpdateDTO): PingTimeDTO =
        service.updateItem(requestId, componentId, dto)

    @DeleteMapping("/{requestId}/items/{componentId}")
    @Operation(summary = "Удалить элемент из запроса времени пинга", description = "Удаляет компонент из запроса")
    @SecurityRequirement(name = "bearerAuth")
    fun deleteItem(@PathVariable requestId: Int, @PathVariable componentId: Int): PingTimeDTO = service.deleteItem(requestId, componentId)
}