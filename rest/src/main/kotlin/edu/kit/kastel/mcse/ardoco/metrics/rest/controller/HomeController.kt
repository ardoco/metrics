package edu.kit.kastel.mcse.ardoco.metrics.rest.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {
    @GetMapping("/")
    fun redirectToSwagger(): String = "redirect:/swagger-ui/index.html"
}
