package com.bookbridge.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.model.BookStatus;
import com.bookbridge.model.ListingType;
import com.bookbridge.model.TransactionStatus;
import com.bookbridge.model.TransactionType;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.TransactionRepository;
import com.bookbridge.repository.UserRepository;
import com.bookbridge.service.UserService;

/**
 * Admin panel endpoints (user/book moderation) under /api/admin.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpSession session) {
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // User stats
        stats.put("totalUsers", userRepository.count());
        
        // Book stats
        stats.put("totalBooks", bookRepository.count());
        stats.put("availableBooks", bookRepository.findByStatus(BookStatus.AVAILABLE).size());
        stats.put("soldBooks", bookRepository.findByStatus(BookStatus.SOLD).size());
        stats.put("exchangedBooks", bookRepository.findByStatus(BookStatus.EXCHANGED).size());
        stats.put("donatedBooks", bookRepository.findByStatus(BookStatus.DONATED).size());
        
        // Books by listing type
        stats.put("booksForSale", bookRepository.findByListingType(ListingType.SELL).size());
        stats.put("booksForExchange", bookRepository.findByListingType(ListingType.EXCHANGE).size());
        stats.put("booksForDonation", bookRepository.findByListingType(ListingType.DONATE).size());
        
        // Transaction stats
        stats.put("totalTransactions", transactionRepository.count());
        stats.put("pendingTransactions", transactionRepository.findByStatus(TransactionStatus.PENDING).size());
        stats.put("completedTransactions", transactionRepository.findByStatus(TransactionStatus.COMPLETED).size());
        
        // Transactions by type
        stats.put("saleTransactions", transactionRepository.findByType(TransactionType.SALE).size());
        stats.put("exchangeTransactions", transactionRepository.findByType(TransactionType.EXCHANGE).size());
        stats.put("donationTransactions", transactionRepository.findByType(TransactionType.DONATION).size());
        
        return ResponseEntity.ok(stats);
    }
}
