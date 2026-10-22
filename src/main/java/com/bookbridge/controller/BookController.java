package com.bookbridge.controller;

import java.util.HashMap;

import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.multipart.MultipartFile;

import com.bookbridge.model.Book;
import com.bookbridge.model.BookCondition;
import com.bookbridge.model.BookStatus;
import com.bookbridge.model.Category;
import com.bookbridge.model.ListingType;
import com.bookbridge.model.User;
import com.bookbridge.service.BookService;
import com.bookbridge.service.CategoryService;
import com.bookbridge.service.UserService;

/**
 * Book listing CRUD and search endpoints under /api/books.
 */
@RestController
@RequestMapping("/api/books")
public class BookController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CategoryService categoryService;
    
    /**
     * Lists all book listings with pagination and sorting.
     */
    @GetMapping
    public ResponseEntity<?> getAllBooks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Book> books = bookService.getAllBooks(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("books", books.getContent().stream()
                .map(this::convertToBookResponse)
                .collect(Collectors.toList()));
        response.put("currentPage", books.getNumber());
        response.put("totalItems", books.getTotalElements());
        response.put("totalPages", books.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Returns a single book listing by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        try {
            Book book = bookService.getBookById(id);
            return ResponseEntity.ok(convertToBookResponse(book));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Lists a user's book listings with pagination.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBooksByUser(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        try {
            User owner = userService.getUserById(userId);
            Pageable pageable = PageRequest.of(page, size);
            Page<Book> books = bookService.getBooksByOwner(owner, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("books", books.getContent().stream()
                    .map(this::convertToBookResponse)
                    .collect(Collectors.toList()));
            response.put("currentPage", books.getNumber());
            response.put("totalItems", books.getTotalElements());
            response.put("totalPages", books.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Lists the logged-in user's own book listings with pagination.
     */
    @GetMapping("/my-books")
    public ResponseEntity<?> getMyBooks(
            HttpSession session,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> books = bookService.getBooksByOwner(currentUser, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("books", books.getContent().stream()
                .map(this::convertToBookResponse)
                .collect(Collectors.toList()));
        response.put("currentPage", books.getNumber());
        response.put("totalItems", books.getTotalElements());
        response.put("totalPages", books.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Searches book listings by keyword, category, condition, listing type, and price range.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchBooks(
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "condition", required = false) BookCondition condition,
            @RequestParam(value = "listingType", required = false) ListingType listingType,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<Book> books = bookService.advancedSearch(
                keyword,
                categoryId,
                condition,
                listingType,
                minPrice,
                maxPrice,
                pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("books", books.getContent().stream()
                .map(this::convertToBookResponse)
                .collect(Collectors.toList()));
        response.put("currentPage", books.getNumber());
        response.put("totalItems", books.getTotalElements());
        response.put("totalPages", books.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Creates a new book listing for the logged-in user.
     */
    @PostMapping
    public ResponseEntity<?> createBook(
            @RequestParam("title") String title,
            @RequestParam("author") String author,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("condition") BookCondition condition,
            @RequestParam("listingType") ListingType listingType,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Category category = categoryService.getCategoryById(categoryId);
            
            Book book = new Book();
            book.setTitle(title);
            book.setAuthor(author);
            book.setCategory(category);
            book.setCondition(condition);
            book.setListingType(listingType);
            book.setPrice(price);
            book.setDescription(description);
            
            Book createdBook = bookService.createBook(book, image, currentUser);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToBookResponse(createdBook));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Updates a book listing; the book owner only.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "condition", required = false) BookCondition condition,
            @RequestParam(value = "listingType", required = false) ListingType listingType,
            @RequestParam(value = "price", required = false) Double price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Book bookDetails = new Book();
            bookDetails.setTitle(title);
            bookDetails.setAuthor(author);
            
            if (categoryId != null) {
                Category category = categoryService.getCategoryById(categoryId);
                bookDetails.setCategory(category);
            }
            
            bookDetails.setCondition(condition);
            bookDetails.setListingType(listingType);
            bookDetails.setPrice(price);
            bookDetails.setDescription(description);
            
            Book updatedBook = bookService.updateBook(id, bookDetails, image, currentUser);
            
            return ResponseEntity.ok(convertToBookResponse(updatedBook));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Deletes a book listing; the book owner only.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id, HttpSession session) {
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            bookService.deleteBook(id, currentUser);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Book deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Changes a book listing's availability status; the book owner only.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBookStatus(
            @PathVariable Long id,
            @RequestParam("status") BookStatus status,
            HttpSession session) {
        
        User currentUser = userService.getCurrentUser(session);
        
        if (currentUser == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            Book book = bookService.getBookById(id);
            
            // Check if the current user is the owner of the book
            if (!book.getOwner().getId().equals(currentUser.getId())) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "You are not authorized to update this book's status");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Book updatedBook = bookService.updateBookStatus(id, status);
            
            return ResponseEntity.ok(convertToBookResponse(updatedBook));
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    private Map<String, Object> convertToBookResponse(Book book) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", book.getId());
        response.put("title", book.getTitle());
        response.put("author", book.getAuthor());
        
        if (book.getCategory() != null) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", book.getCategory().getId());
            category.put("name", book.getCategory().getName());
            response.put("category", category);
        }
        
        response.put("condition", book.getCondition());
        response.put("listingType", book.getListingType());
        response.put("price", book.getPrice());
        response.put("description", book.getDescription());
        response.put("imagePath", book.getImagePath());
        response.put("status", book.getStatus());
        response.put("createdAt", book.getCreatedAt());
        response.put("updatedAt", book.getUpdatedAt());
        
        Map<String, Object> owner = new HashMap<>();
        owner.put("id", book.getOwner().getId());
        owner.put("username", book.getOwner().getUsername());
        response.put("owner", owner);
        
        return response;
    }
}
