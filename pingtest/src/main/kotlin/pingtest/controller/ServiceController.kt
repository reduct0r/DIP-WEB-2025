package com.dip.pingtest.controller

import com.dip.pingtest.domain.Component
import com.dip.pingtest.repository.ComponentRepository
import com.dip.pingtest.service.ComponentService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam


@Controller
class ServiceController(private val component: ComponentService, val repository: ComponentRepository) {

    @GetMapping("/")
    fun mainPage(@RequestParam(required = false) filter: String?, model: Model): String {
        val components = repository.getComponents(filter)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("cartSize", repository.getRequestItems().size)
        return "main/main"
    }

    @GetMapping("/component/{id}")
    fun viewService(@PathVariable id: Int, model: Model): String {
        val componentItem = repository.getComponent(id)
        model.addAttribute("component",  componentItem)
        return "component/card-view"
    }

    @GetMapping("/cart/{id}")
    fun viewCart(@PathVariable id: Int, model: Model): String {
        val componentItems = repository.getRequestItems().values.toList()
        model.addAttribute("components", componentItems)
        model.addAttribute("cartSize", repository.getRequestItems().size)
        return "cart/request"
    }
}