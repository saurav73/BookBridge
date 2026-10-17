package com.bookbridge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Category;

/**
 * Spring Data JPA repository for Category persistence and queries.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Finds a category by name.
     */
    Optional<Category> findByName(String name);
    
    /**
     * Reports whether a category with the given name exists.
     */
    boolean existsByName(String name);
}
