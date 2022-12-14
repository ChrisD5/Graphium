package ai.graphium.checkin.scheduling;

import ai.graphium.checkin.entity.Meeting;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.services.AlertService;
import ai.graphium.checkin.services.MailerService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class ReminderService {

    private final MailerService mailerService;
    EntityManager em;
    private AlertService alertService;

    @Async
    @Scheduled(cron = "0 0 0 * * MON-FRI")
    public void missingCheckIn() {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(User.class);
        var root = cq.from(User.class);
        cq.select(root);
        cq.where(cb.and(
                cb.isTrue(root.get("employee")),
                cb.isNotNull(root.get("team"))
        ));
        root.fetch("checkIns", JoinType.LEFT);
        root.fetch("team", JoinType.INNER).fetch("supervisor", JoinType.INNER);
        cq.distinct(true);
        var users = em.createQuery(cq).getResultList();
        users.stream()
                .filter(user -> {
                    var checkIns = user.getCheckIns();
                    var supervisor = user.getTeam().getSupervisor();
                    return !user.isSettingsAlertDisabled() && (checkIns.isEmpty() || checkIns.stream().noneMatch(checkIn -> checkIn.getTime() > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(supervisor.getSettingsAlertThreshold())));
                })
                .forEach(user -> {
                    var team = user.getTeam();
                    var supervisor = team.getSupervisor();
                    var plural = supervisor.getSettingsAlertThreshold() > 1 ? 's' : '\0';
                    try {
                        alertService.createAlert("Missing Check-In",
                                String.format("You have not checked in for more than %d day%s. Please do not forget to check-in!", supervisor.getSettingsAlertThreshold(), plural),
                                String.format("Your employee " + user.getName() + " has not checked in for more than %d day%s.", supervisor.getSettingsAlertThreshold(), plural),
                                AlertType.LOW, AlertVisibility.ALL, user, supervisor);
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Async
    @Scheduled(cron = "0 0 * * * MON-FRI")
    public void reminderCheckIn() {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(User.class);
        var root = cq.from(User.class);
        cq.select(root);
        cq.where(cb.and(
                cb.isTrue(root.get("employee")),
                cb.isNotNull(root.get("team"))
        ));
        root.fetch("checkIns", JoinType.LEFT);
        root.fetch("team", JoinType.INNER).fetch("supervisor", JoinType.INNER);
        cq.distinct(true);
        var users = em.createQuery(cq).getResultList();
        users.stream()
                .filter(user -> {
                    var checkIns = user.getCheckIns();
                    LocalTime time = LocalTime.parse(user.getSettingsAlertReminder().toString());
                    long timeLong = System.currentTimeMillis();
                    timeLong -= timeLong % TimeUnit.DAYS.toMillis(1);
                    final long timeToCheck = timeLong;
                    return !user.isSettingsAlertDisabled() && (time.getHour() == LocalTime.now().getHour()) && (checkIns.isEmpty() || checkIns.stream().noneMatch(checkIn -> checkIn.getTime() > timeToCheck));
                })
                .forEach(user -> {
                    try {
                        alertService.createAlert("Check-In Reminder",
                                "This is your daily reminder to check-in. Please do not forget!",
                                "Your employee " + user.getName() + " has just been reminded to check-in",
                                AlertType.LOW, AlertVisibility.EMPLOYEE, user, user.getTeam().getSupervisor());
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Async
    @Transactional
    @Scheduled(cron = "0 * * * * MON-FRI")
    public void reminderMeeting() {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Meeting.class);
        var root = cq.from(Meeting.class);
        root.fetch("requester", JoinType.INNER);
        root.fetch("requestee", JoinType.INNER);
        cq.select(root);
        cq.where(cb.and(
                cb.greaterThan(root.get("time"), System.currentTimeMillis()),
                cb.lessThan(root.get("time"), System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)),
                cb.isFalse(root.get("reminded"))
        ));
        var meetings = em.createQuery(cq).getResultList();
        meetings.forEach(meeting -> {
            try {
                mailerService.sendMail(meeting.getRequester().getEmail(),
                        "Meeting Reminder",
                        "You have a meeting with " + meeting.getRequestee().getName() + " in 1 hour."
                );
                mailerService.sendMail(meeting.getRequestee().getEmail(),
                        "Meeting Reminder",
                        "You have a meeting with " + meeting.getRequester().getName() + " in 1 hour."
                );
                meeting.setReminded(true);
                em.merge(meeting);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
