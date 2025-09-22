package com.dip.pingtest.controller

import com.dip.pingtest.domain.model.RequestStatus
import com.dip.pingtest.service.ComponentService
import com.dip.pingtest.service.RequestService
import jakarta.servlet.http.HttpServletRequest
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
        val userId = 1 // Hardcoded for demo; in production, use authentication
        val components = componentService.getComponents(filter)
        val draftRequestId = requestService.getDraftRequestIdForUser(userId)
        val requestSize = requestService.getRequestItemCountForUser(userId)
        model.addAttribute("components", components)
        model.addAttribute("filter", filter ?: "")
        model.addAttribute("draftRequestId", draftRequestId)
        model.addAttribute("requestSize", requestSize)
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
        val userId = 1 // Hardcoded for demo; in production, use authentication
        val component = componentService.getComponent(id) ?: throw RuntimeException("Component not found")
        val draftRequestId = requestService.getDraftRequestIdForUser(userId)
        val requestSize = requestService.getRequestItemCountForUser(userId)
        model.addAttribute("component", component)
        model.addAttribute("draftRequestId", draftRequestId)
        model.addAttribute("requestSize", requestSize)
        model.addAttribute("pingIconUrl", componentService.getStaticImageUrl("ping_icon.svg"))
        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png"))
        model.addAttribute("requestIconUrl", componentService.getStaticImageUrl("cart.png"))
        // Add other attributes if needed for the details page header/footer

        return "details-page/component-detailed"
    }

    @GetMapping("/request/{id}")
    fun viewRequest(@PathVariable id: Int, model: Model): String {
        val request = requestService.getRequest(id) ?: throw RuntimeException("Request not found")
        if (request.status == RequestStatus.DELETED) {
            throw RuntimeException("Deleted requests cannot be viewed")
        }
        model.addAttribute("request", request)
        model.addAttribute("minioBaseUrl", "") // If needed for images
        model.addAttribute("iconUrl", componentService.getStaticImageUrl("icon.png")) // For header
        // Optionally add draftRequestId and requestSize if you want the cart icon on this page too

        return "request-page/request"
    }

    @PostMapping("/request/add/{componentId}")
    fun addToRequest(@PathVariable componentId: Int, httpRequest: HttpServletRequest): String {
        val userId = 1 // Hardcoded for demo; in production, use authentication
        requestService.addComponentToRequest(userId, componentId)
        val referer = httpRequest.getHeader("Referer") ?: "/"
        return "redirect:$referer"
    }

    @PostMapping("/request/delete/{id}")
    fun deleteRequest(@PathVariable id: Int): String {
        requestService.logicalDeleteRequest(id)
        return "redirect:/"
    }
}