package com.toastedsiopao.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

	/**
	 * Initializes the storage directory on application startup.
	 */
	void init();

	/**
	 * Saves a file to the storage directory.
	 *
	 * @param file The MultipartFile to save.
	 * @return The web-accessible path to the saved file (e.g.,
	 *         /img/uploads/filename.png).
	 */
	String store(MultipartFile file);

	/**
	 * Deletes a file from the storage directory.
	 *
	 * @param filename The name of the file to delete (e.g., filename.png).
	 */
	void delete(String filename);
}