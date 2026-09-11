package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de sécurité Spring Security (Spring Boot 3.x).
 *
 * RS-01 : Les endpoints /api/reservations/** ne sont PLUS en permitAll().
 *         Ils nécessitent une authentification JWT valide.
 *
 * RS-02 / RS-03 : Les autorisations fines sont gérées par @PreAuthorize
 *                 au niveau des contrôleurs et services.
 *
 * La matrice des autorisations est documentée dans le README.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtRequestFilter jwtRequestFilter;

    public WebSecurityConfiguration(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                     CustomAccessDeniedHandler customAccessDeniedHandler,
                                     JwtRequestFilter jwtRequestFilter) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                // RS-01 : Seuls les endpoints publics sont en permitAll()
                // Les endpoints /api/reservations/** nécessitent maintenant une authentification
                .authorizeHttpRequests(auth -> auth
                        // Utiliser des matchers basés sur l'URI (évite le bug MvcRequestMatcher sous Tomcat)
                        .requestMatchers(request -> request.getRequestURI().equals("/authenticate")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().startsWith("/swagger-ui")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().equals("/swagger-ui.html")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().startsWith("/v3/api-docs")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().startsWith("/swagger-resources")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().startsWith("/webjars")).permitAll()
                        .requestMatchers(request -> request.getRequestURI().equals("/favicon.ico")).permitAll()
                        // RS-01 : Toutes les requêtes non explicitement permises nécessitent une authentification
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}