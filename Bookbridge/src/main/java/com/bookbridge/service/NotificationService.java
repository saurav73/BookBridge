package com.bookbridge.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Notification;
import com.bookbridge.model.NotificationType;
import com.bookbridge.model.User;
import com.bookbridge.repository.NotificationRepository;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<Notification> getUnreadNotificationsByUser(User user) {
        return notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
    }
    
    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }
    
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndRead(user, false);
    }
    
    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        
        Notification savedNotification = notificationRepository.save(notification);
        
        // Send real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                notification.getUser().getUsername(),
                "/queue/notifications",
                savedNotification);
        
        return savedNotification;
    }
    
    @Transactional
    public Notification markNotificationAsRead(Long id, User currentUser) {
        Notification notification = getNotificationById(id);
        
        // Check if the current user is the recipient of the notification
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to mark this notification as read");
        }
        
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        }
        
        return notification;
    }
    
    @Transactional
    public void markAllNotificationsAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
    
    @Transactional
    public void deleteNotification(Long id, User currentUser) {
        Notification notification = getNotificationById(id);
        
        // Check if the current user is the recipient of the notification
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }
    
    @Transactional
    public void createSystemNotification(User user, String message, String link) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(NotificationType.SYSTEM);
        notification.setLink(link);
        
        createNotification(notification);
    }
}
