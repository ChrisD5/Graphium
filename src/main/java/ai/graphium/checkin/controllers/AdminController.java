package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.forms.CreateEmployeeForm;
import ai.graphium.checkin.forms.CreateSupervisorForm;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.List;

@Controller
@RequestMapping("/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("")
    public String adminHomeController() {
        return "admin/index";
    }

    @GetMapping("/e")
    public String adminEmployeesController(Model model) {
        if (model.getAttribute("employee") == null) {
            model.addAttribute("employee", new CreateEmployeeForm());
        }
        Collection<User> employees = userRepository.findFirst10ByEmployeeAndSupervisorIsFalse(true);
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        model.addAttribute("employees", employees);
        return "admin/manage-employees";
    }

    @PostMapping("/e/create")
    public String adminCreateEmployeeSubmit(RedirectAttributes redirectAttributes, @ModelAttribute CreateEmployeeForm employee) {
        boolean emailExists = userRepository.existsByEmail(employee.getEmail());
        if (emailExists) {
            redirectAttributes.addFlashAttribute("status", "error");
            redirectAttributes.addFlashAttribute("message", "Email Address Already Exists");
            redirectAttributes.addFlashAttribute("employee", employee);
            return "redirect:/admin/e";
        }
        boolean teamExists = teamRepository.existsById(employee.getTeam_id());
        if (!teamExists) {
            redirectAttributes.addFlashAttribute("status", "error");
            redirectAttributes.addFlashAttribute("message", "This team does not exist!");
            redirectAttributes.addFlashAttribute("employee", employee);
            return "redirect:/admin/e";
        }
        User newEmployee = new User(employee.getEmail(), passwordEncoder.encode("employee"), UserType.EMPLOYEE, employee.getName(), employee.getPhone());
        Team team = teamRepository.findById(employee.getTeam_id());
        newEmployee.setTeam(team);
        userRepository.save(newEmployee);
        redirectAttributes.addFlashAttribute("status", "success");
        redirectAttributes.addFlashAttribute("message", String.format("Added %s as a new employee", newEmployee.getName()));
        return "redirect:/admin/e";
    }

    @GetMapping("/s")
    public String adminSupervisorsController(Model model) {
        if (model.getAttribute("supervisor") == null) {
            model.addAttribute("supervisor", new CreateSupervisorForm());
        }
        Collection<User> supervisors = userRepository.findFirst10BySupervisorIsTrue();
        model.addAttribute("supervisors", supervisors);
//        List<Team> teams = teamRepository.findBySupervisorExistsIsFalse();
//        model.addAttribute("teams", teams);
        return "admin/manage-supervisors";
    }

    @PostMapping("/s/create")
    public String adminCreateSupervisorSubmit(RedirectAttributes redirectAttributes, @ModelAttribute CreateSupervisorForm supervisor) {
        boolean emailExists = userRepository.existsByEmail(supervisor.getEmail());
        if (emailExists) {
            redirectAttributes.addFlashAttribute("status", "error");
            redirectAttributes.addFlashAttribute("message", "Email Address Already Exists");
            redirectAttributes.addFlashAttribute("supervisor", supervisor);
            return "redirect:/admin/s";
        }
        User newSupervisor = new User(supervisor.getEmail(), passwordEncoder.encode("supervisor"), UserType.SUPERVISOR, supervisor.getName(), supervisor.getPhone());
        newSupervisor.setEmployee(true);
        userRepository.save(newSupervisor);
        redirectAttributes.addFlashAttribute("status", "success");
        redirectAttributes.addFlashAttribute("message", String.format("Added %s as a new supervisor. To assign them to a team go to teams page", newSupervisor.getName()));
        return "redirect:/admin/s";
    }

    @GetMapping("/team")
    public String adminTeamsController() {
        return "admin/manage-teams";
    }
}
