package tn.vermeg.gestionuser.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import tn.vermeg.gestionuser.entities.User;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        Optional<User> findByUsername(String username);

        boolean existsByUsername(String username);
    }