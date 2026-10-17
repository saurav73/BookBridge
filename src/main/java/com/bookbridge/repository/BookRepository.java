package com.bookbridge.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookbridge.model.Book;
import com.bookbridge.model.BookCondition;
import com.bookbridge.model.BookStatus;
import com.bookbridge.model.Category;
import com.bookbridge.model.ListingType;
import com.bookbridge.model.User;

/**
 * Spring Data JPA repository for Book persistence and queries.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    /**
     * Finds all books owned by the given user.
     */
    List<Book> findByOwner(User owner);
    
    /**
     * Finds a page of the books owned by the given user.
     */
    Page<Book> findByOwner(User owner, Pageable pageable);
    
    /**
     * Finds all books in the given category.
     */
    List<Book> findByCategory(Category category);
    
    /**
     * Finds all books with the given listing type.
     */
    List<Book> findByListingType(ListingType listingType);
    
    /**
     * Finds all books with the given physical condition.
     */
    List<Book> findByCondition(BookCondition condition);
    
    /**
     * Finds all books with the given availability status.
     */
    List<Book> findByStatus(BookStatus status);
    
    /**
     * Finds all books matching the given status and listing type.
     */
    @Query("SELECT b FROM Book b WHERE b.status = :status AND b.listingType = :listingType")
    List<Book> findByStatusAndListingType(@Param("status") BookStatus status, @Param("listingType") ListingType listingType);
    
    @Query("SELECT b FROM Book b WHERE " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "b.status = 'AVAILABLE'")
    /**
     * Finds books whose title or author matches the given keyword.
     */
    List<Book> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT b FROM Book b WHERE " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(:condition IS NULL OR b.condition = :condition) AND " +
           "(:listingType IS NULL OR b.listingType = :listingType) AND " +
           "(:minPrice IS NULL OR b.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR b.price <= :maxPrice) AND " +
           "b.status = 'AVAILABLE'")
    Page<Book> advancedSearch(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("condition") BookCondition condition,
            @Param("listingType") ListingType listingType,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);
}
