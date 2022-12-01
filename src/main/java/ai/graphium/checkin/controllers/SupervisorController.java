package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/s")
@Secured("ROLE_SUPERVISOR")
public class SupervisorController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

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

    @GetMapping("/employees")
    public String supervisorEmployeesController(Model model, Authentication authentication) {
        Team team = teamRepository.findBySupervisorEmail(authentication.getName());
        Collection<User> employees = userRepository.findByTeamIdAndSupervisorIsFalse(team.getId());
        System.out.println(employees.size());
        model.addAttribute("employees", employees);
        return "supervisor/employees";
    }

    @GetMapping("/profile/{id}")
    public String supervisorProfileController(Model model, @PathVariable long id, Authentication authentication) {
        User user = userRepository.findById(id);
        if (user == null || user.getTeam() == null) {
            return "redirect:/s/employees";
        }
        Team team = teamRepository.findBySupervisorEmail(authentication.getName());
        if (user.getTeam().getId() != team.getId()) {
            return "redirect:/s/employees";
        }
        model.addAttribute("user", user);
        return "supervisor/profile";

    }
}