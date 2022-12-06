package ai.graphium.checkin.scheduling;

import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.services.AlertService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.criteria.JoinType;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Service
public class ReminderService {

    EntityManager em;
    private AlertService alertService;

    @Async
    @Scheduled(cron = "*/60 * * * * MON-FRI")
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
        var users = em.createQuery(cq).getResultList();
        users.stream()
                .filter(user -> {
                    var checkIns = user.getCheckIns();
                    var supervisor = user.getTeam().getSupervisor();
                    return checkIns.isEmpty() || checkIns.stream().noneMatch(checkIn -> checkIn.getTime() > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(supervisor.getSettingsAlertThreshold()));
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
}
