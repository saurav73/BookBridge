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
    
    /**
     * Returns every transaction in the system.
     *
     * @return all transactions
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
    
    /**
     * Finds a transaction by id, throwing when it does not exist.
     *
     * @param id the transaction id
     * @return the matching transaction
     * @throws ResourceNotFoundException if no transaction has the given id
     */
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }
    
    /**
     * Returns all transactions where the given user is the seller.
     *
     * @param seller the seller
     * @return the seller's transactions
     */
    public List<Transaction> getTransactionsBySeller(User seller) {
        return transactionRepository.findBySeller(seller);
    }
    
    /**
     * Returns all transactions where the given user is the buyer.
     *
     * @param buyer the buyer
     * @return the buyer's transactions
     */
    public List<Transaction> getTransactionsByBuyer(User buyer) {
        return transactionRepository.findByBuyer(buyer);
    }
    
    /**
     * Returns all transactions involving the given book.
     *
     * @param book the book
     * @return the book's transactions
     */
    public List<Transaction> getTransactionsByBook(Book book) {
        return transactionRepository.findByBook(book);
    }
    
    /**
     * Returns all transactions in the given status.
     *
     * @param status the transaction status
     * @return matching transactions
     */
    public List<Transaction> getTransactionsByStatus(TransactionStatus status) {
        return transactionRepository.findByStatus(status);
    }
    
    /**
     * Returns all transactions of the given type (sale, exchange, or donation).
     *
     * @param type the transaction type
     * @return matching transactions
     */
    public List<Transaction> getTransactionsByType(TransactionType type) {
        return transactionRepository.findByType(type);
    }
    
    /**
     * Returns the transactions of a seller that are in the given status.
     *
     * @param seller the seller
     * @param status the transaction status
     * @return matching transactions
     */
    public List<Transaction> getTransactionsBySellerAndStatus(User seller, TransactionStatus status) {
        return transactionRepository.findBySellerAndStatus(seller, status);
    }
    
    /**
     * Returns the transactions of a buyer that are in the given status.
     *
     * @param buyer the buyer
     * @param status the transaction status
     * @return matching transactions
     */
    public List<Transaction> getTransactionsByBuyerAndStatus(User buyer, TransactionStatus status) {
        return transactionRepository.findByBuyerAndStatus(buyer, status);
    }
    
    /**
     * Creates a pending transaction, marks the book as pending, and notifies the seller.
     *
     * @param transaction the transaction to create
     * @return the saved transaction
     */
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
    
    /**
     * Updates a transaction's status as its seller, validating the transition and
     * syncing the book status, then notifies the buyer.
     *
     * @param id the transaction id
     * @param status the new status
     * @param currentUser the user requesting the change; must be the seller
     * @return the updated transaction
     * @throws IllegalArgumentException if any argument is null
     * @throws IllegalStateException if the user is not the seller or the transition is invalid
     */
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
    
    /**
     * Deletes a transaction when the requester is its seller or buyer, restoring
     * the book to AVAILABLE if the transaction was still pending.
     *
     * @param id the transaction id
     * @param currentUser the user requesting the deletion
     * @throws IllegalStateException if the user is neither seller nor buyer
     */
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
