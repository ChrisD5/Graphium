package ai.graphium.checkin.controllers;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/a")
@Secured("ROLE_ADMIN")
public class AdminController {

    @GetMapping("")
    public String adminHomeController() {
        return "admin/index";
    }

    @GetMapping("/e")
    public String adminEmployeesController() {
        return "admin/manage-employees";
    }

    @GetMapping("/s")
    public String adminSupervisorsController() {
        return "admin/manage-supervisors";
    }

    @GetMapping("/team")
    public String adminTeamsController() {
        return "admin/manage-teams";
    }
}
