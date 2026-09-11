package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
        // CORS : activer le CorsFilter de Spring Security avec le bean CorsConfigurationSource.
        // CRUCIAL : le preflight OPTIONS du navigateur doit passer AVANT tout contrôle d'authentification,
        // sinon le frontend (http://localhost:4200) ne peut jamais appeler le backend (401 sur OPTIONS).
        http.cors(Customizer.withDefaults())
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

    /**
     * Configuration CORS utilisée par le CorsFilter de Spring Security.
     * Le preflight OPTIONS est autorisé pour l'origine du frontend Angular.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "No-Auth"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}