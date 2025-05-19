package com.bookbridge.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Book;
import com.bookbridge.model.Chat;
import com.bookbridge.model.Message;
import com.bookbridge.model.MessageStatus;
import com.bookbridge.model.Notification;
import com.bookbridge.model.NotificationType;
import com.bookbridge.model.User;
import com.bookbridge.repository.ChatRepository;
import com.bookbridge.repository.MessageRepository;

@Service
public class MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ChatRepository chatRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public List<Message> getMessagesByChat(Chat chat) {
        return messageRepository.findByChatOrderBySentAtAsc(chat);
    }
    
    public Message getMessageById(Long id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));
    }
    
    @Transactional
    public Message sendMessage(User sender, User receiver, String content, Book relatedBook) {
        // Find or create chat
        Chat chat;
        Optional<Chat> existingChat;
        
        if (relatedBook != null) {
            existingChat = chatRepository.findByUsersAndBook(sender, receiver, relatedBook);
        } else {
            existingChat = chatRepository.findByUsers(sender, receiver);
        }
        
        if (existingChat.isPresent()) {
            chat = existingChat.get();
        } else {
            chat = new Chat();
            chat.setUser1(sender);
            chat.setUser2(receiver);
            chat.setCreatedAt(LocalDateTime.now());
            
            if (relatedBook != null) {
                chat.setRelatedBook(relatedBook);
            }
            
            chat = chatRepository.save(chat);
        }
        
        // Create and save message
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setStatus(MessageStatus.SENT);
        message.setChat(chat);
        
        if (relatedBook != null) {
            message.setRelatedBook(relatedBook);
        }
        
        message = messageRepository.save(message);
        
        // Update chat's last message time
        chat.setLastMessageAt(LocalDateTime.now());
        chatRepository.save(chat);
        
        // Create notification for receiver
        String notificationMessage = sender.getUsername() + " sent you a message";
        Notification notification = new Notification();
        notification.setUser(receiver);
        notification.setMessage(notificationMessage);
        notification.setType(NotificationType.NEW_MESSAGE);
        notification.setLink("/chats/" + chat.getId());
        notificationService.createNotification(notification);
        
        // Send real-time notification via WebSocket
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(),
                "/queue/messages",
                message);
        
        return message;
    }
    
    @Transactional
    public Message markMessageAsRead(Long id, User currentUser) {
        Message message = getMessageById(id);
        
        // Check if the current user is the receiver of the message
        if (!message.getReceiver().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to mark this message as read");
        }
        
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
            message.setStatus(MessageStatus.READ);
            return messageRepository.save(message);
        }
        
        return message;
    }
    
    @Transactional
    public void markAllMessagesAsRead(Chat chat, User currentUser) {
        List<Message> messages = messageRepository.findByChatOrderBySentAtAsc(chat);
        
        for (Message message : messages) {
            if (message.getReceiver().getId().equals(currentUser.getId()) && message.getReadAt() == null) {
                message.setReadAt(LocalDateTime.now());
                message.setStatus(MessageStatus.READ);
                messageRepository.save(message);
            }
        }
    }
    
    public long countUnreadMessages(User user) {
        return messageRepository.countUnreadMessages(user);
    }
    
    @Transactional
    public void deleteMessage(Long id, User currentUser) {
        Message message = getMessageById(id);
        
        // Check if the current user is the sender of the message
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this message");
        }
        
        messageRepository.delete(message);
    }
}
