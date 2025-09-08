package com.dip.pingtest.controller

import com.dip.pingtest.repository.ServiceRepository
import com.dip.pingtest.service.ServiceService
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable


@Controller
class ServiceController(private val service: ServiceService, val repository: ServiceRepository) {

    @GetMapping("/")
    fun main(model: Model, session: HttpSession): String {
        model.addAttribute("services", repository.getServices())

//        val cart = session.getAttribute("cart") as MutableList<*>?
//        val cartSize = cart?.size ?: 0
//        model.addAttribute("cartSize", cartSize)

        return "main/main"
    }

    @GetMapping("/service/{id}")
    fun getService(@PathVariable id: Int, model: Model): String {
        val service = service.getService(id)
        model.addAttribute("service", service)
        return "service"
    }
}