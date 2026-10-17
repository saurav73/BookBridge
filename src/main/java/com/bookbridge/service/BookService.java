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
    
    /**
     * Returns a paginated list of all books.
     *
     * @param pageable the pagination and sorting request
     * @return the requested page of books
     */
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }
    
    /**
     * Finds a book by id, throwing when it does not exist.
     *
     * @param id the book id
     * @return the matching book
     * @throws ResourceNotFoundException if no book has the given id
     */
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }
    
    /**
     * Returns all books owned by the given user.
     *
     * @param owner the book owner
     * @return the owner's books
     */
    public List<Book> getBooksByOwner(User owner) {
        return bookRepository.findByOwner(owner);
    }
    
    /**
     * Returns a paginated list of the books owned by the given user.
     *
     * @param owner the book owner
     * @param pageable the pagination and sorting request
     * @return the requested page of the owner's books
     */
    public Page<Book> getBooksByOwner(User owner, Pageable pageable) {
        return bookRepository.findByOwner(owner, pageable);
    }
    
    /**
     * Returns all books in the given category.
     *
     * @param category the category
     * @return matching books
     */
    public List<Book> getBooksByCategory(Category category) {
        return bookRepository.findByCategory(category);
    }
    
    /**
     * Returns all books offered under the given listing type.
     *
     * @param listingType the listing type
     * @return matching books
     */
    public List<Book> getBooksByListingType(ListingType listingType) {
        return bookRepository.findByListingType(listingType);
    }
    
    /**
     * Returns all books in the given physical condition.
     *
     * @param condition the book condition
     * @return matching books
     */
    public List<Book> getBooksByCondition(BookCondition condition) {
        return bookRepository.findByCondition(condition);
    }
    
    /**
     * Returns all books with the given availability status.
     *
     * @param status the book status
     * @return matching books
     */
    public List<Book> getBooksByStatus(BookStatus status) {
        return bookRepository.findByStatus(status);
    }
    
    /**
     * Returns all books matching the given status and listing type.
     *
     * @param status the book status
     * @param listingType the listing type
     * @return matching books
     */
    public List<Book> getBooksByStatusAndListingType(BookStatus status, ListingType listingType) {
        return bookRepository.findByStatusAndListingType(status, listingType);
    }
    
    /**
     * Searches books by a free-text keyword against title and author.
     *
     * @param keyword the search keyword
     * @return matching books
     */
    public List<Book> searchBooksByKeyword(String keyword) {
        return bookRepository.searchByKeyword(keyword);
    }
    
    /**
     * Searches books by any combination of keyword, category, condition, listing
     * type, and price range. Null filters are ignored.
     *
     * @param keyword optional free-text keyword
     * @param categoryId optional category id
     * @param condition optional book condition
     * @param listingType optional listing type
     * @param minPrice optional minimum price
     * @param maxPrice optional maximum price
     * @param pageable the pagination and sorting request
     * @return the requested page of matching books
     */
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
    
    /**
     * Creates a new book listing owned by the given user, storing an optional cover image.
     *
     * @param book the book to create
     * @param image optional cover image, may be null or empty
     * @param owner the listing owner
     * @return the saved book
     */
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
    
    /**
     * Updates a book's details, allowed only for its owner, replacing the cover
     * image only when a new file is provided.
     *
     * @param id the book id
     * @param bookDetails the new field values
     * @param image optional replacement cover image, may be null or empty
     * @param currentUser the user requesting the change; must be the owner
     * @return the updated book
     * @throws IllegalStateException if the user is not the book owner
     */
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
    
    /**
     * Deletes a book listing when the requester is its owner, removing the stored
     * cover image first.
     *
     * @param id the book id
     * @param currentUser the user requesting the deletion; must be the owner
     * @throws IllegalStateException if the user is not the book owner
     */
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
    
    /**
     * Changes a book's availability status, e.g. as transactions progress.
     *
     * @param id the book id
     * @param status the new status
     * @return the updated book
     */
    @Transactional
    public Book updateBookStatus(Long id, BookStatus status) {
        Book book = getBookById(id);
        book.setStatus(status);
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }
}
