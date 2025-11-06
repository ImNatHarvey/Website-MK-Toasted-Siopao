package com.toastedsiopao.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

	@Value("${file.upload-dir}")
	private String uploadDir;

	private Path rootLocation;

	@Override
	@PostConstruct
	public void init() {
		try {
			rootLocation = Paths.get(uploadDir);
			if (Files.notExists(rootLocation)) {
				Files.createDirectories(rootLocation);
				log.info("Created upload directory: {}", rootLocation);
			} else {
				log.info("Upload directory already exists: {}", rootLocation);
			}
		} catch (IOException e) {
			log.error("Could not initialize storage location", e);
			throw new RuntimeException("Could not initialize storage location", e);
		}
	}

	@Override
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Failed to store empty file.");
		}

		String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
		if (originalFilename == null || originalFilename.contains("..")) {
			throw new IllegalArgumentException("Cannot store file with relative path: " + originalFilename);
		}

		// Generate a unique filename
		String fileExtension = "";
		try {
			fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
		} catch (Exception e) {
			log.warn("Could not determine file extension for: {}", originalFilename);
		}
		String newFilename = UUID.randomUUID().toString() + fileExtension;

		try (InputStream inputStream = file.getInputStream()) {
			Path destinationFile = this.rootLocation.resolve(newFilename);

			// **** START OF FIX ****
			// The previous check was too strict.
			// This new check ensures the normalized absolute path of the file
			// starts with the normalized absolute path of the root directory.
			Path absoluteDestination = destinationFile.normalize().toAbsolutePath();
			Path absoluteRoot = this.rootLocation.normalize().toAbsolutePath();

			if (!absoluteDestination.startsWith(absoluteRoot)) {
				throw new IllegalArgumentException("Cannot store file outside current directory.");
			}
			// **** END OF FIX ****

			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			log.info("Stored file: {}", newFilename);

			// Return the web-accessible path
			return "/img/uploads/" + newFilename;

		} catch (IOException e) {
			log.error("Failed to store file: {}", newFilename, e);
			throw new RuntimeException("Failed to store file", e);
		}
	}

	@Override
	public void delete(String filename) {
		if (!StringUtils.hasText(filename)) {
			log.warn("Attempted to delete file with empty filename.");
			return;
		}

		try {
			// Extract just the filename from the path (e.g., /img/uploads/file.png ->
			// file.png)
			String actualFilename = Paths.get(filename).getFileName().toString();
			Path file = rootLocation.resolve(actualFilename);
			if (Files.exists(file)) {
				Files.delete(file);
				log.info("Deleted file: {}", actualFilename);
			} else {
				log.warn("Attempted to delete non-existent file: {}", actualFilename);
			}
		} catch (IOException e) {
			log.error("Failed to delete file: {}", filename, e);
			throw new RuntimeException("Failed to delete file", e);
		}
	}
}