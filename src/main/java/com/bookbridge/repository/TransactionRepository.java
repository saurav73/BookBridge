package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Book;
import com.bookbridge.model.Transaction;
import com.bookbridge.model.TransactionStatus;
import com.bookbridge.model.TransactionType;
import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for Transaction persistence and queries.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Finds all transactions where the given user is the seller.
     */
    List<Transaction> findBySeller(User seller);
    
    /**
     * Finds all transactions where the given user is the buyer.
     */
    List<Transaction> findByBuyer(User buyer);
    
    /**
     * Finds all transactions involving the given book.
     */
    List<Transaction> findByBook(Book book);
    
    /**
     * Finds all transactions in the given status.
     */
    List<Transaction> findByStatus(TransactionStatus status);
    
    /**
     * Finds all transactions of the given type.
     */
    List<Transaction> findByType(TransactionType type);
    
    /**
     * Finds a seller's transactions in the given status.
     */
    List<Transaction> findBySellerAndStatus(User seller, TransactionStatus status);
    
    /**
     * Finds a buyer's transactions in the given status.
     */
    List<Transaction> findByBuyerAndStatus(User buyer, TransactionStatus status);
}
