package ai.graphium.checkin.controllers;

import ai.graphium.checkin.entity.Alert;
import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.Note;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.forms.CheckinSubmission;
import ai.graphium.checkin.forms.EditEmployeeForm;
import ai.graphium.checkin.repos.AlertRepository;
import ai.graphium.checkin.repos.CheckInRepository;
import ai.graphium.checkin.repos.NoteRepository;
import ai.graphium.checkin.repos.UserRepository;
import ai.graphium.checkin.services.AlertService;
import ai.graphium.checkin.services.EmployeeService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Base64;

@Controller
@RequestMapping("/e")
@Secured("ROLE_EMPLOYEE")
@AllArgsConstructor
public class EmployeeController {

    private final AlertRepository alertRepository;
    private UserRepository userRepository;
    private EmployeeService employeeService;
    private NoteRepository noteRepository;
    private CheckInRepository checkInRepository;
    private AlertService alertService;

    @GetMapping("")
    public String employeeHomeController(Model model, Authentication authentication) {

        boolean hasCheckedInToday = employeeService.hasCheckedInToday(authentication.getName());
        var user = userRepository.findByEmail(authentication.getName());

        var alerts = user.getAlerts();
        boolean noUnreadAlerts = alerts.stream().allMatch(Alert::isReadByTarget);
        int unreadCount = (int) alerts.stream().filter(alert -> !alert.isReadByTarget()).count();

        model.addAttribute("checkedIn", hasCheckedInToday);
        model.addAttribute("noUnreadAlerts", noUnreadAlerts);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("alerts", alerts);

        return "employee/index";
    }

    @PostMapping("/checkin")
    public String checkin(CheckinSubmission checkinSubmission, Authentication authentication, RedirectAttributes atts) throws Exception {

        if (employeeService.hasCheckedInToday(authentication.getName())) {
            atts.addFlashAttribute("error", "You have already checked in today.");
            return "redirect:/e";
        }

        String ratingStr = checkinSubmission.getRating();
        int rating;

        if (StringUtils.isEmpty(ratingStr) || !StringUtils.isNumeric(ratingStr) || (rating = Integer.parseInt(ratingStr)) < 1 || rating > 10) {
            atts.addFlashAttribute("error", "Please enter a valid rating");
            return "redirect:/e";
        }

        Note note;

        var comment = checkinSubmission.getComment();

        if (!StringUtils.isEmpty(comment))
            note = new Note(comment, NoteType.CHECKIN);
        else
            note = null;

        if (note != null)
            noteRepository.saveAndFlush(note);

        var checkIn = new CheckIn(rating, System.currentTimeMillis(), note);

        checkInRepository.save(checkIn);

        var user = userRepository.findByEmail(authentication.getName());

        user.getCheckIns().add(checkIn);

        userRepository.save(user);

        atts.addFlashAttribute("message", "Successfully checked in!");

        if (checkIn.getRating() <= 3)
            alertService.createAlert(
                    "Low check-in rating",
                    "Are you okay? Your check-in rating was " + checkIn.getRating() + " out of 10 today. Would you like to schedule a meeting with your supervisor?",
                    "Your employee " + user.getName() + " has checked-in with a rating of " + checkIn.getRating() + " out of 10 today. Would it help to schedule an meeting with them?",
                    AlertType.MEDIUM, AlertVisibility.ALL,
                    user, user.getTeam().getSupervisor());

        return "redirect:/e";
    }

    @PostMapping("alert/read")
    public String readAlert(@RequestParam("id") long id, Authentication authentication) {

        var user = userRepository.findByEmail(authentication.getName());

        var alertOptional = alertRepository.findById(id);

        if (alertOptional.isEmpty()) {
            return "redirect:/e";
        }

        var alert = alertOptional.get();

        if (alert.getTarget().getId() != user.getId()) {
            return "redirect:/e";
        }

        alert.setReadByTarget(true);
        alertRepository.save(alert);

        return "redirect:/e";
    }

    @GetMapping("/settings")
    public String employeeSettingsController() {
        return "employee/settings";
    }

    @GetMapping("/profile")
    public String employeeProfileController(Model model, Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("image", user.getImage() != null ? Base64.getEncoder().encodeToString(user.getImage()) : null);
        return "employee/employee-profile";
    }

    @GetMapping("/profile/edit")
    public String profileForm(Model model, Authentication authentication) {
        var userLookUp = userRepository.findByEmail(authentication.getName());
        if (userLookUp == null) {
            return "redirect:/e/profile";
        }
        model.addAttribute("user", new EditEmployeeForm(userLookUp.getName(), userLookUp.getPhone()));
        model.addAttribute("image", userLookUp.getImage() != null ? Base64.getEncoder().encodeToString(userLookUp.getImage()) : null);
        return "employee/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String formSubmit(@ModelAttribute EditEmployeeForm user, Model model, Authentication authentication, @RequestParam("image") MultipartFile file) throws IOException {
        var userLookUp = userRepository.findByEmail(authentication.getName());
        userLookUp.setName(user.getName());
        userLookUp.setPhone(user.getPhone());
        userLookUp.setImage(file.getBytes());
        userRepository.save(userLookUp);
        return "redirect:/e/profile";

    }
}
