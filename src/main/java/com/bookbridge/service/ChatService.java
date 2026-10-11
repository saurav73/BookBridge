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
    
    public List<Chat> getChatsByUser(User user) {
        return chatRepository.findByUserOrderByLastMessageAtDesc(user);
    }
    
    public Chat getChatById(Long id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found with id: " + id));
    }
    
    public Optional<Chat> findChatByUsers(User user1, User user2) {
        return chatRepository.findByUsers(user1, user2);
    }
    
    public Optional<Chat> findChatByUsersAndBook(User user1, User user2, Book book) {
        return chatRepository.findByUsersAndBook(user1, user2, book);
    }
    
    @Transactional
    public void deleteChat(Long id, User currentUser) {
        Chat chat = getChatById(id);
        
        // Check if the current user is part of the chat
        if (!chat.getUser1().getId().equals(currentUser.getId()) && !chat.getUser2().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this chat");
        }
        
        chatRepository.delete(chat);
    }
    
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
