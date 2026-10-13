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
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.model.Notification;
import com.bookbridge.model.User;
import com.bookbridge.service.NotificationService;
import com.bookbridge.service.UserService;

/**
 * In-app notification endpoints under /api/notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public ResponseEntity<?> getMyNotifications(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        List<Map<String, Object>> notifications = notificationService.getNotificationsByUser(currentUser).stream()
                .map(notification -> {
                    Map<String, Object> notificationMap = new HashMap<>();
                    notificationMap.put("id", notification.getId());
                    notificationMap.put("message", notification.getMessage());
                    notificationMap.put("type", notification.getType());
                    notificationMap.put("link", notification.getLink());
                    notificationMap.put("read", notification.isRead());
                    notificationMap.put("createdAt", notification.getCreatedAt());
                    notificationMap.put("readAt", notification.getReadAt());
                    return notificationMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        List<Map<String, Object>> notifications = notificationService.getUnreadNotificationsByUser(currentUser).stream()
                .map(notification -> {
                    Map<String, Object> notificationMap = new HashMap<>();
                    notificationMap.put("id", notification.getId());
                    notificationMap.put("message", notification.getMessage());
                    notificationMap.put("type", notification.getType());
                    notificationMap.put("link", notification.getLink());
                    notificationMap.put("createdAt", notification.getCreatedAt());
                    return notificationMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notifications);
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadNotificationCount(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        long unreadCount = notificationService.countUnreadNotifications(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Notification notification = notificationService.markNotificationAsRead(id, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", notification.getId());
            response.put("message", notification.getMessage());
            response.put("type", notification.getType());
            response.put("link", notification.getLink());
            response.put("read", notification.isRead());
            response.put("createdAt", notification.getCreatedAt());
            response.put("readAt", notification.getReadAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllNotificationsAsRead(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        notificationService.markAllNotificationsAsRead(currentUser);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            notificationService.deleteNotification(id, currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
