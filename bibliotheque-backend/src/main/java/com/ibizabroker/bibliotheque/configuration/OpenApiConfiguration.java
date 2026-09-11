package com.ibizabroker.bibliotheque.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI/Swagger avec support de l'authentification JWT.
 *
 * Le bouton "Authorize" dans Swagger UI permet de saisir le token JWT.
 * Format: collez la valeur de jwtToken (sans le préfixe "Bearer").
 */
@Configuration
public class OpenApiConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI bibliothequeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Bibliothèque - Module Réservation Sécurisé")
                        .version("v1")
                        .description("API de gestion de bibliothèque avec sécurité JWT.\n\n"
                                + "**Authentification** : Utilisez POST /authenticate pour obtenir un token JWT.\n"
                                + "**Rôles** : Admin (BIBLIOTHECAIRE) et User (ADHERENT).\n"
                                + "**Sécurité** : Les endpoints /api/reservations/** nécessitent un token valide."))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("1. Appelez POST /authenticate avec username/password\n"
                                        + "2. Copiez la valeur de jwtToken dans la réponse\n"
                                        + "3. Collez-la ici (sans le préfixe Bearer)\n\n"
                                        + "**Rôles disponibles** :\n"
                                        + "- Admin (BIBLIOTHECAIRE) : accès complet\n"
                                        + "- User (ADHERENT) : accès à ses propres données")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}