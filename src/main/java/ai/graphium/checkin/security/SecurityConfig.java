package ai.graphium.checkin.security;

import ai.graphium.checkin.entity.Team;
import ai.graphium.checkin.entity.User;
import ai.graphium.checkin.enums.UserType;
import ai.graphium.checkin.properties.AuthProperties;
import ai.graphium.checkin.repos.TeamRepository;
import ai.graphium.checkin.repos.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private static final String DEFAULT_USERNAME = "admin@graphium.ai";
    private static final String DEFAULT_PASSWORD = "admin";

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth,
                                UserDetailsService userDetailsService,
                                PasswordEncoder passwordEncoder,
                                UserRepository userRepository,
                                TeamRepository teamRepository,
                                AuthProperties authProperties) throws Exception {

        // adding default admin user
        var admin = userRepository
                .findByEmail(DEFAULT_USERNAME);
        if (admin == null) {
            admin = new User(DEFAULT_USERNAME, passwordEncoder.encode(DEFAULT_PASSWORD), UserType.ADMIN, "Admin", "+441234567890");
            userRepository.save(admin);
        }
        if (authProperties.isGenerateDefaultUsers()) {
            var supervisor = userRepository
                    .findByEmail("supervisor@graphium.ai");
            if (supervisor == null) {
                supervisor = new User("supervisor@graphium.ai", passwordEncoder.encode("supervisor"), UserType.SUPERVISOR, "Supervisor", "+441234567890");
                userRepository.save(supervisor);
            }
            var employee = userRepository
                    .findByEmail("employee@graphium.ai");
            if (employee == null) {
                employee = new User("employee@graphium.ai", passwordEncoder.encode("employee"), UserType.EMPLOYEE, "Employee", "+441234567890");
                userRepository.save(employee);
            }
            var teams = teamRepository
                    .findAll();
            if (teams.size() < 1) {
                var team1 = new Team(supervisor, "Dev Ops");
                employee.setTeam(team1);
                teamRepository.save(team1);
                userRepository.save(employee);
            }
        }


        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                .and()
                .authorizeRequests()
                .antMatchers("/logo.gif")
                .permitAll()
                .antMatchers("/js/**")
                .permitAll()
                .antMatchers("/login")
                .permitAll()
                .antMatchers("/logout")
                .permitAll()
                .antMatchers("/**")
                .fullyAuthenticated()
                .and()
                .formLogin()
                .usernameParameter("email")
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .and()
                .logout()
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID");
        return http.build();
    }

}
