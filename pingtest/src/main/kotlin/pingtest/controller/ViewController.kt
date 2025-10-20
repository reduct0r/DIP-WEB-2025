package com.dip.pingtest.controller

import com.dip.pingtest.domain.model.enums.PingTimeStatus
import com.dip.pingtest.service.ComponentService
import com.dip.pingtest.service.PingTimeService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView

@Controller
class ViewController(
    private val componentService: ComponentService,
    private val pingTimeService: PingTimeService
) {
    @GetMapping("/")
    fun mainPage(@RequestParam(required = false) filter: String?, model: Model): String {
        //TODO hardcoded user
        val userId = 1
        val components = componentService.getComponents(filter)
        val draftRequestId = pingTimeService.getDraftTimePingIdForUser(userId)
        val requestSize = pingTimeService.getTimePingItemCountForUser(userId)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("draftTimePingId", draftRequestId)
        model.addAttribute("requestSize", requestSize)
        model.addAttribute("iconUrl", componentService.generatePresignedUrl("icon.png"))
        model.addAttribute("searchIconUrl", componentService.generatePresignedUrl("search_icon.svg"))
        model.addAttribute("pingIconUrl", componentService.generatePresignedUrl("ping_icon.svg"))
        model.addAttribute("plusCircleUrl", componentService.generatePresignedUrl("plus_circle.svg"))
        model.addAttribute("requestIconUrl", componentService.generatePresignedUrl("cart.png"))
        return "main-page/main"
    }
    @GetMapping("/server-component/{id}")
    fun viewServerComponent(@PathVariable id: Int, model: Model): String {
        //TODO hardcoded user
        val userId = 1
        val component = componentService.getComponent(id)
        val draftRequestId = pingTimeService.getDraftTimePingIdForUser(userId)
        val requestSize = pingTimeService.getTimePingItemCountForUser(userId)
        model.addAttribute("component", component)
        model.addAttribute("draftTimePingId", draftRequestId)
        model.addAttribute("requestSize", requestSize)
        model.addAttribute("pingIconUrl", componentService.generatePresignedUrl("ping_icon.svg"))
        model.addAttribute("iconUrl", componentService.generatePresignedUrl("icon.png"))
        model.addAttribute("requestIconUrl", componentService.generatePresignedUrl("cart.png"))
        return "details-page/component-detailed"
    }
    @GetMapping("/ping-time/{id}")
    fun viewTimePing(@PathVariable id: Int, model: Model): String {
        val pingTime = pingTimeService.getTimePingDomain(id) ?: throw RuntimeException("Запрос отклик сервера не найден")
        if (pingTime.status == PingTimeStatus.DELETED) {
            throw RuntimeException("Удаленный расчет отклика сервера")
        }
        val previewTotalTime = pingTime.items.sumOf { it.subtotalTime } * (pingTime.loadCoefficient ?: 1)
        model.addAttribute("ping_time", pingTime)
        model.addAttribute("previewTotalTime", previewTotalTime)
        model.addAttribute("selectedCoefficient", pingTime.loadCoefficient)
        model.addAttribute("iconUrl", componentService.generatePresignedUrl("icon.png"))
        model.addAttribute("pingIconUrl", componentService.generatePresignedUrl("ping_icon.svg"))
        model.addAttribute("deleteIconUrl", componentService.generatePresignedUrl("delete_icon.svg"))
        return "ping-time-page/ping-time"
    }
    @PostMapping("/ping-time/add/{componentId}")
    fun addToTimePing(@PathVariable componentId: Int, @RequestParam(name = "group", required = false) group: String?, httpRequest: HttpServletRequest): String {
        //TODO hardcoded user
        val userId = 1
        pingTimeService.addServerComponentToTimePing(userId, componentId, group)
        val referer = httpRequest.getHeader("Referer") ?: "/"
        return "redirect:$referer"
    }
    @PostMapping("/ping-time/delete/{id}")
    fun deleteTimePing(@PathVariable id: Int): String {
        pingTimeService.logicalDeleteTimePing(id)
        return "redirect:/"
    }
    @ExceptionHandler(RuntimeException::class)
    fun handleException(request: HttpServletRequest, ex: RuntimeException): ModelAndView {
        val modelAndView = ModelAndView("ping-time-page/error")
        modelAndView.addObject("message", ex.message ?: "Произошла неизвестная ошибка")
        return modelAndView
    }
}