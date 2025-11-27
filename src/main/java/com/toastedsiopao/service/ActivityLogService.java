package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityLogService {

	void logAdminAction(String username, String action);

	void logAdminAction(String username, String action, String details);

	Page<ActivityLogEntry> getAllLogs(Pageable pageable);

	// --- NEW ---
	Page<ActivityLogEntry> getWasteLogs(Pageable pageable);
}