package tn.vermeg.gestionuser.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
     // Hash un mot de passe avec BCrypt
     // @param plainPassword Le mot de passe en clair + @return Le mot de passe hashé
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);}
    /**
     * Vérifie si un mot de passe correspond au hash
     * @param plainPassword Le mot de passe en clair + @param hashedPassword Le mot de passe hashé
     * @return true si le mot de passe correspond
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);}
     // Génère un mot de passe temporaire + @return Un mot de passe temporaire de 8 caractères
    public String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * chars.length());
            password.append(chars.charAt(index));}
        return password.toString();}
}