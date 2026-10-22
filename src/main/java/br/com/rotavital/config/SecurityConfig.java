package br.com.rotavital.config;

import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import br.com.rotavital.repository.UserRepository;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            var user = users.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .filter(u -> u.getSenhaHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
            return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()).password(user.getSenhaHash())
                .roles("USUARIO").build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/login", "/login.html", "/registro", "/registro.html",
                    "/css/**", "/js/**", "/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                    org.springframework.http.HttpStatus.UNAUTHORIZED),
                request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
                .defaultAuthenticationEntryPointFor(
                    new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"),
                    request -> true))
            .formLogin(f -> f.loginPage("/login").loginProcessingUrl("/login")
                .usernameParameter("email").passwordParameter("senha")
                .defaultSuccessUrl("/estoque", true).failureUrl("/login?erro").permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?saiu")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .build();
    }
}
