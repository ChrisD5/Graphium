package ai.graphium.checkin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/s")
public class SupervisorController {

    @GetMapping("")
    public String supervisorHomeController() {
        return "supervisor/index";
    }

    @GetMapping("/team/{teamName}")
    public String supervisorTeamController(@PathVariable("teamName") String teamName) {
        // Ensure appropriate security checks are done here to only allow assigned supervisor access
        return "supervisor/team";
    }

    @GetMapping("/settings")
    public String supervisorSettingsController() {
        return "supervisor/settings";
    }
}
