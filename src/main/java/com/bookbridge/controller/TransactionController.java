package com.bookbridge.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookbridge.model.Book;
import com.bookbridge.model.DeliveryMethod;
import com.bookbridge.model.Transaction;
import com.bookbridge.model.TransactionStatus;
import com.bookbridge.model.TransactionType;
import com.bookbridge.model.User;
import com.bookbridge.service.BookService;
import com.bookbridge.service.TransactionService;
import com.bookbridge.service.UserService;

/**
 * Buy / sell / exchange transaction flow endpoints under /api/transactions.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    /**
     * Lists all transactions; requires the ADMIN role.
     */
    @GetMapping
    public ResponseEntity<?> getAllTransactions(HttpSession session) {
        if (!userService.isAdmin(session)) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Unauthorized access");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        List<Map<String, Object>> transactions = transactionService.getAllTransactions().stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Returns a single transaction; participants and admins only.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransactionById(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Transaction transaction = transactionService.getTransactionById(id);
            
            // Check if the current user is the seller or buyer of the transaction
            if (!transaction.getSeller().getId().equals(currentUser.getId()) && 
                !transaction.getBuyer().getId().equals(currentUser.getId()) &&
                !userService.isAdmin(session)) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "You are not authorized to view this transaction");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            return ResponseEntity.ok(convertToTransactionResponse(transaction));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Lists the logged-in user's sales transactions.
     */
    @GetMapping("/my-sales")
    public ResponseEntity<?> getMySales(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        List<Map<String, Object>> transactions = transactionService.getTransactionsBySeller(currentUser).stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Lists the logged-in user's purchase transactions.
     */
    @GetMapping("/my-purchases")
    public ResponseEntity<?> getMyPurchases(HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        List<Map<String, Object>> transactions = transactionService.getTransactionsByBuyer(currentUser).stream()
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * Creates a buy, exchange, or donation request for a book.
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(
            @RequestParam("bookId") Long bookId,
            @RequestParam("type") TransactionType type,
            @RequestParam(value = "amount", required = false) Double amount,
            @RequestParam(value = "deliveryMethod", required = false) DeliveryMethod deliveryMethod,
            @RequestParam(value = "deliveryAddress", required = false) String deliveryAddress,
            @RequestParam(value = "exchangeBookId", required = false) Long exchangeBookId,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Book book = bookService.getBookById(bookId);
            
            // Check if the current user is not the owner of the book
            if (book.getOwner().getId().equals(currentUser.getId())) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "You cannot create a transaction for your own book");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Transaction transaction = new Transaction();
            transaction.setBook(book);
            transaction.setSeller(book.getOwner());
            transaction.setBuyer(currentUser);
            transaction.setType(type);
            transaction.setAmount(amount);
            transaction.setDeliveryMethod(deliveryMethod);
            transaction.setDeliveryAddress(deliveryAddress);
            
            if (exchangeBookId != null) {
                Book exchangeBook = bookService.getBookById(exchangeBookId);
                
                // Check if the current user is the owner of the exchange book
                if (!exchangeBook.getOwner().getId().equals(currentUser.getId())) {
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "You can only offer your own books for exchange");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
                
                transaction.setExchangeBook(exchangeBook);
            }
            
            Transaction createdTransaction = transactionService.createTransaction(transaction);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToTransactionResponse(createdTransaction));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Accepts, rejects, completes, or cancels a transaction; the seller only.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTransactionStatus(
            @PathVariable Long id,
            @RequestParam("status") TransactionStatus status,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Transaction updatedTransaction = transactionService.updateTransactionStatus(id, status, currentUser);
            
            return ResponseEntity.ok(convertToTransactionResponse(updatedTransaction));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Deletes a transaction; the seller or buyer only.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            transactionService.deleteTransaction(id, currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Transaction deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    private Map<String, Object> convertToTransactionResponse(Transaction transaction) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", transaction.getId());
        
        Map<String, Object> book = new HashMap<>();
        book.put("id", transaction.getBook().getId());
        book.put("title", transaction.getBook().getTitle());
        book.put("author", transaction.getBook().getAuthor());
        book.put("imagePath", transaction.getBook().getImagePath());
        response.put("book", book);
        
        Map<String, Object> seller = new HashMap<>();
        seller.put("id", transaction.getSeller().getId());
        seller.put("username", transaction.getSeller().getUsername());
        response.put("seller", seller);
        
        Map<String, Object> buyer = new HashMap<>();
        buyer.put("id", transaction.getBuyer().getId());
        buyer.put("username", transaction.getBuyer().getUsername());
        response.put("buyer", buyer);
        
        response.put("type", transaction.getType());
        response.put("status", transaction.getStatus());
        response.put("amount", transaction.getAmount());
        response.put("createdAt", transaction.getCreatedAt());
        response.put("completedAt", transaction.getCompletedAt());
        response.put("deliveryMethod", transaction.getDeliveryMethod());
        response.put("deliveryAddress", transaction.getDeliveryAddress());
        
        if (transaction.getExchangeBook() != null) {
            Map<String, Object> exchangeBook = new HashMap<>();
            exchangeBook.put("id", transaction.getExchangeBook().getId());
            exchangeBook.put("title", transaction.getExchangeBook().getTitle());
            exchangeBook.put("author", transaction.getExchangeBook().getAuthor());
            exchangeBook.put("imagePath", transaction.getExchangeBook().getImagePath());
            response.put("exchangeBook", exchangeBook);
        }
        
        return response;
    }
}
