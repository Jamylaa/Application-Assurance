package tn.vermeg.gestionuser.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vermeg.gestionuser.entities.User;
import tn.vermeg.gestionuser.repositories.UserRepository;
import tn.vermeg.gestionuser.services.MailService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl{

    private final UserRepository userRepository;
    private final MailService mailService;

    public UserServiceImpl(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    // GET ALL
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // GET BY ID
    public Optional<User> getUserById(String idUser) {
        return userRepository.findById(idUser);
    }

    // CREATE (profil métier uniquement)
    public User createUser(User user) {

        if (!user.estValide()) {
            throw new IllegalArgumentException("Données utilisateur invalides");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email déjà existant");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username déjà existant");
        }

        User saved = userRepository.save(user);

        // email simple (sans password)
        mailService.sendAccountCreated(saved.getEmail(), saved.getUsername());

        return saved;
    }

    // UPDATE
    public User updateUser(String idUser, User user) {

        User existing = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getUsername() != null)
            existing.setUsername(user.getUsername());

        if (user.getEmail() != null)
            existing.setEmail(user.getEmail());

        if (user.getPhone() != null)
            existing.setPhone(user.getPhone());

        if (user.getDepartement() != null)
            existing.setDepartement(user.getDepartement());

        User updated = userRepository.save(existing);

        mailService.sendUpdateNotification(updated.getEmail());

        return updated;
    }

    // DELETE
    public void deleteUser(String idUser) {

        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.deleteById(idUser);

        mailService.sendDeleteNotification(user.getEmail());
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}