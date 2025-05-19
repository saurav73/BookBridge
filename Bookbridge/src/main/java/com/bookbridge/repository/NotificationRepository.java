package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Notification;
import com.bookbridge.model.NotificationType;
import com.bookbridge.model.User;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUser(User user);
    
    List<Notification> findByUserAndReadOrderByCreatedAtDesc(User user, boolean read);
    
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndType(User user, NotificationType type);
    
    long countByUserAndRead(User user, boolean read);
}
