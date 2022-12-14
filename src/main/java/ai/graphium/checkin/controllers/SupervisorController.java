package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.*;
import ai.graphium.checkin.entity.joins.EmployeeJoinCheckIn;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.forms.SetSupervisorAlertThresholdForm;
import ai.graphium.checkin.repos.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.component.VEvent;
import org.springframework.lang.Nullable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Controller
@RequestMapping("/s")
@Secured("ROLE_SUPERVISOR")
public class SupervisorController {

    private final AlertRepository alertRepository;
    private final MeetingRepository meetingRepository;
    private final NoteRepository noteRepository;
    private UserRepository userRepository;
    private TeamRepository teamRepository;
    private EntityManager em;
    private ObjectMapper objectMapper;
    private EntityManager entityManager;

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

                long time = System.currentTimeMillis();
                time -= time % TimeUnit.DAYS.toMillis(1);
                time -= TimeUnit.DAYS.toMillis(7);

                cq.where(cb.and(
                        root.get("id").in(sq),
                        cb.gt(root.get("time"), time)
                ));
            }
            cq.orderBy(cb.desc(root.get("time")));
            map.put(emp.getName(), em.createQuery(cq).getResultList());
        }
        List<Alert> alerts;
        {
            var cb = em.getCriteriaBuilder();
            var cq = cb.createQuery(Alert.class);
            var root = cq.from(Alert.class);
            cq.select(root);
            var filterVisibility = cb.or(cb.equal(root.get("visibility"), AlertVisibility.ALL), cb.equal(root.get("visibility"), AlertVisibility.SUPERVISOR));
            cq.where(cb.and(cb.equal(root.get("supervisor"), team.getSupervisor())), filterVisibility);
            cq.orderBy(cb.desc(root.get("created")));
            alerts = em.createQuery(cq).getResultList();
        }
        boolean noUnreadAlerts = alerts.stream().allMatch(Alert::isReadBySupervisor);
        int unreadCount = (int) alerts.stream().filter(alert -> !alert.isReadBySupervisor()).count();

        model.addAttribute("supervisor", team.getSupervisor());
        model.addAttribute("employees", objectMapper.writeValueAsString(map));
        model.addAttribute("noUnreadAlerts", noUnreadAlerts);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("alerts", alerts);
        return "supervisor/index";
    }

    @PostMapping("alert/read")
    public String readAlert(@RequestParam("id") long id, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());

        var alertOptional = alertRepository.findById(id);

        if (alertOptional.isEmpty()) {
            return "redirect:/s";
        }

        var alert = alertOptional.get();

        if (alert.getSupervisor().getId() != user.getId()) {
            return "redirect:/s";
        }

        alert.setReadBySupervisor(true);
        alertRepository.save(alert);

        return "redirect:/s";
    }

    @GetMapping("meetings")
    public String meetings(Model model, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetings = meetingRepository.findByRequesteeOrderByTimeDesc(user);

        model.addAttribute("meetings", meetings);

        return "supervisor/meetings";
    }

    @PostMapping("meetings/accept")
    public String acceptMeeting(@RequestParam("id") long id, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetingOptional = meetingRepository.findById(id);

        if (meetingOptional.isEmpty()) {
            return "redirect:/s/meetings";
        }

        var meeting = meetingOptional.get();

        if (meeting.getRequestee().getId() != user.getId()) {
            return "redirect:/s/meetings";
        }

        meeting.setConfirmed(true);
        meetingRepository.save(meeting);

        return "redirect:/s/meetings";
    }

    @PostMapping("meetings/reschedule")
    public String rescheduleMeeting(@RequestParam("id") long id, @RequestParam("time") String timeStr, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetingOptional = meetingRepository.findById(id);

        if (meetingOptional.isEmpty()) {
            return "redirect:/s/meetings";
        }

        var meeting = meetingOptional.get();

        if (meeting.getRequestee().getId() != user.getId()) {
            return "redirect:/s/meetings";
        }

        if (timeStr.isBlank()) {
            return "redirect:/s/meetings";
        }

        long time;

        try {
            time = Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            return "redirect:/s/meetings";
        }

        if (time < System.currentTimeMillis()) {
            return "redirect:/s/meetings";
        }

        meeting.setTime(time);
        meeting.setConfirmed(true);

        meetingRepository.save(meeting);

        return "redirect:/s/meetings";
    }

    @GetMapping("meetings/{id}/notes/add")
    public String addMeetingNotes(@PathVariable("id") long id, Model model, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetingOptional = meetingRepository.findById(id);

        if (meetingOptional.isEmpty()) {
            return "redirect:/s/meetings";
        }

        var meeting = meetingOptional.get();

        if (meeting.getRequestee().getId() != user.getId()) {
            return "redirect:/s/meetings";
        }

        model.addAttribute("meeting", meeting);

        return "supervisor/notes/add";
    }

    @GetMapping("meetings/{id}/notes")
    public String viewMeetingNotes(@PathVariable("id") long id, Model model, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetingOptional = meetingRepository.findById(id);

        if (meetingOptional.isEmpty()) {
            return "redirect:/s/meetings";
        }

        var meeting = meetingOptional.get();

        if (meeting.getRequestee().getId() != user.getId()) {
            return "redirect:/s/meetings";
        }

        model.addAttribute("meeting", meeting);

        return "supervisor/notes/view";
    }

    @PostMapping("meetings/notes/add")
    public String addMeetingNotes(@RequestParam("meetingId") long meetingId, @RequestParam("content") String content, RedirectAttributes redirectAttributes, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());
        var meetingOptional = meetingRepository.findById(meetingId);

        if (meetingOptional.isEmpty()) {
            return "redirect:/s/meetings";
        }

        var meeting = meetingOptional.get();

        if (meeting.getRequestee().getId() != user.getId()) {
            return "redirect:/s/meetings";
        }

        var note = noteRepository.save(new Note(
                content,
                NoteType.MEETING
        ));

        meeting.getNotes().add(note);

        meetingRepository.save(meeting);

        redirectAttributes.addFlashAttribute("message", "Meeting notes added successfully");

        return "redirect:/s/meetings";
    }

    @GetMapping("/settings")
    public String supervisorSettingsController(Model model, Authentication authentication) {
        var supervisor = userRepository.findByEmail(authentication.getName());
        model.addAttribute("supervisor", supervisor);
        model.addAttribute("alertthreshold", new SetSupervisorAlertThresholdForm(supervisor.getSettingsAlertThreshold()));
        return "supervisor/settings";
    }

    @PostMapping("/settings")
    public String supervisorSettingsControllerPost(@RequestParam("icalUrl") @Nullable String icalUrl, RedirectAttributes redirectAttributes, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName());

        if (icalUrl != null && !icalUrl.isBlank()) {
            CalendarBuilder builder = new CalendarBuilder();
            try {
                var calendar = builder.build(new URL(icalUrl).openStream());
                boolean hasEvents = calendar.getComponents()
                        .stream().anyMatch(VEvent.class::isInstance);

                if (!hasEvents) {
                    redirectAttributes.addFlashAttribute("error", "No events found in calendar");
                    return "redirect:/s/settings";
                }

                user.setIcalUrl(icalUrl);
                userRepository.save(user);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Invalid iCal URL");
                return "redirect:/s/settings";
            }
        }

        return "redirect:/s/settings";
    }

    @PostMapping("/settings/alert/threshold")
    public String setSupervisorAlertThresholdSettings(RedirectAttributes redirectAttributes, @ModelAttribute SetSupervisorAlertThresholdForm setSupervisorAlertThresholdForm, Authentication authentication) {
        if (setSupervisorAlertThresholdForm.getThreshold() > 10 || setSupervisorAlertThresholdForm.getThreshold() < 0) {
            redirectAttributes.addFlashAttribute("flagstatus", "error");
            redirectAttributes.addFlashAttribute("flagmessage", "Invalid Value for Threshold");
            return "redirect:/s/settings";
        }
        User supervisor = userRepository.findByEmail(authentication.getName());
        supervisor.setSettingsAlertThreshold(setSupervisorAlertThresholdForm.getThreshold());
        userRepository.save(supervisor);
        redirectAttributes.addFlashAttribute("flagstatus", "success");
        redirectAttributes.addFlashAttribute("flagmessage", "Updated Flag Threshold");
        return "redirect:/s/settings";
    }

    @GetMapping("/employees")
    public String supervisorEmployeesController(Model model, Authentication authentication) {
        Team supervisorteam = teamRepository.findBySupervisorEmail(authentication.getName());
        List<User> employees = userRepository.findByTeamIdAndSupervisorIsFalseAndEmployeeIsTrue(supervisorteam.getId());
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