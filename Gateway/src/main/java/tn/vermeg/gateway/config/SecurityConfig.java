package tn.vermeg.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuration sécurité du Gateway.
 *
 * Sans cette classe, spring-boot-starter-security applique son verrouillage réactif
 * par défaut (authentification HTTP Basic exigée sur TOUTES les routes, y compris les
 * routes proxifiées vers les microservices), ce qui rendait le Gateway totalement
 * inopérant : toute requête, y compris /api/produits/** relayée vers gestionProduit,
 * échouait en 401 avant même d'atteindre la route Spring Cloud Gateway. Cela n'avait
 * pas été détecté car le frontend interroge directement gestionProduit/chatbot-service
 * en développement, sans transiter par le Gateway.
 *
 * Posture actuelle volontairement alignée sur GestionProduit (permitAll) : la validation
 * JWT n'est pas encore activée ici (dépendances OAuth2 resource server absentes), c'est
 * une action de durcissement identifiée avant toute exposition publique.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .authorizeExchange(auth -> auth.anyExchange().permitAll())
            .build();
    }
}
