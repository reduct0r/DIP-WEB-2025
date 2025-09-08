package com.dip.pingtest.controller

import com.dip.pingtest.service.ServiceService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class ServiceController(private val service: ServiceService) {

    @GetMapping("/services")
    fun getServices(model: Model): String {
        val data = service.getWithTime()
//        model.addAttribute("time", data["time"])
//        model.addAttribute("services", data["services"])
        return "main/card"
    }

    @GetMapping("/service/{id}")
    fun getService(@PathVariable id: Int, model: Model): String {
        val service = service.getService(id)
        model.addAttribute("service", service)
        return "service"
    }
}