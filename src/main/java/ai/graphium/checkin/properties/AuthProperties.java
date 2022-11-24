package ai.graphium.checkin.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties("auth")
@Component
@Getter
@Setter
public class AuthProperties {

    private boolean generateDefaultUsers;

}
