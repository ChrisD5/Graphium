package ai.graphium.checkin.services;

import ai.graphium.checkin.entity.Alert;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.AlertType;
import ai.graphium.checkin.enums.AlertVisibility;
import ai.graphium.checkin.repos.AlertRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

@AllArgsConstructor
@Service
public class AlertService {

    private MailerService mailerService;
    private AlertRepository alertRepository;

    @Async
    public void createAlert(String title, String targetMessage, String supervisorMessage, AlertType type, AlertVisibility visibility, User target, User supervisor) throws MessagingException {

        Alert alert = new Alert(target, supervisor, title, targetMessage, supervisorMessage, type, visibility);
        alertRepository.save(alert);

//        if (visibility == AlertVisibility.EMPLOYEE || visibility == AlertVisibility.ALL) {
//            mailerService.sendMail(target.getEmail(), title, targetMessage);
//        }

        // Alert supervisor only if the alert has a medium or higher type
        if (type.compareTo(AlertType.MEDIUM) >= 0
                && (visibility == AlertVisibility.SUPERVISOR || visibility == AlertVisibility.ALL)) {
            mailerService.sendMail(supervisor.getEmail(), title, supervisorMessage);
        }
    }
}
