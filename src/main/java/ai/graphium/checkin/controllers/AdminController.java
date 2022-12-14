package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.entity.joins.EmployeeJoinCheckIn;
import ai.graphium.checkin.entity.joins.SupervisorJoinTeam;
import ai.graphium.checkin.entity.joins.TeamJoinCheckIn;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.forms.*;
import ai.graphium.checkin.repos.CheckInRepository;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import java.util.*;

@AllArgsConstructor
@Controller
@RequestMapping("/admin")
@Secured("ROLE_ADMIN")
public class AdminController {

    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private CheckInRepository checkInRepository;
    private PasswordEncoder passwordEncoder;

    private EntityManager entityManager;

    @GetMapping("")
    public String adminHomeController(Model model) {
        model.addAttribute("employeesCount", userRepository.countAllByEmployeeIsTrueAndSupervisorIsFalse());
        model.addAttribute("supervisorsCount", userRepository.countAllBySupervisorIsTrue());
        Double avgrating = entityManager.createQuery("SELECT AVG(e.rating) FROM CheckIn e", Double.class).getSingleResult();
        model.addAttribute("avgrating", Math.round(avgrating * 100.0) / 100.0);
        List<User> supervisors = userRepository.findAllBySupervisor(true);
        List<SupervisorJoinTeam> supervisorsList = new ArrayList<>();
        for (User supervisor : supervisors) {
            Team team = teamRepository.findBySupervisor(supervisor);
            SupervisorJoinTeam supervisorL = new SupervisorJoinTeam(supervisor.getId(), supervisor.getName(), supervisor.isDisabled());
            if (team != null) {
                var cb = entityManager.getCriteriaBuilder();
                var cq = cb.createQuery(Double.class);
                var root = cq.from(User.class);
                var checkinsroot = root.join("checkIns", JoinType.LEFT);
                cq.select(cb.avg(checkinsroot.get("rating")));
                cq.where(cb.and(
                        cb.isTrue(root.get("employee")),
                        cb.equal(root.get("team"), team.getId())));
                cq.distinct(true);
                var rating = entityManager.createQuery(cq).getSingleResult();
                if (rating == null) rating = Double.valueOf(0);
                supervisorL.setAvgrating(Math.round(rating * 100.0) / 100.0);
                supervisorL.setTeamname(team.getName());
            } else {
                supervisorL.setTeamname("N/A");
            }
            supervisorsList.add(supervisorL);
        }
        supervisorsList.sort(Comparator.comparing(SupervisorJoinTeam::getAvgrating));
        supervisorsList.sort(Comparator.comparing(SupervisorJoinTeam::getName));
        model.addAttribute("supervisors", supervisorsList);
        return "admin/index";
    }

    @GetMapping("/e")
    public String adminEmployeesController(Model model) {
        if (model.getAttribute("employee") == null) {
            model.addAttribute("employee", new CreateEmployeeForm());
        }
        if (model.getAttribute("promoteemployeetosupervisor") == null) {
            model.addAttribute("promoteemployeetosupervisor", new PromoteEmployeeToSupervisorForm());
        }
        List<User> employees = userRepository.findByEmployeeAndSupervisorIsFalse(true);
        List<EmployeeJoinCheckIn> employeesList = new ArrayList<>();
        for (User emp : employees) {
            Team team = emp.getTeam();
            EmployeeJoinCheckIn employeeJoinCheckIn = new EmployeeJoinCheckIn(emp.getId(), emp.getName(), emp.isDisabled());
            if (team != null) {
                var cb = entityManager.getCriteriaBuilder();
                var cq = cb.createQuery(Double.class);
                var root = cq.from(User.class);
                var checkinsroot = root.join("checkIns", JoinType.LEFT);
                cq.select(cb.avg(checkinsroot.get("rating")));
                cq.where(cb.and(
                        cb.isTrue(root.get("employee")),
                        cb.equal(root.get("id"), emp.getId())));
                cq.distinct(true);
                var rating = entityManager.createQuery(cq).getSingleResult();
                if (rating == null) rating = Double.valueOf(0);
                employeeJoinCheckIn.setAvgrating(Math.round(rating * 100.0) / 100.0);
                employeeJoinCheckIn.setTeamname(team.getName());
            } else {
                employeeJoinCheckIn.setTeamname("N/A");
            }
            employeesList.add(employeeJoinCheckIn);
        }
        employeesList.sort(Comparator.comparing(EmployeeJoinCheckIn::getAvgrating));
        employeesList.sort(Comparator.comparing(EmployeeJoinCheckIn::getName));
        model.addAttribute("employees", employeesList);
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
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

    @PostMapping("/e/disable/{id}")
    public String adminDisableEmployee(RedirectAttributes redirectAttributes, @PathVariable long id) {
        User employee = userRepository.findById(id);
        if (employee == null || !employee.isEmployee()) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "This employee does not exist");
            return "redirect:/admin/e";
        }
        employee.setDisabled(true);
        userRepository.save(employee);
        redirectAttributes.addFlashAttribute("delstatus", "success");
        redirectAttributes.addFlashAttribute("delmessage", String.format("The employee %s has been disabled", employee.getName()));
        return "redirect:/admin/e";
    }

    @PostMapping("/e/enable/{id}")
    public String adminEnableEmployee(RedirectAttributes redirectAttributes, @PathVariable long id) {
        User employee = userRepository.findById(id);
        if (employee == null || !employee.isEmployee()) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "This employee does not exist");
            return "redirect:/admin/e";
        }
        employee.setDisabled(false);
        userRepository.save(employee);
        redirectAttributes.addFlashAttribute("delstatus", "success");
        redirectAttributes.addFlashAttribute("delmessage", String.format("The employee %s has been re-enabled", employee.getName()));
        return "redirect:/admin/e";
    }

    @PostMapping("/e/promote")
    public String adminPromoteEmployee(RedirectAttributes redirectAttributes, @ModelAttribute PromoteEmployeeToSupervisorForm promoteEmployeeToSupervisorForm) {
        boolean employeeExists = userRepository.existsById(promoteEmployeeToSupervisorForm.getEmployee_id());
        if (!employeeExists) {
            redirectAttributes.addFlashAttribute("promstatus", "error");
            redirectAttributes.addFlashAttribute("prommessage", "This employee does not exist");
            return "redirect:/admin/e";
        }

        User employee = userRepository.findById(promoteEmployeeToSupervisorForm.getEmployee_id());
        employee.setSupervisor(true);
        userRepository.save(employee);

        redirectAttributes.addFlashAttribute("promstatus", "success");
        redirectAttributes.addFlashAttribute("prommessage", String.format("The employee %s has been promoted to supervisor", employee.getName()));
        return "redirect:/admin/e";
    }

    @GetMapping("/s")
    public String adminSupervisorsController(Model model) {
        if (model.getAttribute("supervisor") == null) {
            model.addAttribute("supervisor", new CreateSupervisorForm());
        }
        if (model.getAttribute("demotesupervisortoemployee") == null) {
            model.addAttribute("demotesupervisortoemployee", new DemoteSupervisorToEmployeeForm());
        }
        Collection<SupervisorJoinTeam> supervisorJoinTeams = new ArrayList<>();
        Collection<User> supervisors = userRepository.findBySupervisorIsTrue();
        for (User u : supervisors) {
            SupervisorJoinTeam supervisorJoinTeam = new SupervisorJoinTeam(u.getId(), u.getName(), u.isDisabled());
            Team findTeam = teamRepository.findBySupervisor(u);
            if (findTeam != null) {
                var cb = entityManager.getCriteriaBuilder();
                var cq = cb.createQuery(Double.class);
                var root = cq.from(User.class);
                var checkinsroot = root.join("checkIns", JoinType.LEFT);
                cq.select(cb.avg(checkinsroot.get("rating")));
                cq.where(cb.and(
                        cb.isTrue(root.get("employee")),
                        cb.equal(root.get("team"), findTeam.getId())));
                cq.distinct(true);
                var rating = entityManager.createQuery(cq).getSingleResult();
                if (rating == null) rating = Double.valueOf(0);
                supervisorJoinTeam.setAvgrating(Math.round(rating * 100.0) / 100.0);
                supervisorJoinTeam.setTeamname(findTeam.getName());
            } else {
                supervisorJoinTeam.setTeamname("N/A");
            }
            supervisorJoinTeams.add(supervisorJoinTeam);
        }
        model.addAttribute("supervisors", supervisorJoinTeams);

        return "admin/manage-supervisors";
    }

    @PostMapping("/e/demote")
    public String adminDemoteSupervisor(RedirectAttributes redirectAttributes, @ModelAttribute DemoteSupervisorToEmployeeForm demoteSupervisorToEmployeeForm) {
        boolean supervisorExists = userRepository.existsById(demoteSupervisorToEmployeeForm.getSupervisor_id());
        if (!supervisorExists) {
            redirectAttributes.addFlashAttribute("demstatus", "error");
            redirectAttributes.addFlashAttribute("demmessage", "This employee does not exist");
            return "redirect:/admin/s";
        }

        User supervisor = userRepository.findById(demoteSupervisorToEmployeeForm.getSupervisor_id());
        Team findTeam = teamRepository.findBySupervisor(supervisor);
        if (findTeam != null) {
            redirectAttributes.addFlashAttribute("demstatus", "error");
            redirectAttributes.addFlashAttribute("demmessage", "This supervisor is still assigned to a team");
            return "redirect:/admin/s";
        }
        supervisor.setSupervisor(false);
        userRepository.save(supervisor);

        redirectAttributes.addFlashAttribute("demstatus", "success");
        redirectAttributes.addFlashAttribute("demmessage", String.format("The supervisor %s has been demoted to employee", supervisor.getName()));
        return "redirect:/admin/s";
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

    @PostMapping("/s/disable/{id}")
    public String adminDisableSupervisor(RedirectAttributes redirectAttributes, @PathVariable long id) {
        User supervisor = userRepository.findById(id);
        if (supervisor == null || !supervisor.isSupervisor()) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "This supervisor does not exist");
            return "redirect:/admin/s";
        }
        supervisor.setDisabled(true);
        userRepository.save(supervisor);
        redirectAttributes.addFlashAttribute("delstatus", "success");
        redirectAttributes.addFlashAttribute("delmessage", String.format("The supervisor %s has been disabled", supervisor.getName()));
        return "redirect:/admin/s";
    }

    @PostMapping("/s/enable/{id}")
    public String adminEnableSupervisor(RedirectAttributes redirectAttributes, @PathVariable long id) {
        User supervisor = userRepository.findById(id);
        if (supervisor == null || !supervisor.isSupervisor()) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "This supervisor does not exist");
            return "redirect:/admin/s";
        }
        supervisor.setDisabled(false);
        userRepository.save(supervisor);
        redirectAttributes.addFlashAttribute("delstatus", "success");
        redirectAttributes.addFlashAttribute("delmessage", String.format("The supervisor %s has been re-enabled", supervisor.getName()));
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
        if (model.getAttribute("assignemployeetoteam") == null) {
            model.addAttribute("assignemployeetoteam", new AssignEmployeeToTeamForm());
        }
        Collection<SupervisorJoinTeam> unassignedSupervisors = new ArrayList<>();
        Collection<User> supervisors = userRepository.findBySupervisorIsTrue();
        for (User u : supervisors) {
            Team findTeam = teamRepository.findBySupervisor(u);
            if (findTeam == null) {
                unassignedSupervisors.add(new SupervisorJoinTeam(u.getId(), u.getName(), u.isDisabled()));
            }
        }
        List<Team> teams = teamRepository.findAll();
        List<TeamJoinCheckIn> teamsList = new ArrayList<>();
        for (Team team : teams) {
            User supervisor = team.getSupervisor();
            TeamJoinCheckIn teamL = new TeamJoinCheckIn(team.getId(), supervisor != null ? supervisor.getName() : "N/A", team.getName());
            var cb = entityManager.getCriteriaBuilder();
            var cq = cb.createQuery(Double.class);
            var root = cq.from(User.class);
            var checkinsroot = root.join("checkIns", JoinType.LEFT);
            cq.select(cb.avg(checkinsroot.get("rating")));
            cq.where(cb.and(
                    cb.isTrue(root.get("employee")),
                    cb.equal(root.get("team"), team.getId())));
            cq.distinct(true);
            var rating = entityManager.createQuery(cq).getSingleResult();
            if (rating == null) rating = Double.valueOf(0);
            teamL.setAvgrating(Math.round(rating * 100.0) / 100.0);
            teamsList.add(teamL);
        }
        teamsList.sort(Comparator.comparing(TeamJoinCheckIn::getAvgrating));
        teamsList.sort(Comparator.comparing(TeamJoinCheckIn::getName));
        model.addAttribute("teams", teamsList);
        List<User> employees = userRepository.findAllByEmployeeAndSupervisorAndAdmin(true, false, false);
        model.addAttribute("employees", employees);
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

    @PostMapping("/team/assign/supervisor")
    public String adminAssignSupervisorToTeamSubmit(RedirectAttributes redirectAttributes, @ModelAttribute AssignSupervisorToTeamForm assignSupervisorToTeamForm) {
        boolean supervisorExists = userRepository.existsById(assignSupervisorToTeamForm.getSupervisor_id());
        if (!supervisorExists) {
            redirectAttributes.addFlashAttribute("s_assignstatus", "error");
            redirectAttributes.addFlashAttribute("s_assignmessage", "This supervisor does not exist");
            redirectAttributes.addFlashAttribute("assignsupervisortoteam", assignSupervisorToTeamForm);
            return "redirect:/admin/team#assignsup";
        }
        boolean teamExists = teamRepository.existsById(assignSupervisorToTeamForm.getTeam_id());
        if (!teamExists) {
            redirectAttributes.addFlashAttribute("s_assignstatus", "error");
            redirectAttributes.addFlashAttribute("s_assignmessage", "This team does not exist");
            redirectAttributes.addFlashAttribute("assignsupervisortoteam", assignSupervisorToTeamForm);
            return "redirect:/admin/team#assignsup";
        }
        boolean hasTeam = teamRepository.existsBySupervisorId(assignSupervisorToTeamForm.getSupervisor_id());
        if (hasTeam) {
            redirectAttributes.addFlashAttribute("s_assignstatus", "error");
            redirectAttributes.addFlashAttribute("s_assignmessage", "This supervisor is already assigned to a team");
            redirectAttributes.addFlashAttribute("assignsupervisortoteam", assignSupervisorToTeamForm);
            return "redirect:/admin/team#assignsup";
        }
        Team team = teamRepository.findById(assignSupervisorToTeamForm.getTeam_id());
        User supervisor = userRepository.findById(assignSupervisorToTeamForm.getSupervisor_id());
        boolean hadSupervisor = team.getSupervisor() != null;
        team.setSupervisor(supervisor);
        teamRepository.save(team);
        redirectAttributes.addFlashAttribute("s_assignstatus", "success");
        redirectAttributes.addFlashAttribute("s_assignmessage", String.format("%s supervisor of %s to %s", hadSupervisor ? "Re-assigned" : "Assigned", team.getName(), supervisor.getName()));
        return "redirect:/admin/team#assignsup";
    }

    @PostMapping("/team/assign/employee")
    public String adminAssignEmployeeToTeamSubmit(RedirectAttributes redirectAttributes, @ModelAttribute AssignEmployeeToTeamForm assignEmployeeToTeamForm) {
        boolean employeeExists = userRepository.existsById(assignEmployeeToTeamForm.getEmployee_id());
        if (!employeeExists) {
            redirectAttributes.addFlashAttribute("e_assignstatus", "error");
            redirectAttributes.addFlashAttribute("e_assignmessage", "This employee does not exist");
            redirectAttributes.addFlashAttribute("assignemployeetoteam", assignEmployeeToTeamForm);
            return "redirect:/admin/team#assignemp";
        }
        boolean teamExists = teamRepository.existsById(assignEmployeeToTeamForm.getTeam_id());
        if (!teamExists) {
            redirectAttributes.addFlashAttribute("e_assignstatus", "error");
            redirectAttributes.addFlashAttribute("e_assignmessage", "This team does not exist");
            redirectAttributes.addFlashAttribute("assignemployeetoteam", assignEmployeeToTeamForm);
            return "redirect:/admin/team#assignemp";
        }
        boolean hasTeam = teamRepository.existsBySupervisorId(assignEmployeeToTeamForm.getEmployee_id());
        if (hasTeam) {
            redirectAttributes.addFlashAttribute("e_assignstatus", "error");
            redirectAttributes.addFlashAttribute("e_assignmessage", "This employee is already assigned to a team");
            redirectAttributes.addFlashAttribute("assignemployeetoteam", assignEmployeeToTeamForm);
            return "redirect:/admin/team#assignemp";
        }
        Team team = teamRepository.findById(assignEmployeeToTeamForm.getTeam_id());
        User employee = userRepository.findById(assignEmployeeToTeamForm.getEmployee_id());
        boolean hadTeam = employee.getTeam() != null;
        employee.setTeam(team);
        userRepository.save(employee);
        redirectAttributes.addFlashAttribute("e_assignstatus", "success");
        redirectAttributes.addFlashAttribute("e_assignmessage", String.format("%s employee of %s to %s", hadTeam ? "Re-assigned" : "Assigned", team.getName(), employee.getName()));
        return "redirect:/admin/team#assignemp";
    }

    @PostMapping("/team/delete/{id}")
    public String adminDeleteTeam(RedirectAttributes redirectAttributes, @PathVariable long id) {
        Team team = teamRepository.findById(id);
        if (team == null) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "This team does not exist");
            return "redirect:/admin/team#teamslist";
        }
        List<User> users = userRepository.findAllByTeamId(id);
        if (users.size() > 0) {
            redirectAttributes.addFlashAttribute("delstatus", "error");
            redirectAttributes.addFlashAttribute("delmessage", "Re-Assign all employees to another team to delete this team");
            return "redirect:/admin/team#teamslist";
        }
        teamRepository.delete(team);
        redirectAttributes.addFlashAttribute("delstatus", "success");
        redirectAttributes.addFlashAttribute("delmessage", String.format("Deleted %s", team.getName()));
        return "redirect:/admin/team#teamslist";
    }
}
