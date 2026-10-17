package com.bookbridge.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Book;
import com.bookbridge.model.Chat;
import com.bookbridge.model.User;
import com.bookbridge.repository.ChatRepository;

@Service
/**
 * Business logic for real-time buyer-seller chat conversations.
 */
public class ChatService {
    
    @Autowired
    private ChatRepository chatRepository;
    
    /**
     * Returns all chats involving the given user, most recently active first.
     *
     * @param user the user whose chats to list
     * @return the user's chats ordered by last message time descending
     */
    public List<Chat> getChatsByUser(User user) {
        return chatRepository.findByUserOrderByLastMessageAtDesc(user);
    }
    
    /**
     * Finds a chat by id, throwing when it does not exist.
     *
     * @param id the chat id
     * @return the matching chat
     * @throws ResourceNotFoundException if no chat has the given id
     */
    public Chat getChatById(Long id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + id));
    }
    
    /**
     * Finds the direct chat between two users, if one exists.
     *
     * @param user1 the first participant
     * @param user2 the second participant
     * @return the chat, or empty when the users have never chatted
     */
    public Optional<Chat> findChatByUsers(User user1, User user2) {
        return chatRepository.findByUsers(user1, user2);
    }
    
    /**
     * Finds the chat between two users about a specific book, if one exists.
     *
     * @param user1 the first participant
     * @param user2 the second participant
     * @param book the book the conversation is about
     * @return the chat, or empty when none exists for this combination
     */
    public Optional<Chat> findChatByUsersAndBook(User user1, User user2, Book book) {
        return chatRepository.findByUsersAndBook(user1, user2, book);
    }
    
    /**
     * Deletes a chat, but only when the current user is one of its participants.
     *
     * @param id the chat id
     * @param currentUser the user requesting the deletion
     * @throws IllegalStateException if the user is not a participant of the chat
     */
    @Transactional
    public void deleteChat(Long id, User currentUser) {
        Chat chat = getChatById(id);
        
        // Check if the current user is part of the chat
        if (!chat.getUser1().getId().equals(currentUser.getId()) && !chat.getUser2().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this chat");
        }
        
        chatRepository.delete(chat);
    }
    
    /**
     * Persists a chat after validating that it has both participants.
     *
     * @param chat the chat to save
     * @return the saved chat
     * @throws IllegalArgumentException if the chat or either participant is null
     */
    @Transactional
    public Chat saveChat(Chat chat) {
        if (chat == null) {
            throw new IllegalArgumentException("Chat must not be null");
        }
        if (chat.getUser1() == null || chat.getUser2() == null) {
            throw new IllegalArgumentException("Chat must have both user1 and user2");
        }
        return chatRepository.save(chat);
    }
}
