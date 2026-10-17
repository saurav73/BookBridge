package com.bookbridge.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookbridge.exception.ResourceAlreadyExistsException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Category;
import com.bookbridge.repository.CategoryRepository;

@Service
/**
 * Business logic for book categories used to organize listings.
 */
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * Returns all book categories.
     *
     * @return all categories
     */
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    /**
     * Finds a category by id, throwing when it does not exist.
     *
     * @param id the category id
     * @return the matching category
     * @throws ResourceNotFoundException if no category has the given id
     */
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
    
    /**
     * Finds a category by name, throwing when it does not exist.
     *
     * @param name the category name
     * @return the matching category
     * @throws ResourceNotFoundException if no category has the given name
     */
    public Category getCategoryByName(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
    }
    
    /**
     * Creates a category, rejecting duplicate names.
     *
     * @param category the category to create
     * @return the saved category
     * @throws ResourceAlreadyExistsException if the name is already taken
     */
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new ResourceAlreadyExistsException("Category with name " + category.getName() + " already exists");
        }
        
        return categoryRepository.save(category);
    }
    
    /**
     * Updates a category's name and description, rejecting duplicate names.
     *
     * @param id the category id
     * @param categoryDetails the new field values
     * @return the updated category
     * @throws ResourceAlreadyExistsException if the new name is already taken
     */
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        
        if (categoryDetails.getName() != null && !categoryDetails.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(categoryDetails.getName())) {
                throw new ResourceAlreadyExistsException("Category with name " + categoryDetails.getName() + " already exists");
            }
            category.setName(categoryDetails.getName());
        }
        
        if (categoryDetails.getDescription() != null) {
            category.setDescription(categoryDetails.getDescription());
        }
        
        return categoryRepository.save(category);
    }
    
    /**
     * Deletes a category.
     *
     * @param id the category id
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
