package com.bookbridge.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Book;
import com.bookbridge.model.BookStatus;
import com.bookbridge.model.Notification;
import com.bookbridge.model.NotificationType;
import com.bookbridge.model.Transaction;
import com.bookbridge.model.TransactionStatus;
import com.bookbridge.model.TransactionType;
import com.bookbridge.model.User;
import com.bookbridge.repository.TransactionRepository;

@Service
/**
 * Business logic for book buy/sell/exchange transactions.
 */
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private NotificationService notificationService;
    
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }
    
    public List<Transaction> getTransactionsBySeller(User seller) {
        return transactionRepository.findBySeller(seller);
    }
    
    public List<Transaction> getTransactionsByBuyer(User buyer) {
        return transactionRepository.findByBuyer(buyer);
    }
    
    public List<Transaction> getTransactionsByBook(Book book) {
        return transactionRepository.findByBook(book);
    }
    
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }
    
    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type);
    }
    
    public List<Transaction> getTransactionsBySellerAndStatus(User seller, TransactionStatus status) {
        return transactionRepository.findBySellerAndStatus(seller, status);
    }
    
    public List<Transaction> getTransactionsByBuyerAndStatus(User buyer, TransactionStatus status) {
        return transactionRepository.findByBuyerAndStatus(buyer, status);
    }
    
    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.PENDING);
        
        // Update book status to PENDING
        Book book = transaction.getBook();
        bookService.updateBookStatus(book.getId(), BookStatus.PENDING);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Create notification for seller
        String notificationMessage = transaction.getBuyer().getUsername() + " has requested to ";
        
        switch (transaction.getType()) {
            case SALE:
                notificationMessage += "buy your book: " + book.getTitle();
                break;
            case EXCHANGE:
                notificationMessage += "exchange for your book: " + book.getTitle();
                break;
            case DONATION:
                notificationMessage += "receive your donated book: " + book.getTitle();
                break;
        }
        
        Notification notification = new Notification();
        notification.setUser(transaction.getSeller());
        notification.setMessage(notificationMessage);
        notification.setType(NotificationType.TRANSACTION_UPDATE);
        notification.setLink("/transactions/" + savedTransaction.getId());
        notificationService.createNotification(notification);
        
        return savedTransaction;
    }
    
    @Transactional
    public Transaction updateTransactionStatus(Long id, TransactionStatus status, User currentUser) {
        // Validate inputs
        if (id == null || status == null || currentUser == null) {
            throw new IllegalArgumentException("Transaction ID, status, and current user must not be null");
        }

        // Retrieve transaction and validate
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with ID: " + id));

        // Check if the current user is the seller
        if (transaction.getSeller() == null || !transaction.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to update this transaction");
        }

        // Validate transaction status transition (optional, add logic based on business rules)
        if (!isValidStatusTransition(transaction.getStatus(), status)) {
            throw new IllegalStateException("Invalid status transition from " + transaction.getStatus() + " to " + status);
        }

        // Update transaction status
        transaction.setStatus(status);

        // Update book status based on transaction status
        Book book = transaction.getBook();
        if (book == null) {
            throw new IllegalStateException("Associated book not found for transaction");
        }

        if (status == TransactionStatus.ACCEPTED) {
            // Keep book status as PENDING (no action needed if already PENDING)
            bookService.updateBookStatus(book.getId(), BookStatus.PENDING);
        } else if (status == TransactionStatus.REJECTED || status == TransactionStatus.CANCELLED) {
            bookService.updateBookStatus(book.getId(), BookStatus.AVAILABLE);
        } else if (status == TransactionStatus.COMPLETED) {
            // Update book status based on transaction type
            TransactionType type = transaction.getType();
            if (type == null) {
                throw new IllegalStateException("Transaction type is not set");
            }
            switch (type) {
                case SALE:
                    bookService.updateBookStatus(book.getId(), BookStatus.SOLD);
                    break;
                case EXCHANGE:
                    bookService.updateBookStatus(book.getId(), BookStatus.EXCHANGED);
                    break;
                case DONATION:
                    bookService.updateBookStatus(book.getId(), BookStatus.DONATED);
                    break;
                default:
                    throw new IllegalStateException("Unsupported transaction type: " + type);
            }
            transaction.setCompletedAt(LocalDateTime.now());
        }

        // Save updated transaction
        Transaction updatedTransaction = transactionRepository.save(transaction);

        // Create notification for buyer
        if (transaction.getBuyer() != null) {
            String notificationMessage = String.format(
                    "%s has %s your request for the book: %s",
                    transaction.getSeller().getUsername(),
                    status.toString().toLowerCase(),
                    book.getTitle()
            );

            Notification notification = new Notification();
            notification.setUser(transaction.getBuyer());
            notification.setMessage(notificationMessage);
            notification.setType(NotificationType.TRANSACTION_UPDATE);
            notification.setLink("/transactions/" + updatedTransaction.getId());
            notificationService.createNotification(notification);
        }

        return updatedTransaction;
    }
    /**
     * Validates if the transition from currentStatus to newStatus is allowed.
     * Customize this method based on your business rules.
     */
    private boolean isValidStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        // Example: Define allowed transitions
        // This is a placeholder; replace with actual business logic
        if (currentStatus == TransactionStatus.PENDING) {
            return newStatus == TransactionStatus.ACCEPTED ||
                   newStatus == TransactionStatus.REJECTED ||
                   newStatus == TransactionStatus.CANCELLED;
        } else if (currentStatus == TransactionStatus.ACCEPTED) {
            return newStatus == TransactionStatus.COMPLETED ||
                   newStatus == TransactionStatus.CANCELLED;
        }
        return false; // Default to false for invalid transitions
    }
    
    @Transactional
    public void deleteTransaction(Long id, User currentUser) {
        Transaction transaction = getTransactionById(id);
        
        // Check if the current user is the seller or buyer of the transaction
        if (!transaction.getSeller().getId().equals(currentUser.getId()) && 
            !transaction.getBuyer().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this transaction");
        }
        
        // If transaction is in PENDING status, update book status back to AVAILABLE
        if (transaction.getStatus() == TransactionStatus.PENDING) {
            Book book = transaction.getBook();
            bookService.updateBookStatus(book.getId(), BookStatus.AVAILABLE);
        }
        
        transactionRepository.delete(transaction);
    }
}
