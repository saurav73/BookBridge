package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Book;
import com.bookbridge.model.Transaction;
import com.bookbridge.model.TransactionStatus;
import com.bookbridge.model.TransactionType;
import com.bookbridge.model.User;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findBySeller(User seller);
    
    List<Transaction> findByBuyer(User buyer);
    
    List<Transaction> findByBook(Book book);
    
    List<Transaction> findByStatus(TransactionStatus status);
    
    List<Transaction> findByType(TransactionType type);
    
    List<Transaction> findBySellerAndStatus(User seller, TransactionStatus status);
    
    List<Transaction> findByBuyerAndStatus(User buyer, TransactionStatus status);
}
