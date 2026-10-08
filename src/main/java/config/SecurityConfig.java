package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import user.domain.User;
import user.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                        .roles(mapRoleToAuthority(user.getRole()))
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
                admin.setRole("Administrator");
                admin.setEmail(email.trim());
                userRepository.save(admin);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/api/users/**", "/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/providers/**", "/api/officers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/payments/**", "/api/members/**", "/api/insurance-plans/**",
                                "/api/coverage/**").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/api/payments/**", "/api/members/**", "/api/insurance-plans/**",
                                "/api/coverage/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/payments/**").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers("/payments/**").hasRole("ADMIN")
                        .requestMatchers("/api/claims/*/decision").hasRole("OFFICER")
                        .requestMatchers(HttpMethod.GET, "/api/claims/**").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers(HttpMethod.POST, "/api/claims/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/claims/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/claims/**").hasRole("ADMIN")
                        .requestMatchers("/claims/*/decision").hasRole("OFFICER")
                        .requestMatchers("/claims/*/review").hasRole("OFFICER")
                        .requestMatchers(HttpMethod.GET, "/claims/new", "/claims/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/claims", "/claims/*/update", "/claims/*/delete").hasRole("ADMIN")
                        .requestMatchers("/claims/**").hasAnyRole("ADMIN", "OFFICER")
                        .requestMatchers(HttpMethod.GET, "/members/new", "/members/*/edit",
                                "/insurance-plans/new", "/insurance-plans/*/edit",
                                "/coverages/new", "/coverages/*/edit").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/members/**", "/insurance-plans/**", "/coverages/**",
                                "/member/**", "/insurancePlan/**", "/coverage/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/member/**", "/insurancePlan/**", "/coverage/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/members/**", "/insurance-plans/**", "/coverages/**",
                                "/member/**", "/insurancePlan/**", "/coverage/**").hasRole("ADMIN")
                        .requestMatchers("/officers/**").hasRole("ADMIN")
                        .requestMatchers("/providers/**").hasRole("ADMIN")
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/dashboard", "/members/**", "/insurance-plans/**", "/coverages/**").hasAnyRole("ADMIN", "OFFICER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());
        return http.build();
    }

    private String mapRoleToAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "USER";
        }
        if ("Administrator".equalsIgnoreCase(role.trim())) {
            return "ADMIN";
        }
        if ("Insurance Officer".equalsIgnoreCase(role.trim())) {
            return "OFFICER";
        }
        return "USER";
    }
}
