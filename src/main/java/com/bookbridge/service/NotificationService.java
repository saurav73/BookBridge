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
/**
 * Business logic for user notifications, including unread tracking.
 */
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Returns all notifications for a user, newest first.
     *
     * @param user the user whose notifications to list
     * @return the user's notifications ordered by creation time descending
     */
    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Returns the unread notifications for a user, newest first.
     *
     * @param user the user whose unread notifications to list
     * @return the user's unread notifications ordered by creation time descending
     */
    public List<Notification> getUnreadNotificationsByUser(User user) {
        return notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
    }
    
    /**
     * Finds a notification by id, throwing when it does not exist.
     *
     * @param id the notification id
     * @return the matching notification
     * @throws ResourceNotFoundException if no notification has the given id
     */
    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }
    
    /**
     * Counts the unread notifications of the given user.
     *
     * @param user the user to count for
     * @return the number of unread notifications
     */
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByUserAndRead(user, false);
    }
    
    /**
     * Persists a notification as unread and pushes it to the user over WebSocket.
     *
     * @param notification the notification to create
     * @return the saved notification
     */
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
    
    /**
     * Marks a single notification as read, allowed only for its recipient.
     *
     * @param id the notification id
     * @param currentUser the user requesting the change
     * @return the updated notification, or unchanged if already read
     * @throws IllegalStateException if the user is not the recipient
     */
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
    
    /**
     * Marks every unread notification of the given user as read.
     *
     * @param user the user whose notifications become read
     */
    @Transactional
    public void markAllNotificationsAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository.findByUserAndReadOrderByCreatedAtDesc(user, false);
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
    
    /**
     * Deletes a notification, allowed only for its recipient.
     *
     * @param id the notification id
     * @param currentUser the user requesting the deletion
     * @throws IllegalStateException if the user is not the recipient
     */
    @Transactional
    public void deleteNotification(Long id, User currentUser) {
        Notification notification = getNotificationById(id);
        
        // Check if the current user is the recipient of the notification
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }
    
    /**
     * Creates a system notification for a user without custom payload details.
     *
     * @param user the user to notify
     * @param message the notification text
     * @param link the in-app link the notification points to
     */
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
