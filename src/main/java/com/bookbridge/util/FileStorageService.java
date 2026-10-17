package com.bookbridge.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bookbridge.exception.FileStorageException;

/**
 * Stores uploaded book photos under the configured upload dir and resolves/deletes them.
 */
@Service
public class FileStorageService {
    
    private final Path fileStorageLocation;
    
    /**
     * Initializes the storage root under the configured upload directory, creating
     * it when it does not exist.
     *
     * @param uploadDir the upload directory, defaults to "uploads"
     */
    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }
    
    /**
     * Stores an uploaded file under the given subdirectory with a generated unique
     * name.
     *
     * @param file the uploaded file
     * @param subDirectory the subdirectory under the upload root
     * @return the stored file's path relative to the upload root
     * @throws FileStorageException if the file cannot be stored
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        try {
            // Create subdirectory if it doesn't exist
            Path subDirectoryPath = this.fileStorageLocation.resolve(subDirectory);
            if (!Files.exists(subDirectoryPath)) {
                Files.createDirectories(subDirectoryPath);
            }
            
            // Generate a unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Copy file to the target location
            Path targetLocation = subDirectoryPath.resolve(fileName);
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            
            return subDirectory + "/" + fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file. Please try again!", ex);
        }
    }
    
    /**
     * Deletes the stored file at the given relative path, if it exists.
     *
     * @param filePath the file's path relative to the upload root
     * @throws FileStorageException if the file cannot be deleted
     */
    public void deleteFile(String filePath) {
        try {
            Path targetLocation = this.fileStorageLocation.resolve(filePath);
            Files.deleteIfExists(targetLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not delete file. Please try again!", ex);
        }
    }
    
    /**
     * Resolves the absolute path of the stored file at the given relative path.
     *
     * @param filePath the file's path relative to the upload root
     * @return the absolute path of the stored file
     */
    public Path getFilePath(String filePath) {
        return this.fileStorageLocation.resolve(filePath);
    }
}
