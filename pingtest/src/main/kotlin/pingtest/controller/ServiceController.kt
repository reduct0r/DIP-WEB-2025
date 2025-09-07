package com.dip.pingtest.controller

import com.dip.pingtest.service.ServiceService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ServiceController(private val service: ServiceService) {

    @GetMapping("/hello")
    fun hello(model: Model): String {
        val data = service.getWithTime()
        model.addAttribute("time", data["time"])
        model.addAttribute("services", data["services"])
        return "test-index"
    }
}