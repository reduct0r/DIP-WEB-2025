package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.ComponentDTO
import com.dip.pingtest.infrastructure.dto.PingTimeDTO
import com.dip.pingtest.service.ComponentService
import com.dip.pingtest.service.PingTimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Server Components", description = "API for managing server components")
@RestController
@RequestMapping("/api/server-components")
class ComponentController(private val service: ComponentService, private val pingTimeService: PingTimeService) {

    @GetMapping
    @Operation(summary = "Get all components", description = "Returns list of components with optional filter")
    fun getAll(@RequestParam(required = false) filter: String?): List<ComponentDTO> = service.getComponents(filter)

    @GetMapping("/{id}")
    @Operation(summary = "Get one component by ID")
    fun getOne(@PathVariable id: Int): ComponentDTO = service.getComponent(id)

    @PostMapping
    @Operation(summary = "Create new component")
    @SecurityRequirement(name = "bearerAuth")  // Требует JWT
    fun create(@RequestBody dto: ComponentDTO): ComponentDTO = service.createComponent(dto)

    @PutMapping("/{id}")
    @Operation(summary = "Update component by ID")
    @SecurityRequirement(name = "bearerAuth")
    fun update(@PathVariable id: Int, @RequestBody dto: ComponentDTO): ComponentDTO = service.updateComponent(id, dto)

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete component by ID")
    @SecurityRequirement(name = "bearerAuth")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        service.deleteComponent(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/image")
    @Operation(summary = "Upload image for component")
    @SecurityRequirement(name = "bearerAuth")
    fun uploadImage(@PathVariable id: Int, @RequestParam("file") file: MultipartFile): String = service.uploadImage(id, file)

    @PostMapping("/{componentId}/add-to-draft")
    @Operation(summary = "Add component to draft ping time")
    @SecurityRequirement(name = "bearerAuth")
    fun addToDraft(@PathVariable componentId: Int): PingTimeDTO = pingTimeService.addServerComponentToDraft(componentId)
}