package com.toastedsiopao.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource; // --- ADDED ---
import org.springframework.core.io.UrlResource; // --- ADDED ---
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException; // --- ADDED ---
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

		String fileExtension = "";
		try {
			fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
		} catch (Exception e) {
			log.warn("Could not determine file extension for: {}", originalFilename);
		}
		String newFilename = UUID.randomUUID().toString() + fileExtension;

		try (InputStream inputStream = file.getInputStream()) {
			Path destinationFile = this.rootLocation.resolve(newFilename);

			Path absoluteDestination = destinationFile.normalize().toAbsolutePath();
			Path absoluteRoot = this.rootLocation.normalize().toAbsolutePath();

			if (!absoluteDestination.startsWith(absoluteRoot)) {
				throw new IllegalArgumentException("Cannot store file outside current directory.");
			}
			
			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
			log.info("Stored file: {}", newFilename);

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

	// === NEW METHOD IMPLEMENTATION ===
	@Override
	public Resource loadAsResource(String filename) {
		if (!StringUtils.hasText(filename)) {
			return null;
		}
		try {
			// Get only the filename (e.g., "image.png" from "/img/uploads/image.png")
			String actualFilename = Paths.get(filename).getFileName().toString();
			Path file = rootLocation.resolve(actualFilename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				log.warn("Could not read file: {}", filename);
				return null;
			}
		} catch (MalformedURLException e) {
			log.error("Could not create URL for file: {}", filename, e);
			return null;
		}
	}
}