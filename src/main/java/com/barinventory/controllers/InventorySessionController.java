package com.barinventory.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.barinventory.entities.InventorySession;
import com.barinventory.services.InventorySessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions")
public class InventorySessionController {

    private final InventorySessionService sessionService;

    /*
     -----------------------------------------
     STEP 1 -> OPEN CREATE SESSION PAGE
     URL:
     GET /sessions/create-page
     -----------------------------------------
    */
    @GetMapping("/create-page")
    public String createSessionPage() {
        return "session/create-session";
    }

    /*
     -----------------------------------------
     STEP 2 -> CREATE SESSION
     POST /sessions/create
     -----------------------------------------
    */
    @PostMapping("/create")
    public String createSession(
            @RequestParam String sessionName
    ) {

        InventorySession session =
                sessionService.createSession(sessionName);

        return "redirect:/sessions/dashboard/" +
                session.getSessionId();
    }

    /*
     -----------------------------------------
     STEP 3 -> DASHBOARD PAGE
     GET /sessions/dashboard/{sessionId}
     -----------------------------------------
    */
    @GetMapping("/dashboard/{sessionId}")
    public String sessionDashboard(
            @PathVariable Long sessionId,
            Model model
    ) {
        model.addAttribute("sessionId", sessionId);

        return "session/session-dashboard";
    }

    /*
     -----------------------------------------
     FINAL STEP -> SESSION SUMMARY PAGE
     GET /sessions/summary/{sessionId}
     -----------------------------------------
    */
    @GetMapping("/summary/{sessionId}")
    public String sessionSummary(
            @PathVariable Long sessionId,
            Model model
    ) {
        model.addAttribute("sessionId", sessionId);

        // later fetch actual totals
        model.addAttribute("stockroomSales", 5000);
        model.addAttribute("wellSales", 4500);

        return "reports/session-summary";
    }

    /*
     -----------------------------------------
     FINAL CLOSE SESSION
     POST /sessions/close/{sessionId}
     -----------------------------------------
    */
    @PostMapping("/close/{sessionId}")
    public String closeSession(
            @PathVariable Long sessionId
    ) {

        sessionService.closeSession(sessionId);

        return "redirect:/sessions/create-page";
    }
}