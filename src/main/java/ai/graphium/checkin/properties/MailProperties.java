package ai.graphium.checkin.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("mail")
@Component
@Getter
@Setter
public class MailProperties {

    private String host;
    private String username;
    private String password;

}
