package com.elzakaria.copiweb.web;

import com.elzakaria.copiweb.dto.CreateSessionRequest;
import com.elzakaria.copiweb.model.SessionStatus;
import com.elzakaria.copiweb.repository.AgentEventRepository;
import com.elzakaria.copiweb.repository.AgentSessionRepository;
import com.elzakaria.copiweb.service.AgentSessionService;
import com.elzakaria.copiweb.service.ModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final AgentSessionService sessionService;
    private final AgentSessionRepository sessionRepo;
    private final AgentEventRepository eventRepo;
    private final ModelService modelService;

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long activeSessions = sessionRepo.countByStatusIn(List.of(SessionStatus.ACTIVE, SessionStatus.IDLE));
        long totalSessions = sessionRepo.count();
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long eventsToday = eventRepo.countByOccurredAtAfter(todayStart);
        long errorsToday = sessionRepo.countByStatus(SessionStatus.ERROR);
        var recentActivity = eventRepo.findRecentEventsAcrossAllSessions(LocalDateTime.now().minusHours(24));
        var recentSessions = sessionRepo.findTop10ByOrderByLastActiveAtDesc();

        model.addAttribute("activeSessions", activeSessions);
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("eventsToday", eventsToday);
        model.addAttribute("errorsToday", errorsToday);
        model.addAttribute("recentActivity", recentActivity);
        model.addAttribute("recentSessions", recentSessions);
        return "dashboard";
    }

    @GetMapping("/sessions")
    public String listSessions(Model model) {
        model.addAttribute("sessions", sessionService.listSessions());
        return "sessions/list";
    }

    @GetMapping("/sessions/new")
    public String newSession(Model model) {
        model.addAttribute("models", modelService.getModels());
        model.addAttribute("createRequest", new CreateSessionRequest("", "gpt-4.1", "", "", true));
        return "sessions/new";
    }

    @PostMapping("/sessions")
    public String createSession(@Valid @ModelAttribute("createRequest") CreateSessionRequest req,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("models", modelService.getModels());
            return "sessions/new";
        }
        try {
            var session = sessionService.createSession(req);
            redirectAttrs.addFlashAttribute("success", "Session '" + session.getName() + "' created successfully.");
            return "redirect:/sessions/" + session.getId();
        } catch (Exception e) {
            log.error("Failed to create session", e);
            model.addAttribute("models", modelService.getModels());
            model.addAttribute("error", "Failed to create session: " + e.getMessage());
            return "sessions/new";
        }
    }

    @GetMapping("/sessions/{id}")
    public String sessionDetail(@PathVariable Long id, Model model) {
        var agentSession = sessionService.getSession(id);
        var recentEvents = sessionService.getRecentEvents(id, 50);
        model.addAttribute("agentSession", agentSession);
        model.addAttribute("events", recentEvents);
        return "sessions/detail";
    }

    @GetMapping("/sessions/{id}/history")
    public String sessionHistory(@PathVariable Long id, Model model) {
        var agentSession = sessionService.getSession(id);
        var allEvents = sessionService.getHistory(id);
        model.addAttribute("agentSession", agentSession);
        model.addAttribute("events", allEvents);
        return "sessions/history";
    }

    @PostMapping("/sessions/{id}/resume")
    public String resumeSession(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            sessionService.resumeSession(id);
            redirectAttrs.addFlashAttribute("success", "Session resumed.");
        } catch (Exception e) {
            log.error("Failed to resume session {}", id, e);
            redirectAttrs.addFlashAttribute("error", "Failed to resume: " + e.getMessage());
        }
        return "redirect:/sessions/" + id;
    }

    @PostMapping("/sessions/{id}/delete")
    public String deleteSession(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            sessionService.deleteSession(id);
            redirectAttrs.addFlashAttribute("success", "Session deleted.");
        } catch (Exception e) {
            log.error("Failed to delete session {}", id, e);
            redirectAttrs.addFlashAttribute("error", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/sessions";
    }
}
