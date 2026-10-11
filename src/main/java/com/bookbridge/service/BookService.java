package com.bookbridge.service;

import java.time.LocalDateTime;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.model.Book;
import com.bookbridge.model.BookCondition;
import com.bookbridge.model.BookStatus;
import com.bookbridge.model.Category;
import com.bookbridge.model.ListingType;
import com.bookbridge.model.User;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.CategoryRepository;
import com.bookbridge.util.FileStorageService;

@Service
/**
 * Business logic for book listings: browsing, lookup, and managing the book catalog.
 */
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }
    
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }
    
    public List<Book> getBooksByOwner(User owner) {
        return bookRepository.findByOwner(owner);
    }
    
    public Page<Book> getBooksByOwner(User owner, Pageable pageable) {
        return bookRepository.findByOwner(owner, pageable);
    }
    
    public List<Book> getBooksByCategory(Category category) {
        return bookRepository.findByCategory(category);
    }
    
    public List<Book> getBooksByListingType(ListingType listingType) {
        return bookRepository.findByListingType(listingType);
    }
    
    public List<Book> getBooksByCondition(BookCondition condition) {
        return bookRepository.findByCondition(condition);
    }
    
    public List<Book> getBooksByStatus(BookStatus status) {
        return bookRepository.findByStatus(status);
    }
    
    public List<Book> getBooksByStatusAndListingType(BookStatus status, ListingType listingType) {
        return bookRepository.findByStatusAndListingType(status, listingType);
    }
    
    public List<Book> searchBooksByKeyword(String keyword) {
        return bookRepository.searchByKeyword(keyword);
    }
    
    public Page<Book> advancedSearch(
            String keyword,
            Long categoryId,
            BookCondition condition,
            ListingType listingType,
            Double minPrice,
            Double maxPrice,
            Pageable pageable) {
        
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElse(null);
        }
        
        return bookRepository.advancedSearch(
                keyword,
                category,
                condition,
                listingType,
                minPrice,
                maxPrice,
                pageable);
    }
    
    @Transactional
    public Book createBook(Book book, MultipartFile image, User owner) {
        book.setOwner(owner);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        book.setStatus(BookStatus.AVAILABLE);
        
        // Store book image if provided
        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.storeFile(image, "book_images");
            book.setImagePath(imagePath);
        }
        
        return bookRepository.save(book);
    }
    
    @Transactional
    public Book updateBook(Long id, Book bookDetails, MultipartFile image, User currentUser) {
        Book book = getBookById(id);
        
        // Check if the current user is the owner of the book
        if (!book.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to update this book");
        }
        
        if (bookDetails.getTitle() != null) {
            book.setTitle(bookDetails.getTitle());
        }
        
        if (bookDetails.getAuthor() != null) {
            book.setAuthor(bookDetails.getAuthor());
        }
        
        if (bookDetails.getCategory() != null) {
            book.setCategory(bookDetails.getCategory());
        }
        
        if (bookDetails.getCondition() != null) {
            book.setCondition(bookDetails.getCondition());
        }
        
        if (bookDetails.getListingType() != null) {
            book.setListingType(bookDetails.getListingType());
        }
        
        if (bookDetails.getPrice() != null) {
            book.setPrice(bookDetails.getPrice());
        }
        
        if (bookDetails.getDescription() != null) {
            book.setDescription(bookDetails.getDescription());
        }
        
        // Update book image if provided
        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (book.getImagePath() != null) {
                fileStorageService.deleteFile(book.getImagePath());
            }
            
            String imagePath = fileStorageService.storeFile(image, "book_images");
            book.setImagePath(imagePath);
        }
        
        book.setUpdatedAt(LocalDateTime.now());
        
        return bookRepository.save(book);
    }
    
    @Transactional
    public void deleteBook(Long id, User currentUser) {
        Book book = getBookById(id);
        
        // Check if the current user is the owner of the book
        if (!book.getOwner().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this book");
        }
        
        // Delete book image if exists
        if (book.getImagePath() != null) {
            fileStorageService.deleteFile(book.getImagePath());
        }
        
        bookRepository.delete(book);
    }
    
    @Transactional
    public Book updateBookStatus(Long id, BookStatus status) {
        Book book = getBookById(id);
        book.setStatus(status);
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }
}
