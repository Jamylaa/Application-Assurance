package tn.vermeg.gestionuser.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.vermeg.gestionuser.entities.User;
import tn.vermeg.gestionuser.services.impl.UserServiceImpl;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserServiceImpl userService;

    public UserController(UserServiceImpl userService) {
        this.userService = userService;
    }

 //   @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

 //   @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{idUser}")
    public ResponseEntity<User> getUserById(@PathVariable String idUser) {

        Optional<User> user = userService.getUserById(idUser);

        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

  //  @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(userService.createUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

  //  @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{idUser}")
    public ResponseEntity<User> updateUser(@PathVariable String idUser,
                                           @RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.updateUser(idUser, user));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

  //  @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{idUser}")
    public ResponseEntity<Void> deleteUser(@PathVariable String idUser) {
        try {
            userService.deleteUser(idUser);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

  //  @PreAuthorize("isAuthenticated()")
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}