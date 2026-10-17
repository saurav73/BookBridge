package com.bookbridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Book;
import com.bookbridge.model.Chat;
import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for Chat persistence and queries.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    
    /**
     * Finds the chat between two users.
     */
    @Query("SELECT c FROM Chat c WHERE (c.user1 = :user1 AND c.user2 = :user2) OR (c.user1 = :user2 AND c.user2 = :user1)")
    Optional<Chat> findByUsers(@Param("user1") User user1, @Param("user2") User user2);
    
    /**
     * Finds a user's chats ordered by most recent message first.
     */
    @Query("SELECT c FROM Chat c WHERE (c.user1 = :user OR c.user2 = :user) ORDER BY c.lastMessageAt DESC")
    List<Chat> findByUserOrderByLastMessageAtDesc(@Param("user") User user);
    
    /**
     * Finds the chat between two users about a specific book.
     */
    @Query("SELECT c FROM Chat c WHERE ((c.user1 = :user1 AND c.user2 = :user2) OR (c.user1 = :user2 AND c.user2 = :user1)) AND c.relatedBook = :book")
    Optional<Chat> findByUsersAndBook(@Param("user1") User user1, @Param("user2") User user2, @Param("book") Book book);
}
