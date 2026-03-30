package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.service.A2ARouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/a2a")
@RequiredArgsConstructor
@Slf4j
public class A2AController {

    private final A2ARouterService routerService;

    @GetMapping
    public String hub(Model model) {
        model.addAttribute("agents", routerService.discoverAgents());
        model.addAttribute("recentMessages", routerService.getRecentMessages(20));
        model.addAttribute("pendingCount", routerService.countPending());
        return "a2a/hub";
    }
}
