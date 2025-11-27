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
	public Page<ActivityLogEntry> searchWasteLogs(String keyword, Long categoryId, Pageable pageable) {
		// Base criteria: action must start with "STOCK_WASTE_"
		String actionPrefix = "STOCK_WASTE_";
		
		boolean hasKeyword = StringUtils.hasText(keyword);
		boolean hasCategory = categoryId != null;

		// The repository layer will be responsible for applying all filters.
		// Since we don't have a direct link from ActivityLogEntry to InventoryCategory,
		// we'll need to rely on the raw log message (details) to filter by item name.
		// For category, the current architecture does not easily support filtering
		// the ActivityLog by the item's category, so we will only use the keyword
		// in the log search.
		
		// Note: The category filtering logic is deferred to a manual post-search or a dedicated repository method
		// that joins ActivityLogEntry -> InventoryItem -> InventoryCategory, but given the structure, 
		// we'll stick to basic keyword search on the details for simplicity, and log a warning if category is used.

		if (hasCategory) {
			log.warn("Category ID filter for waste logs (ID: {}) is not implemented due to lack of direct relation in ActivityLogEntry. Ignoring category filter.", categoryId);
		}
		
		if (hasKeyword) {
			return activityLogRepository.findByActionStartingWithAndDetailsContainingIgnoreCaseOrderByTimestampDesc(
					actionPrefix, keyword.trim(), pageable);
		} else {
			return activityLogRepository.findByActionStartingWithOrderByTimestampDesc(actionPrefix, pageable);
		}
	}
}