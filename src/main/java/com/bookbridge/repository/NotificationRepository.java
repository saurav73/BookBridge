package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Notification;
import com.bookbridge.model.NotificationType;
import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for Notification persistence and queries.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Finds all notifications for the given user.
     */
    List<Notification> findByUser(User user);
    
    /**
     * Finds a user's notifications by read flag, newest first.
     */
    List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, boolean read);
    
    /**
     * Finds a user's notifications newest first.
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Finds a user's notifications of the given type.
     */
    List<Notification> findByUserAndType(User user, NotificationType type);
    
    /**
     * Counts a user's notifications by read flag.
     */
    long countByUserAndRead(User user, boolean read);
}
