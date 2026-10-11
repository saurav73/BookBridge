package com.bookbridge.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.exception.UserAlreadyExistsException;
import com.bookbridge.model.User;
import com.bookbridge.model.UserRole;
import com.bookbridge.repository.UserRepository;
import com.bookbridge.util.FileStorageService;
import com.bookbridge.util.PasswordEncoder;

@Service
/**
 * Business logic for user registration, authentication, and session handling.
 */
public class UserService {
    
    private static final String USER_SESSION_KEY = "currentUser";
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Transactional
    public User registerUser(User user, MultipartFile idCardImage, MultipartFile panDocumentImage) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        
        // Hash the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Set default role
        user.setRole(UserRole.USER);
        
        // Set creation time
        user.setCreatedAt(LocalDateTime.now());
        
        // Store ID card image if provided
        if (idCardImage != null && !idCardImage.isEmpty()) {
            String idCardPath = fileStorageService.storeFile(idCardImage, "id_cards");
            user.setIdCardImagePath(idCardPath);
        }
        
        // Store PAN document image if provided
        if (panDocumentImage != null && !panDocumentImage.isEmpty()) {
            String panDocPath = fileStorageService.storeFile(panDocumentImage, "pan_documents");
            user.setPanDocumentImagePath(panDocPath);
        }
        
        return userRepository.save(user);
    }
    
    public User authenticateUser(String username, String password, HttpSession session) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            if (passwordEncoder.matches(password, user.getPassword())) {
                // Update last login time
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                
                // Store user in session
                session.setAttribute(USER_SESSION_KEY, user.getId());
                
                return user;
            }
        }
        
        return null;
    }
    
    public void logout(HttpSession session) {
        session.removeAttribute(USER_SESSION_KEY);
        session.invalidate();
    }
    
    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElse(null);
        }
        
        return null;
    }
    
    public boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(USER_SESSION_KEY) != null;
    }
    
    public boolean isAdmin(HttpSession session) {
        User currentUser = getCurrentUser(session);
        return currentUser != null && UserRole.ADMIN.equals(currentUser.getRole());
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional
    public User updateUser(Long id, User userDetails, MultipartFile idCardImage, MultipartFile panDocumentImage) {
        User user = getUserById(id);
        
        if (userDetails.getUsername() != null && !userDetails.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(userDetails.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            user.setUsername(userDetails.getUsername());
        }
        
        if (userDetails.getEmail() != null && !userDetails.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            user.setEmail(userDetails.getEmail());
        }
        
        if (userDetails.getFullName() != null) {
            user.setFullName(userDetails.getFullName());
        }
        
        // Update password if provided
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        
        // Update ID card image if provided
        if (idCardImage != null && !idCardImage.isEmpty()) {
            // Delete old image if exists
            if (user.getIdCardImagePath() != null) {
                fileStorageService.deleteFile(user.getIdCardImagePath());
            }
            
            String idCardPath = fileStorageService.storeFile(idCardImage, "id_cards");
            user.setIdCardImagePath(idCardPath);
        }
        
        // Update PAN document image if provided
        if (panDocumentImage != null && !panDocumentImage.isEmpty()) {
            // Delete old image if exists
            if (user.getPanDocumentImagePath() != null) {
                fileStorageService.deleteFile(user.getPanDocumentImagePath());
            }
            
            String panDocPath = fileStorageService.storeFile(panDocumentImage, "pan_documents");
            user.setPanDocumentImagePath(panDocPath);
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        
        // Delete user images if they exist
        if (user.getIdCardImagePath() != null) {
            fileStorageService.deleteFile(user.getIdCardImagePath());
        }
        
        if (user.getPanDocumentImagePath() != null) {
            fileStorageService.deleteFile(user.getPanDocumentImagePath());
        }
        
        userRepository.delete(user);
    }
    
    @Transactional
    public User setUserRole(Long id, UserRole role) {
        User user = getUserById(id);
        user.setRole(role);
        return userRepository.save(user);
    }
}
