package com.dip.pingtest.controller

import com.dip.pingtest.repository.ComponentRepository
import com.dip.pingtest.service.ComponentService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable


@Controller
class ServiceController(private val component: ComponentService, val repository: ComponentRepository) {

    @GetMapping("/")
    fun main(model: Model, session: HttpSession): String {
        model.addAttribute("components", repository.getComponents())

//        val cart = session.getAttribute("cart") as MutableList<*>?
//        val cartSize = cart?.size ?: 0
//        model.addAttribute("cartSize", cartSize)

        return "main/main"
    }

    @GetMapping("/component/{id}")
    fun viewService(@PathVariable id: Int, model: Model): String {
        val componentItem = repository.getComponent(id)
        model.addAttribute("component",  componentItem)
        return "component/card-view"
    }
}