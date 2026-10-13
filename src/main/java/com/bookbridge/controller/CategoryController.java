package com.bookbridge.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.model.Category;
import com.bookbridge.service.CategoryService;
import com.bookbridge.service.UserService;

/**
 * Book category CRUD endpoints under /api/categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        List<Map<String, Object>> categories = categoryService.getAllCategories().stream()
                .map(category -> {
                    Map<String, Object> categoryMap = new HashMap<>();
                    categoryMap.put("id", category.getId());
                    categoryMap.put("name", category.getName());
                    categoryMap.put("description", category.getDescription());
                    return categoryMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", category.getId());
            response.put("name", category.getName());
            response.put("description", category.getDescription());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session) {
        
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            
            Category createdCategory = categoryService.createCategory(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdCategory.getId());
            response.put("name", createdCategory.getName());
            response.put("description", createdCategory.getDescription());
            response.put("message", "Category created successfully");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session) {
        
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            Category categoryDetails = new Category();
            categoryDetails.setName(name);
            categoryDetails.setDescription(description);
            
            Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedCategory.getId());
            response.put("name", updatedCategory.getName());
            response.put("description", updatedCategory.getDescription());
            response.put("message", "Category updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, HttpSession session) {
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            categoryService.deleteCategory(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Category deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
