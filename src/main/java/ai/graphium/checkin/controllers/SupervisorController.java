package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Controller
@RequestMapping("/s")
@Secured("ROLE_SUPERVISOR")
public class SupervisorController {

    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private EntityManager em;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("")
    public String supervisorHomeController(Model model, Authentication authentication) throws JsonProcessingException {
        Map<String, List<CheckIn>> map = new TreeMap<>();
        Team team = teamRepository.findBySupervisorEmail(authentication.getName());
        if (team == null) {
            User supervisor = userRepository.findByEmail(authentication.getName());
            model.addAttribute("supervisor", supervisor);
            model.addAttribute("employees", map);
            return "supervisor/index";
        }
        List<User> employees = userRepository.findAllByTeamId(team.getId());
        for (User emp : employees) {
            var cb = em.getCriteriaBuilder();
            var cq = cb.createQuery(CheckIn.class);
            var root = cq.from(CheckIn.class);
            cq.select(root);
            {
                var sq = cq.subquery(Long.class);
                var userRoot = sq.from(User.class);
                var checkInRoot = userRoot.join("checkIns");
                sq.select(checkInRoot.get("id"));
                sq.where(cb.equal(userRoot.get("id"), emp.getId()));

                cq.where(cb.and(
                        root.get("id").in(sq),
                        cb.gt(root.get("time"), System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))
                ));
            }
            cq.orderBy(cb.desc(root.get("time")));
            map.put(emp.getName(), em.createQuery(cq).getResultList());
        }
        model.addAttribute("supervisor", team.getSupervisor());
        model.addAttribute("employees", objectMapper.writeValueAsString(map));
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
        model.addAttribute("checkins",
                user.getCheckIns()
                        .stream()
                        .sorted(Comparator.comparingLong(CheckIn::getTime).reversed())
                        .toList()
        );
        model.addAttribute("image", user.getImage() != null ? Base64.getEncoder().encodeToString(user.getImage()) : null);
        return "supervisor/profile";

    }
}