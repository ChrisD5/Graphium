package ai.graphium.checkin.controllers;

import ai.graphium.checkin.forms.CheckinSubmission;
import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.Note;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.repos.CheckInRepository;
import ai.graphium.checkin.repos.NoteRepository;
import ai.graphium.checkin.repos.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/e")
@Secured("ROLE_EMPLOYEE")
public class EmployeeController {

    @GetMapping("")
    public String employeeHomeController() {
        return "employee/index";
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private CheckInRepository checkInRepository;

    @PostMapping("/checkin")
    public String checkin(CheckinSubmission checkinSubmission, Authentication authentication, RedirectAttributes atts) {

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

        return "redirect:/e";
    }

    @GetMapping("/settings")
    public String employeeSettingsController() {
        return "employee/settings";
    }
}
