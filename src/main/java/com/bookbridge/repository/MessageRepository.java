package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Chat;
import com.bookbridge.model.Message;
import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for Message persistence and queries.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Finds all messages sent from one user to another.
     */
    List<Message> findBySenderAndReceiver(User sender, User receiver);
    
    /**
     * Finds all messages in the given chat.
     */
    List<Message> findByChat(Chat chat);
    
    /**
     * Finds a chat's messages oldest first.
     */
    @Query("SELECT m FROM Message m WHERE m.chat = :chat ORDER BY m.sentAt ASC")
    List<Message> findByChatOrderBySentAtAsc(@Param("chat") Chat chat);
    
    /**
     * Counts the unread messages received by the given user.
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.readAt IS NULL")
    long countUnreadMessages(@Param("user") User user);
}
