package ai.graphium.checkin.services.security;

import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;

@Getter
public class TotpWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String totp;

    public TotpWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        totp = request.getParameter("totp");
    }
}
