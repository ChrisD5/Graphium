package ai.graphium.checkin.services;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    public void sendMail(String to, String name, String subject, String text) throws MessagingException {

        if (to.toLowerCase().endsWith("@graphium.ai")) {
            System.err.println("Not sending email, since we're in a development stage!");
            return;
        }

        // send html mail
        MimeMessage message = emailSender.createMimeMessage();

        Context context = new Context();
        context.setVariable("text", text);
        context.setVariable("name", name);

        var html = templateEngine.process("mail", context);

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("checkin@kavin.rocks");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        emailSender.send(message);
    }
}
