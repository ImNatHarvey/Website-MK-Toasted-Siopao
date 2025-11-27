package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

public interface ActivityLogService {

	void logAdminAction(String username, String action);

	void logAdminAction(String username, String action, String details);
	
	void logWasteAction(String username, String action, String details, 
			String itemName, BigDecimal quantity, BigDecimal costPerUnit);

	Page<ActivityLogEntry> getAllLogs(Pageable pageable);

	Page<ActivityLogEntry> getWasteLogs(Pageable pageable);
	
	Page<ActivityLogEntry> searchWasteLogs(String keyword, String reasonCategory, String wasteType, Pageable pageable);
	
	// --- MODIFIED: Added parameters to support filtered metrics ---
	Map<String, Object> getWasteMetrics(String keyword, String reasonCategory, String wasteType);
}