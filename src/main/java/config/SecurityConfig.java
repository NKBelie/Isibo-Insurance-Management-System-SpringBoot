package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import user.domain.User;
import user.domain.SystemRole;
import user.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public ApplicationRunner normalizeStoredUserRoles(UserRepository userRepository) {
        return args -> {
            userRepository.normalizeAdministratorRoles();
            userRepository.normalizeInsuranceOfficerRoles();
            userRepository.normalizeCustomerRoles();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPasswordHash())
                        .authorities(SystemRole.from(user.getRole()).authority())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public ApplicationRunner seedConfiguredAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.email:}") String email) {
        return args -> {
            if (username.isBlank() && password.isBlank() && email.isBlank()) {
                return;
            }
            if (username.isBlank() || password.isBlank() || email.isBlank()) {
                throw new IllegalStateException(
                        "Configure all app.bootstrap-admin.username, password, and email values to bootstrap an administrator.");
            }
            if (password.length() < 8) {
                throw new IllegalStateException("The bootstrap administrator password must have at least 8 characters.");
            }
            if (userRepository.findByUsername(username.trim()).isEmpty()) {
                User admin = new User();
                admin.setUsername(username.trim());
                admin.setPasswordHash(passwordEncoder.encode(password));
                admin.setRole(SystemRole.ADMINISTRATOR.name());
                admin.setEmail(email.trim());
                userRepository.save(admin);
            }
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/access-denied", "/css/**", "/js/**", "/images/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index", "/home", "/dashboard").authenticated()
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/officer/dashboard").hasRole("INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/customer/dashboard").hasRole("CUSTOMER")
                        .requestMatchers("/reports").hasRole("ADMINISTRATOR")
                        .requestMatchers("/insurance-application").hasRole("CUSTOMER")

                        .requestMatchers("/api/users/**", "/users/**", "/api/providers/**", "/api/officers/**",
                                "/providers/**", "/officers/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/claims/*/review").hasRole("INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/claims/*/decision", "/claims/*/decision").hasRole("INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/members/new", "/members/*/edit")
                                .hasAnyRole("ADMINISTRATOR", "INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/members", "/members/*/update")
                                .hasAnyRole("ADMINISTRATOR", "INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/members/**")
                                .hasAnyRole("ADMINISTRATOR", "INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.PUT, "/api/members/**")
                                .hasAnyRole("ADMINISTRATOR", "INSURANCE_OFFICER")
                        .requestMatchers(HttpMethod.GET, "/claims/new", "/claims/*/edit",
                                "/payments/new", "/payments/*/edit",
                                "/insurance-plans/new", "/insurance-plans/*/edit",
                                "/coverages/new", "/coverages/*/edit").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.POST, "/claims", "/claims/*/update", "/claims/*/delete",
                                "/payments", "/payments/*/update", "/payments/*/delete",
                                "/members/**", "/insurance-plans/**", "/coverages/**",
                                "/providers/**", "/officers/**", "/users/**",
                                "/api/insurance-plans/**", "/api/coverage/**",
                                "/api/payments/**", "/api/providers/**", "/api/officers/**", "/api/users/**")
                                .hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/insurance-plans/**",
                                "/api/coverage/**", "/api/payments/**", "/api/providers/**",
                                "/api/officers/**", "/api/users/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/members/**", "/api/insurance-plans/**",
                                "/api/coverage/**", "/api/payments/**", "/api/providers/**",
                                "/api/officers/**", "/api/users/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/members/**", "/insurance-plans/**",
                                "/coverages/**", "/claims/**", "/payments/**",
                                "/api/members/**", "/api/insurance-plans/**", "/api/coverage/**",
                                "/api/claims/**", "/api/payments/**")
                                .hasAnyRole("ADMINISTRATOR", "INSURANCE_OFFICER")
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .exceptionHandling(exception -> exception.accessDeniedPage("/access-denied"))
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll());
        return http.build();
    }

}
