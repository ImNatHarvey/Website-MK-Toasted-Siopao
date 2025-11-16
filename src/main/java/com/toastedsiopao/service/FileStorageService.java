package com.toastedsiopao.service;

import org.springframework.core.io.Resource; // --- ADDED ---
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

	void init();

	String store(MultipartFile file);

	void delete(String filename);
	
	Resource loadAsResource(String filename); // --- ADDED ---
}