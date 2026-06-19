package tn.vermeg.gestionuser.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        return ResponseEntity.ok(
                Map.of(
                        "username", authentication.getName(),
                        "authorities", authentication.getAuthorities()
                )
        );
    }
}