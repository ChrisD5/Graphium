package ai.graphium.checkin.security;

import ai.graphium.checkin.repos.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        var user = userRepository.findByEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        if (user.isAdmin())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        if (user.isSupervisor())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_SUPERVISOR"));
        if (user.isEmployee())
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

        return User.withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(grantedAuthorities)
                .build();
    }
}
