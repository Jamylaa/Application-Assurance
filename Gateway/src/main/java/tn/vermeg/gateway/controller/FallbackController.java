package tn.vermeg.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        logger.warn("User service fallback triggered - service unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Le service de gestion des utilisateurs est temporairement indisponible. Veuillez réessayer ultérieurement.");
        response.put("service", "GESTIONUSER");
        response.put("path", "/api/auth/**, /api/users/**");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/product")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        logger.warn("Product service fallback triggered - service unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Le service de gestion des produits est temporairement indisponible. Veuillez réessayer ultérieurement.");
        response.put("service", "GESTIONPRODUIT");
        response.put("path", "/api/produits/**, /api/packs/**, /api/garanties/**, /api/chatbot/**");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/recommendation")
    public ResponseEntity<Map<String, Object>> recommendationServiceFallback() {
        logger.warn("Recommendation service fallback triggered - service unavailable");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", "Le service de recommandation est temporairement indisponible. Veuillez réessayer ultérieurement.");
        response.put("service", "GESTIONPRODUIT");
        response.put("path", "/api/recommendations/**");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
