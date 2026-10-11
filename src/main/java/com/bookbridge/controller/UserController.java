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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bookbridge.model.User;
import com.bookbridge.model.UserRole;
import com.bookbridge.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        List<Map<String, Object>> users = userService.getAllUsers().stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("fullName", user.getFullName());
                    userMap.put("role", user.getRole());
                    userMap.put("createdAt", user.getCreatedAt());
                    userMap.put("lastLogin", user.getLastLogin());
                    return userMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, HttpSession session) {
        try {
            User user = userService.getUserById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole());
            response.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "idCardImage", required = false) MultipartFile idCardImage,
            @RequestParam(value = "panDocumentImage", required = false) MultipartFile panDocumentImage,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Only allow users to update their own profile, unless they are an admin
        if (!currentUser.getId().equals(id) && !UserRole.ADMIN.equals(currentUser.getRole())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            User userDetails = new User();
            userDetails.setUsername(username);
            userDetails.setEmail(email);
            userDetails.setFullName(fullName);
            userDetails.setPassword(password);
            
            User updatedUser = userService.updateUser(id, userDetails, idCardImage, panDocumentImage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("fullName", updatedUser.getFullName());
            response.put("message", "User updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Only allow users to delete their own account, unless they are an admin
        if (!currentUser.getId().equals(id) && !UserRole.ADMIN.equals(currentUser.getRole())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            userService.deleteUser(id);
            
            // If user deleted their own account, invalidate the session
            if (currentUser.getId().equals(id)) {
                userService.logout(session);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestParam("role") UserRole role,
            HttpSession session) {
        
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        try {
            User updatedUser = userService.setUserRole(id, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("username", updatedUser.getUsername());
            response.put("role", updatedUser.getRole());
            response.put("message", "User role updated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
