package com.dip.pingtest.controller

import com.dip.pingtest.domain.model.PingTimeStatus
import com.dip.pingtest.service.ComponentService
import com.dip.pingtest.service.RequestService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

@Controller
class ServiceController(
    private val componentService: ComponentService,
    private val pingTimeService: RequestService
) {

    @GetMapping("/")
    fun mainPage(@RequestParam(required = false) filter: String?, model: Model): String {
        //TODO hardcoded user
        val userId = 1
        val components = componentService.getComponents(filter)
        val draftRequestId = pingTimeService.getDraftRequestIdForUser(userId)
        val requestSize = pingTimeService.getRequestItemCountForUser(userId)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("draftRequestId", draftRequestId)
        model.addAttribute("requestSize", requestSize)

        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png"))
        model.addAttribute("searchIconUrl", componentService.getStaticImageUrl("search_icon.svg"))
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        model.addAttribute("plusCircleUrl", componentService.getStaticImageUrl("plus_circle.svg"))
        model.addAttribute("requestIconUrl", componentService.getStaticImageUrl("cart.png"))

        return "main-page/main"
    }

    @GetMapping("/component/{id}")
    fun viewService(@PathVariable id: Int, model: Model): String {
        //TODO hardcoded user
        val userId = 1
        val component = componentService.getComponent(id) ?: throw RuntimeException("Component not found")
        val draftRequestId = pingTimeService.getDraftRequestIdForUser(userId)
        val requestSize = pingTimeService.getRequestItemCountForUser(userId)
        model.addAttribute("component", component)
        model.addAttribute("draftRequestId", draftRequestId)
        model.addAttribute("requestSize", requestSize)
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png"))
        model.addAttribute("requestIconUrl", componentService.getStaticImageUrl("cart.png"))

        return "details-page/component-detailed"
    }

    @GetMapping("/ping-time/{id}")
    fun viewRequest(@PathVariable id: Int, model: Model): String {
        val pingTime = pingTimeService.getRequest(id) ?: throw RuntimeException("Request not found")
        if (pingTime.status == PingTimeStatus.DELETED) {
            throw RuntimeException("Deleted requests cannot be viewed")
        }
        model.addAttribute("ping_time", pingTime)
        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png"))
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        model.addAttribute("deleteIconUrl", componentService.getStaticImageUrl("delete_icon.svg"))

        return "ping-time-page/ping-time"
    }

    @PostMapping("/ping-time/add/{componentId}")
    fun addToRequest(@PathVariable componentId: Int, httpRequest: HttpServletRequest): String {
        //TODO hardcoded user
        pingTimeService.addComponentToRequest(userId = 1, componentId)
        val referer = httpRequest.getHeader("Referer") ?: "/"
        return "redirect:$referer"
    }

    @PostMapping("/ping-time/delete/{id}")
    fun deleteRequest(@PathVariable id: Int): String {
        pingTimeService.logicalDeleteRequest(id)
        return "redirect:/"
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleException(request: HttpServletRequest, ex: RuntimeException): ModelAndView {
        val modelAndView = ModelAndView("ping-time-page/error")
        modelAndView.addObject("message", ex.message ?: "Произошла неизвестная ошибка")
        return modelAndView
    }
}