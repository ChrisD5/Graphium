package ai.graphium.checkin.services;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@AllArgsConstructor
@Service
public class MailerService {

    private JavaMailSender emailSender;
    private SpringTemplateEngine templateEngine;

    @Async
    public void sendMail(String to, String subject, String text) throws MessagingException {

        if (to.toLowerCase().endsWith("@grapium.ai")) {
            System.err.println("Not sending email, since we're in a development stage!");
            return;
        }

        // send html mail
        MimeMessage message = emailSender.createMimeMessage();

        Context context = new Context();
        context.setVariable("text", text);

        var html = templateEngine.process("mail", context);

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("checkin@kavin.rocks");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        emailSender.send(message);
    }
}
