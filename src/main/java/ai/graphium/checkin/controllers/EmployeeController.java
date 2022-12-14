package ai.graphium.checkin.controllers;

import ai.graphium.checkin.data.AvailableTime;
import ai.graphium.checkin.entity.Alert;
import ai.graphium.checkin.entity.CheckIn;
import ai.graphium.checkin.entity.Meeting;
import ai.graphium.checkin.entity.Note;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.enums.NoteType;
import ai.graphium.checkin.forms.CheckinSubmission;
import ai.graphium.checkin.forms.EditEmployeeForm;
import ai.graphium.checkin.forms.SetEmployeeReminderTime;
import ai.graphium.checkin.forms.ToggleEmployeeAlertDayForm;
import ai.graphium.checkin.repos.*;
import ai.graphium.checkin.services.AlertService;
import ai.graphium.checkin.services.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.AllArgsConstructor;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VFreeBusy;
import net.fortuna.ical4j.model.parameter.FbType;
import net.fortuna.ical4j.model.property.FreeBusy;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/e")
@Secured("ROLE_EMPLOYEE")
@AllArgsConstructor
public class EmployeeController {

    private final AlertRepository alertRepository;
    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final QrDataFactory qrDataFactory;
    private final CodeVerifier codeVerifier;
    private final ObjectMapper objectMapper;
    private final MeetingRepository meetingRepository;
    private UserRepository userRepository;
    private EmployeeService employeeService;
    private NoteRepository noteRepository;
    private CheckInRepository checkInRepository;
    private AlertService alertService;

    @GetMapping("")
    public String employeeHomeController(Model model, Authentication authentication) {

        boolean hasCheckedInToday = employeeService.hasCheckedInToday(authentication.getName());
        var user = userRepository.findByEmail(authentication.getName());
        var allAlerts = user.getAlerts();

        var alerts = allAlerts.stream().filter(alert -> alert.getVisibility().equals(AlertVisibility.EMPLOYEE) || alert.getVisibility().equals(AlertVisibility.ALL)).toList();
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
    public String employeeSettingsController(Model model, Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName());
        boolean IsAlertsDisabled = user.isSettingsAlertDisabled();
        Set<String> IsAlertsDayDisabled = user.getSettingsAlertDayDisabled();
        if (model.getAttribute("toggleemployeealertdayform") == null) {
            model.addAttribute("toggleemployeealertdayform", new ToggleEmployeeAlertDayForm());
        }
        model.addAttribute("remindertime", new SetEmployeeReminderTime(user.getSettingsAlertReminder().toString()));
        model.addAttribute("IsAlertsDisabled", IsAlertsDisabled);
        model.addAttribute("IsAlertsDayDisabled", IsAlertsDayDisabled);

        System.out.println(IsAlertsDayDisabled.contains("MONDAY"));
        model.addAttribute("monday", IsAlertsDayDisabled.contains("MONDAY"));
        model.addAttribute("tuesday", IsAlertsDayDisabled.contains("TUESDAY"));
        model.addAttribute("wednesday", IsAlertsDayDisabled.contains("WEDNESDAY"));
        model.addAttribute("thursday", IsAlertsDayDisabled.contains("THURSDAY"));
        model.addAttribute("friday", IsAlertsDayDisabled.contains("FRIDAY"));
        return "employee/settings";
    }

    @PostMapping("/settings/alert/disable")
    public String disableAlert(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName());
        user.setSettingsAlertDisabled(!user.isSettingsAlertDisabled());
        userRepository.save(user);
        return "redirect:/e/settings";
    }

    @PostMapping("/settings/alert/toggleday")
    public String disableAlertDay(Authentication authentication, @ModelAttribute ToggleEmployeeAlertDayForm toggleEmployeeAlertDayForm) {
        var user = userRepository.findByEmail(authentication.getName());

        String day = toggleEmployeeAlertDayForm.getDay();
        if (user.getSettingsAlertDayDisabled().contains(day)) {
            user.getSettingsAlertDayDisabled().remove(day);
        } else {
            user.getSettingsAlertDayDisabled().add(day);
        }

        userRepository.save(user);
        return "redirect:/e/settings";
    }


    @PostMapping("/settings/alert/reminder")
    public String setAlertReminder(Authentication authentication, @ModelAttribute SetEmployeeReminderTime setEmployeeReminderTime) {
        var user = userRepository.findByEmail(authentication.getName());
        Time time = Time.valueOf(setEmployeeReminderTime.getStringTime());
        user.setSettingsAlertReminder(time);
        userRepository.save(user);
        return "redirect:/e/settings";
    }

    @GetMapping("/meeting")
    public String employeeMeetingController(Model model, Authentication authentication) throws IOException, ParserException {

        var user = userRepository.findByEmail(authentication.getName());
        var supervisor = user.getTeam().getSupervisor();

        assert supervisor != null;

        String icalUrl = supervisor.getIcalUrl();

        var availableTimes = getAvailableTimes(icalUrl);

        model.addAttribute("availableTimes",
                objectMapper.writeValueAsString(availableTimes)
        );

        return "employee/schedule-meeting";
    }

    @PostMapping("/schedule")
    public String employeeMeetingController(@RequestParam("time") long start, Authentication authentication, RedirectAttributes atts) throws IOException, ParserException {
        var user = userRepository.findByEmail(authentication.getName());
        var supervisor = user.getTeam().getSupervisor();

        assert supervisor != null;

        var availableTimes = getAvailableTimes(supervisor.getIcalUrl());

        for (var availableTime : availableTimes) {
            if (start >= availableTime.getStart() && start <= availableTime.getEnd() - TimeUnit.MINUTES.toMillis(30)) {
                var meeting = new Meeting(start, user, supervisor, "https://meet.jit.si/" + UUID.randomUUID());
                meetingRepository.save(meeting);
                atts.addFlashAttribute("message", "Successfully scheduled meeting!");
                return "redirect:/e/meeting";
            }
        }

        atts.addFlashAttribute("error", "Invalid time selected");

        return "redirect:/e/meeting";
    }

    private List<AvailableTime> getAvailableTimes(String icalUrl) throws IOException, ParserException {
        List<AvailableTime> availableTimes = new ArrayList<>();

        if (icalUrl == null) {
            // assume that supervisor is always free
            for (int i = 0; i < 14; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, i);
                var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    // skip weekends
                    continue;
                }

                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                cal.set(Calendar.HOUR_OF_DAY, 9);
                long start = cal.getTimeInMillis();

                cal.set(Calendar.HOUR_OF_DAY, 17);
                long end = cal.getTimeInMillis();

                availableTimes.add(new AvailableTime(start, end));
            }
        } else {
            var calendar = new CalendarBuilder().build(new URL(icalUrl).openStream());
            // find all mondays to fridays in the next
            // 2 weeks

            // loop through each day from now to 2 weeks from now
            for (int i = 0; i < 14; i++) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, i);
                var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    // skip weekends
                    continue;
                }

                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                cal.set(Calendar.HOUR_OF_DAY, 9);
                var start = cal.getTime();
                cal.set(Calendar.HOUR_OF_DAY, 17);
                var end = cal.getTime();
                var request = new VFreeBusy(new DateTime(start), new DateTime(end), Duration.ofMinutes(30));
                ComponentList<CalendarComponent> busyTimes = new ComponentList<>();
                busyTimes.addAll(calendar.getComponents());
                var response = new VFreeBusy(request, busyTimes);
                var freeBusy = (FreeBusy) response.getProperty("FREEBUSY");
                boolean isFree = freeBusy.getParameter(Parameter.FBTYPE) == FbType.FREE;
                if (isFree) {
                    freeBusy.getPeriods().forEach(period -> {
                        System.out.println(new Date(period.getStart().getTime()) + " - " + new Date(period.getEnd().getTime()));
                        availableTimes.add(new AvailableTime(period.getStart().getTime(), period.getEnd().getTime()));
                    });
                }
            }
        }

        return availableTimes;
    }

    @GetMapping("/profile")
    public String employeeProfileController(Model model, Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("image", user.getImage() != null ? Base64.getEncoder().encodeToString(user.getImage()) : null);
        model.addAttribute("checkins",
                user.getCheckIns()
                        .stream()
                        .sorted(Comparator.comparingLong(CheckIn::getTime).reversed())
                        .toList());
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
    public String formSubmit(@ModelAttribute EditEmployeeForm user, Authentication authentication, @RequestParam("image") MultipartFile file) throws IOException {
        var userLookUp = userRepository.findByEmail(authentication.getName());
        userLookUp.setName(user.getName());
        userLookUp.setPhone(user.getPhone());
        userLookUp.setImage(file.getBytes());
        userRepository.save(userLookUp);
        return "redirect:/e/profile";

    }

    @GetMapping("2fa")
    public String twoFactorAuth(Model model, Authentication authentication) throws QrGenerationException {
        String secret = model.containsAttribute("secret") ?
                (String) model.getAttribute("secret") :
                secretGenerator.generate();
        var qrData = qrDataFactory.newBuilder()
                .secret(secret)
                .issuer("Graphium")
                .label(authentication.getName())
                .build();

        model.addAttribute("secret", secret);
        model.addAttribute("qrdata", Base64.getEncoder().encodeToString(qrGenerator.generate(qrData)));

        return "employee/2fa";
    }

    @PostMapping("2fa")
    public String twoFactorAuth(@RequestParam("secret") String secret, @RequestParam("totp") String totp, Authentication authentication, RedirectAttributes redirectAttributes) {
        var user = userRepository.findByEmail(authentication.getName());

        redirectAttributes.addFlashAttribute("secret", secret);

        if (user.getTotpSecret() != null) {
            redirectAttributes.addFlashAttribute("error", "You have already enabled 2FA");
            return "redirect:/e/2fa";
        }

        if (!secret.matches("^[A-Z\\d]{32}$")) {
            redirectAttributes.addFlashAttribute("error", "Secret appears to be invalid");
            return "redirect:/e/2fa";
        }

        if (!totp.matches("^\\d{6}$")) {
            redirectAttributes.addFlashAttribute("error", "Invalid code provided");
            return "redirect:/e/2fa";
        }

        if (!codeVerifier.isValidCode(secret, totp)) {
            redirectAttributes.addFlashAttribute("error", "Invalid code provided");
            return "redirect:/e/2fa";
        }

        user.setTotpSecret(secret);
        userRepository.save(user);

        return "redirect:/e";
    }
}
