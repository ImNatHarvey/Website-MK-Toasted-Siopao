package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import com.toastedsiopao.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Service
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

	private static final Logger log = LoggerFactory.getLogger(ActivityLogServiceImpl.class);

	@Autowired
	private ActivityLogRepository activityLogRepository;

	@Override
	@Transactional 
	public void logAdminAction(String username, String action) {
		logAdminAction(username, action, null);
	}

	@Override
	@Transactional 
	public void logAdminAction(String username, String action, String details) {
		if (username == null || username.isBlank() || action == null || action.isBlank()) {
			log.warn("Attempted to log an invalid admin action (null/blank username or action)");
			return; 
		}
		try {
			ActivityLogEntry entry = new ActivityLogEntry(username, action, details);
			activityLogRepository.save(entry);
			log.info("Logged admin action: User='{}', Action='{}'", username, action);
		} catch (Exception e) {
			log.error("Failed to save activity log entry for user '{}', action '{}': {}", username, action,
					e.getMessage());
		}
	}

	@Override
	public Page<ActivityLogEntry> getAllLogs(Pageable pageable) {
		return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
	}

	@Override
	public Page<ActivityLogEntry> getWasteLogs(Pageable pageable) {
		// Fetch logs starting with "STOCK_WASTE_" to identify damaged/expired items
		return activityLogRepository.findByActionStartingWithOrderByTimestampDesc("STOCK_WASTE_", pageable);
	}
	
	@Override
	public Page<ActivityLogEntry> searchWasteLogs(String keyword, String reasonCategory, Pageable pageable) {
		// Base criteria: action must start with "STOCK_WASTE_"
		String actionPrefix = "STOCK_WASTE_";
		
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = StringUtils.hasText(reasonCategory); // --- MODIFIED to check String ---

		String fullActionFilter = actionPrefix;
		if (hasCategory) {
			// Prepend the specific category to the prefix for an exact match on action: "STOCK_WASTE_EXPIRED"
			fullActionFilter += reasonCategory.trim().toUpperCase();
		}
		
		if (hasKeyword) {
			// If filtering by category, use the fullActionFilter. Otherwise, use the base prefix.
			String finalActionPrefix = hasCategory ? fullActionFilter : actionPrefix;
			return activityLogRepository.findByActionStartingWithAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
					finalActionPrefix, keyword.trim(), pageable);
		} else {
			// If only filtering by category, use the exact action name. Otherwise, use the base prefix.
			String finalActionPrefix = hasCategory ? fullActionFilter : actionPrefix;
			return activityLogRepository.findByActionStartingWithOrderByTimestampDesc(finalActionPrefix, pageable);
		}
	}
}