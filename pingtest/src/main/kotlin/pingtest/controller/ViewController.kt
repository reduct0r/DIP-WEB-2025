package com.dip.pingtest.controller

import com.dip.pingtest.domain.model.RequestStatus
import com.dip.pingtest.service.ComponentService
import com.dip.pingtest.service.RequestService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
class ServiceController(
    private val componentService: ComponentService,
    private val requestService: RequestService
) {

    @GetMapping("/")
    fun mainPage(@RequestParam(required = false) filter: String?, model: Model): String {
        val components = componentService.getComponents(filter)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("requestSize", requestService.getRequestItemCount(1))
        model.addAttribute("minioBaseUrl", "")

        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png"))
        model.addAttribute("searchIconUrl", componentService.getStaticImageUrl("search_icon.svg"))
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        model.addAttribute("plusCircleUrl", componentService.getStaticImageUrl("plus_circle.svg"))
        model.addAttribute("requestIconUrl", componentService.getStaticImageUrl("cart.png"))
        return "main-page/main"
    }

    @GetMapping("/component/{id}")
    fun viewService(@PathVariable id: Int, model: Model): String {
        val component = componentService.getComponent(id) ?: throw RuntimeException("Component not found")
        model.addAttribute("component", component)
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        return "details-page/component-detailed"
    }

    @GetMapping("/request/{id}")
    fun viewRequest(@PathVariable id: Int, model: Model): String {
        val request = requestService.getRequest(id) ?: throw RuntimeException("Request not found")
        if (request.status == RequestStatus.DELETED) {
            throw RuntimeException("Deleted requests cannot be viewed")  // As per TZ
        }
        val itemsWithComponents = request.items.map { it to it.component }
        model.addAttribute("request", request)
        model.addAttribute("itemsWithComponents", itemsWithComponents)
        return "request-page/request"
    }

    @PostMapping("/request/add/{componentId}")
    fun addToRequest(@PathVariable componentId: Int): String {
        val request = requestService.addComponentToRequest(1, componentId)  // Hardcoded userId=1; add auth later
        return "redirect:/request/${request.id}"
    }

    @PostMapping("/request/delete/{id}")
    fun deleteRequest(@PathVariable id: Int): String {
        requestService.logicalDeleteRequest(id)
        return "redirect:/"
    }
}