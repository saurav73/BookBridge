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
    
    /**
     * Registers a new user: checks uniqueness, hashes the password, stores optional
     * ID and PAN documents, and assigns the default USER role.
     *
     * @param user the user to register
     * @param idCardImage optional ID card image, may be null or empty
     * @param panDocumentImage optional PAN document image, may be null or empty
     * @return the saved user
     * @throws UserAlreadyExistsException if the username or email is taken
     */
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
    
    /**
     * Authenticates a user by credentials, updates last login, and stores the user
     * id in the session on success.
     *
     * @param username the login username
     * @param password the raw password to verify
     * @param session the HTTP session to populate on success
     * @return the authenticated user, or null when credentials are invalid
     */
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
    
    /**
     * Clears the current user from the session and invalidates it.
     *
     * @param session the HTTP session to invalidate
     */
    public void logout(HttpSession session) {
        session.removeAttribute(USER_SESSION_KEY);
        session.invalidate();
    }
    
    /**
     * Resolves the logged-in user from the session.
     *
     * @param session the HTTP session holding the user id
     * @return the current user, or null when nobody is logged in
     */
    public User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(USER_SESSION_KEY);
        
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Reports whether the session holds a logged-in user.
     *
     * @param session the HTTP session to check
     * @return true when a user is logged in
     */
    public boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(USER_SESSION_KEY) != null;
    }
    
    /**
     * Reports whether the session's logged-in user has the ADMIN role.
     *
     * @param session the HTTP session to check
     * @return true when the current user is an admin
     */
    public boolean isAdmin(HttpSession session) {
        User currentUser = getCurrentUser(session);
        return currentUser != null && UserRole.ADMIN.equals(currentUser.getRole());
    }
    
    /**
     * Finds a user by id, throwing when it does not exist.
     *
     * @param id the user id
     * @return the matching user
     * @throws ResourceNotFoundException if no user has the given id
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    /**
     * Finds a user by username, throwing when it does not exist.
     *
     * @param username the username
     * @return the matching user
     * @throws ResourceNotFoundException if no user has the given username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
    
    /**
     * Returns every registered user.
     *
     * @return all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Updates a user's profile fields, password, and documents, replacing stored
     * images only when new files are provided.
     *
     * @param id the user id
     * @param userDetails the new field values
     * @param idCardImage optional replacement ID card image, may be null or empty
     * @param panDocumentImage optional replacement PAN document image, may be null or empty
     * @return the updated user
     * @throws UserAlreadyExistsException if a changed username or email is taken
     */
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
    
    /**
     * Deletes a user along with any stored ID and PAN document images.
     *
     * @param id the user id
     */
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
    
    /**
     * Assigns a role to a user.
     *
     * @param id the user id
     * @param role the role to assign
     * @return the updated user
     */
    @Transactional
    public User setUserRole(Long id, UserRole role) {
        User user = getUserById(id);
        user.setRole(role);
        return userRepository.save(user);
    }
}
