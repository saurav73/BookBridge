package com.bookbridge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for User persistence and queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Finds a user by username.
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by email.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Reports whether the given username is already taken.
     */
    boolean existsByUsername(String username);
    
    /**
     * Reports whether the given email is already registered.
     */
    boolean existsByEmail(String email);
}
