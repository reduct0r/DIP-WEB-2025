package com.dip.pingtest.controller

import com.dip.pingtest.infrastructure.dto.ComponentDTO
import com.dip.pingtest.service.ComponentService
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
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/server-components")
class ComponentController(private val service: ComponentService) {

    @GetMapping
    fun getAll(@RequestParam(required = false) filter: String?): List<ComponentDTO> = service.getComponents(filter)

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Int): ComponentDTO = service.getComponent(id)

    @PostMapping
    fun create(@RequestBody dto: ComponentDTO): ComponentDTO = service.createComponent(dto)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Int, @RequestBody dto: ComponentDTO): ComponentDTO = service.updateComponent(id, dto)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Int): ResponseEntity<Void> {
        service.deleteComponent(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/image")
    fun uploadImage(@PathVariable id: Int, @RequestParam("file") file: MultipartFile): String = service.uploadImage(id, file)
}