package com.barinventory.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.config.SecurityUtils;
import com.barinventory.entities.InventorySession;
import com.barinventory.services.InventorySessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions")
public class InventorySessionController {

    private final InventorySessionService sessionService;

    // GET /sessions/create-page
    @GetMapping("/create-page")
    public String createSessionPage(Model model) {
        Long barId = SecurityUtils.getBarId();
        model.addAttribute("barId", barId);
        return "session/create-session";
    }

    // POST /sessions/create
    @PostMapping("/create")
    public String createSession(@RequestParam String sessionName) {
        Long barId = SecurityUtils.getBarId();
        InventorySession session = sessionService.createSession(barId, sessionName);
        return "redirect:/sessions/dashboard/" + session.getSessionId();
    }

    // GET /sessions/dashboard/{sessionId}
    @GetMapping("/dashboard/{sessionId}")
    public String sessionDashboard(@PathVariable Long sessionId, Model model) {
        Long barId = SecurityUtils.getBarId();
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("barId", barId);
        return "session/session-dashboard";
    }

    // GET /sessions/summary/{sessionId}
    @GetMapping("/summary/{sessionId}")
    public String sessionSummary(@PathVariable Long sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("stockroomSales", 5000);
        model.addAttribute("wellSales", 4500);
        return "reports/session-summary";
    }

    // POST /sessions/close/{sessionId}
    @PostMapping("/close/{sessionId}")
    public String closeSession(@PathVariable Long sessionId,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        sessionService.closeSession(sessionId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        return "redirect:/login";
    }
}