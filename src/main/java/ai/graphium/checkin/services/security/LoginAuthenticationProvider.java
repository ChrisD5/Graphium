package ai.graphium.checkin.services.security;

import ai.graphium.checkin.repos.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class LoginAuthenticationProvider implements AuthenticationProvider {

    private UserRepository userRepository;
    private CodeVerifier codeVerifier;
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        var user = userRepository.findByEmail(authentication.getName());

        if (user == null)
            throw new UsernameNotFoundException("User not found");

        if (user.isDisabled())
            throw new UsernameNotFoundException("User is disabled");

        if (!passwordEncoder.matches(authentication.getCredentials().toString(), user.getPassword()))
            throw new BadCredentialsException("Invalid credentials");

        String totpCode = ((TotpWebAuthenticationDetails) authentication.getDetails()).getTotp();
        String totpSecret = user.getTotpSecret();
        if (totpCode != null && totpSecret != null && !codeVerifier.isValidCode(user.getTotpSecret(), totpCode)) {
            throw new BadCredentialsException("Invalid TOTP code");
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        if (user.isAdmin())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (user.isSupervisor())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_SUPERVISOR"));
        if (user.isEmployee())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        return new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword(), grantedAuthorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

}
