package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.entity.joins.SupervisorJoinTeam;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.forms.AssignSupervisorToTeamForm;
import ai.graphium.checkin.forms.CreateEmployeeForm;
import ai.graphium.checkin.forms.CreateSupervisorForm;
import ai.graphium.checkin.forms.CreateTeamForm;
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

import java.util.ArrayList;
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
        Collection<User> employees = userRepository.findByEmployeeAndSupervisorIsFalse(true);
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
        Collection<SupervisorJoinTeam> supervisorJoinTeams = new ArrayList<>();
        Collection<User> supervisors = userRepository.findBySupervisorIsTrue();
        for (User u : supervisors) {
            SupervisorJoinTeam supervisorJoinTeam = new SupervisorJoinTeam(u.getId(), u.getName());
            Team findTeam = teamRepository.findBySupervisor(u);
            if (findTeam != null) {
                supervisorJoinTeam.setTeamname(findTeam.getName());
            } else {
                supervisorJoinTeam.setTeamname("N/A");
            }
            supervisorJoinTeams.add(supervisorJoinTeam);
        }
        model.addAttribute("supervisors", supervisorJoinTeams);
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
    public String adminTeamsController(Model model) {
        if (model.getAttribute("team") == null) {
            model.addAttribute("team", new CreateTeamForm());
        }
        if (model.getAttribute("assignsupervisortoteam") == null) {
            model.addAttribute("assignsupervisortoteam", new AssignSupervisorToTeamForm());
        }
        Collection<SupervisorJoinTeam> unassignedSupervisors = new ArrayList<>();
        Collection<User> supervisors = userRepository.findBySupervisorIsTrue();
        for (User u : supervisors) {
            Team findTeam = teamRepository.findBySupervisor(u);
            if (findTeam == null) {
                unassignedSupervisors.add(new SupervisorJoinTeam(u.getId(), u.getName()));
            }
        }
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        model.addAttribute("unassigned_supervisors", unassignedSupervisors);
        return "admin/manage-teams";
    }

    @PostMapping("/team/create")
    public String adminCreateTeamSubmit(RedirectAttributes redirectAttributes, @ModelAttribute CreateTeamForm team) {
        boolean nameExists = teamRepository.existsByName(team.getName());
        if (nameExists) {
            redirectAttributes.addFlashAttribute("createstatus", "error");
            redirectAttributes.addFlashAttribute("createmessage", "Team Name Already Exists");
            redirectAttributes.addFlashAttribute("team", team);
            return "redirect:/admin/team";
        }
        Team newTeam = new Team(team.getName());
        teamRepository.save(newTeam);
        redirectAttributes.addFlashAttribute("createstatus", "success");
        redirectAttributes.addFlashAttribute("createmessage", String.format("Added %s as a new team", team.getName()));
        return "redirect:/admin/team";
    }

    @PostMapping("/team/assign")
    public String adminAssignSupervisorToTeamSubmit(RedirectAttributes redirectAttributes, @ModelAttribute AssignSupervisorToTeamForm assignSupervisorToTeamForm) {
        boolean supervisorExists = userRepository.existsById(assignSupervisorToTeamForm.getSupervisor_id());
        if (!supervisorExists) {
            redirectAttributes.addFlashAttribute("assignstatus", "error");
            redirectAttributes.addFlashAttribute("assignmessage", "This supervisor does not exist");
            redirectAttributes.addFlashAttribute("assignsupervisortoteam", assignSupervisorToTeamForm);
            return "redirect:/admin/team";
        }
        boolean hasTeam = teamRepository.existsBySupervisorId(assignSupervisorToTeamForm.getSupervisor_id());
        if (hasTeam) {
            redirectAttributes.addFlashAttribute("assignstatus", "error");
            redirectAttributes.addFlashAttribute("assignmessage", "This supervisor is already assigned to a team");
            redirectAttributes.addFlashAttribute("assignsupervisortoteam", assignSupervisorToTeamForm);
            return "redirect:/admin/team";
        }
        Team team = teamRepository.findById(assignSupervisorToTeamForm.getTeam_id());
        User supervisor = userRepository.findById(assignSupervisorToTeamForm.getSupervisor_id());
        boolean hadSupervisor = team.getSupervisor() != null;
        team.setSupervisor(supervisor);
        teamRepository.save(team);
        redirectAttributes.addFlashAttribute("assignstatus", "success");
        redirectAttributes.addFlashAttribute("assignmessage", String.format("%s supervisor of %s to %s", hadSupervisor ? "Re-assigned" : "Assigned", team.getName(), supervisor.getName()));
        return "redirect:/admin/team";
    }
}
