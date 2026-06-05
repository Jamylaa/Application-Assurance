package tn.vermeg.gestionuser.services;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
@Service
public class MailService {

        private final JavaMailSender mailSender;

        @Value("${spring.mail.username}")
        private String fromEmail;

        public MailService(JavaMailSender mailSender) {
            this.mailSender = mailSender;
        }

        public void sendEmail(String to, String subject, String body) {

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            try {
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Erreur email: " + e.getMessage());
            }
        }

        // ✔ utilisateur créé via Keycloak
        public void sendAccountCreated(String to, String username) {
            sendEmail(
                    to,
                    "Compte créé",
                    "Bonjour " + username + ",\n\nVotre compte a été créé avec succès."
            );
        }

        public void sendUpdateNotification(String to) {
            sendEmail(to, "Mise à jour du compte",
                    "Votre compte a été mis à jour.");
        }

        public void sendDeleteNotification(String to) {
            sendEmail(to, "Suppression du compte",
                    "Votre compte a été supprimé.");
        }
    }