package com.bookbridge.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Book;
import com.bookbridge.model.Chat;
import com.bookbridge.model.User;
import com.bookbridge.service.BookService;
import com.bookbridge.service.ChatService;
import com.bookbridge.service.UserService;

/**
 * Chat conversation endpoints (start/list chats) under /api/chats.
 */
@RestController
@RequestMapping("/api/chats")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @GetMapping
    public ResponseEntity<?> getMyChats(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        List<Map<String, Object>> chats = chatService.getChatsByUser(currentUser).stream()
                .map(chat -> {
                    Map<String, Object> chatMap = new HashMap<>();
                    chatMap.put("id", chat.getId());
                    
                    // Determine the other user in the chat
                    User otherUser = chat.getUser1().getId().equals(currentUser.getId()) ? chat.getUser2() : chat.getUser1();
                    
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", otherUser.getId());
                    user.put("username", otherUser.getUsername());
                    chatMap.put("otherUser", user);
                    
                    chatMap.put("createdAt", chat.getCreatedAt());
                    chatMap.put("lastMessageAt", chat.getLastMessageAt());
                    
                    if (chat.getRelatedBook() != null) {
                        Map<String, Object> book = new HashMap<>();
                        book.put("id", chat.getRelatedBook().getId());
                        book.put("title", chat.getRelatedBook().getTitle());
                        chatMap.put("relatedBook", book);
                    }
                    
                    return chatMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(chats);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getChatById(@PathVariable Long id, HttpSession session) {
        // Validate input
        if (id == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Chat ID must not be null");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Chat chat = chatService.getChatById(id);
            
            // Check if the current user is part of the chat
            if (chat.getUser1() == null || chat.getUser2() == null ||
                !chat.getUser1().getId().equals(currentUser.getId()) && !chat.getUser2().getId().equals(currentUser.getId())) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "You are not authorized to view this chat");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", chat.getId());
            
            // Determine the other user in the chat
            User otherUser = chat.getUser1().getId().equals(currentUser.getId()) ? chat.getUser2() : chat.getUser1();
            
            Map<String, Object> user = new HashMap<>();
            user.put("id", otherUser.getId());
            user.put("username", otherUser.getUsername());
            response.put("otherUser", user);
            
            response.put("createdAt", chat.getCreatedAt());
            response.put("lastMessageAt", chat.getLastMessageAt());
            
            if (chat.getRelatedBook() != null) {
                Map<String, Object> book = new HashMap<>();
                book.put("id", chat.getRelatedBook().getId());
                book.put("title", chat.getRelatedBook().getTitle());
                response.put("relatedBook", book);
            }
            
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/find-or-create")
    public ResponseEntity<?> findOrCreateChat(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "bookId", required = false) Long bookId,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            User otherUser = userService.getUserById(userId);
            Book relatedBook = null;
            
            if (bookId != null) {
                relatedBook = bookService.getBookById(bookId);
            }
            
            Optional<Chat> existingChat;
            
            if (relatedBook != null) {
                existingChat = chatService.findChatByUsersAndBook(currentUser, otherUser, relatedBook);
            } else {
                existingChat = chatService.findChatByUsers(currentUser, otherUser);
            }
            
            Chat chat;
            
            if (existingChat.isPresent()) {
                chat = existingChat.get();
            } else {
                chat = new Chat();
                chat.setUser1(currentUser);
                chat.setUser2(otherUser);
                
                if (relatedBook != null) {
                    chat.setRelatedBook(relatedBook);
                }
                
                chat = chatService.saveChat(chat);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", chat.getId());
            
            Map<String, Object> user = new HashMap<>();
            user.put("id", otherUser.getId());
            user.put("username", otherUser.getUsername());
            response.put("otherUser", user);
            
            response.put("createdAt", chat.getCreatedAt());
            response.put("lastMessageAt", chat.getLastMessageAt());
            
            if (chat.getRelatedBook() != null) {
                Map<String, Object> book = new HashMap<>();
                book.put("id", chat.getRelatedBook().getId());
                book.put("title", chat.getRelatedBook().getTitle());
                response.put("relatedBook", book);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteChat(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            chatService.deleteChat(id, currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Chat deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
