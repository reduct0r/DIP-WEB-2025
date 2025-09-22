package com.dip.pingtest.controller

import com.dip.pingtest.repository.ComponentRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam


@Controller
class ServiceController(val repository: ComponentRepository) {

    @GetMapping("/")
    fun mainPage(@RequestParam(required = false) filter: String?, model: Model): String {
        val components = repository.getComponents(filter)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("requestSize", repository.getRequestItemCount(id = 1))
        model.addAttribute("minioBaseUrl", "http://localhost:9000/main/images/")
        return "main-page/main"
    }

    @GetMapping("/component/{id}")
    fun viewService(@PathVariable id: Int, model: Model): String {
        val componentItem = repository.getComponent(id)
        model.addAttribute("component",  componentItem)
        model.addAttribute("minioBaseUrl", "http://localhost:9000/main/images/")
        return "details-page/component-detailed"
    }

    @GetMapping("/request/{id}")
    fun viewRequest(@PathVariable id: Int, model: Model): String {
        val request = repository.getRequest(id) ?: throw RuntimeException("Request not found")
        model.addAttribute("request", request)
        model.addAttribute("minioBaseUrl", "http://localhost:9000/main/images/")
        return "request-page/request"
    }
}