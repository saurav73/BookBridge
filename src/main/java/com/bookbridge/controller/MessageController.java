package com.bookbridge.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.model.Book;
import com.bookbridge.model.Chat;
import com.bookbridge.model.Message;
import com.bookbridge.model.User;
import com.bookbridge.service.BookService;
import com.bookbridge.service.ChatService;
import com.bookbridge.service.MessageService;
import com.bookbridge.service.UserService;

/**
 * Message REST endpoints under /api/messages plus the STOMP @MessageMapping for live chat.
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<?> getMessagesByChat(@PathVariable Long chatId, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Chat chat = chatService.getChatById(chatId);
            
            // Check if the current user is part of the chat
            if (!chat.getUser1().getId().equals(currentUser.getId()) && !chat.getUser2().getId().equals(currentUser.getId())) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "You are not authorized to view this chat");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            List<Map<String, Object>> messages = messageService.getMessagesByChat(chat).stream()
                    .map(message -> convertToMessageResponse(message))
                    .collect(Collectors.toList());
            
            // Mark all messages as read
            messageService.markAllMessagesAsRead(chat, currentUser);
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestParam("receiverId") Long receiverId,
            @RequestParam("content") String content,
            @RequestParam(value = "bookId", required = false) Long bookId,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            User receiver = userService.getUserById(receiverId);
            Book relatedBook = null;
            
            if (bookId != null) {
                relatedBook = bookService.getBookById(bookId);
            }
            
            Message message = messageService.sendMessage(currentUser, receiver, content, relatedBook);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToMessageResponse(message));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @MessageMapping("/chat")
    public void processMessage(@Payload Map<String, Object> payload) {
        try {
            Long senderId = Long.valueOf(payload.get("senderId").toString());
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = payload.get("content").toString();
            Long bookId = payload.get("bookId") != null ? Long.valueOf(payload.get("bookId").toString()) : null;
            
            User sender = userService.getUserById(senderId);
            User receiver = userService.getUserById(receiverId);
            Book relatedBook = null;
            
            if (bookId != null) {
                relatedBook = bookService.getBookById(bookId);
            }
            
            Message message = messageService.sendMessage(sender, receiver, content, relatedBook);
            
            // Message is already sent to the receiver via WebSocket in the service
            // This is just for confirmation to the sender
            messagingTemplate.convertAndSendToUser(
                    sender.getUsername(),
                    "/queue/messages",
                    convertToMessageResponse(message));
        } catch (Exception e) {
            // Log the error
            System.err.println("Error processing WebSocket message: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Message message = messageService.markMessageAsRead(id, currentUser);
            
            return ResponseEntity.ok(convertToMessageResponse(message));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            messageService.deleteMessage(id, currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Message deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        long unreadCount = messageService.countUnreadMessages(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> convertToMessageResponse(Message message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        
        Map<String, Object> sender = new HashMap<>();
        sender.put("id", message.getSender().getId());
        sender.put("username", message.getSender().getUsername());
        response.put("sender", sender);
        
        Map<String, Object> receiver = new HashMap<>();
        receiver.put("id", message.getReceiver().getId());
        receiver.put("username", message.getReceiver().getUsername());
        response.put("receiver", receiver);
        
        response.put("content", message.getContent());
        response.put("sentAt", message.getSentAt());
        response.put("readAt", message.getReadAt());
        response.put("status", message.getStatus());
        
        if (message.getChat() != null) {
            response.put("chatId", message.getChat().getId());
        }
        
        if (message.getRelatedBook() != null) {
            Map<String, Object> book = new HashMap<>();
            book.put("id", message.getRelatedBook().getId());
            book.put("title", message.getRelatedBook().getTitle());
            response.put("relatedBook", book);
        }
        
        return response;
    }
}
