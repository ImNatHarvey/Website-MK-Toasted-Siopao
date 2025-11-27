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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
	@Transactional
	public void logWasteAction(String username, String action, String details, 
			String itemName, BigDecimal quantity, BigDecimal costPerUnit) {
		
		BigDecimal totalValue = BigDecimal.ZERO;
		if (quantity != null && costPerUnit != null) {
			totalValue = quantity.multiply(costPerUnit);
		}
		
		try {
			ActivityLogEntry entry = new ActivityLogEntry(username, action, details, itemName, quantity, costPerUnit, totalValue);
			activityLogRepository.save(entry);
			log.info("Logged waste action: Item='{}', Value='{}'", itemName, totalValue);
		} catch (Exception e) {
			log.error("Failed to save waste log entry: {}", e.getMessage());
		}
	}

	@Override
	public Page<ActivityLogEntry> getAllLogs(Pageable pageable) {
		return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
	}

	@Override
	public Page<ActivityLogEntry> getWasteLogs(Pageable pageable) {
		// Default view showing all waste types
		return activityLogRepository.searchWasteLogs(null, null, null, pageable);
	}
	
	@Override
	public Page<ActivityLogEntry> searchWasteLogs(String keyword, String reasonCategory, String wasteType, Pageable pageable) {
		String itemKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		String reasonSuffix = StringUtils.hasText(reasonCategory) ? reasonCategory.trim().toUpperCase() : null;
		String typeFilter = StringUtils.hasText(wasteType) ? wasteType.trim().toUpperCase() : null;
		
		return activityLogRepository.searchWasteLogs(typeFilter, reasonSuffix, itemKeyword, pageable);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getWasteMetrics(String keyword, String reasonCategory, String wasteType) {
		Map<String, Object> metrics = new HashMap<>();
		
		// Parse filters to match search logic
		String itemKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		String reasonSuffix = StringUtils.hasText(reasonCategory) ? reasonCategory.trim().toUpperCase() : null;
		String typeFilter = StringUtils.hasText(wasteType) ? wasteType.trim().toUpperCase() : null;
		
		// 1. Total Items matching current filter
		long totalItems = activityLogRepository.countFilteredWaste(typeFilter, reasonSuffix, itemKeyword);
		
		// 2. Total Value matching current filter
		BigDecimal totalWasteValue = activityLogRepository.sumFilteredWasteValue(typeFilter, reasonSuffix, itemKeyword);
		
		// 3. Breakdown Values (Within the current filter context)
		// e.g., If Filter is "Inventory Only", this sums "Inventory Expired", "Inventory Damaged", etc.
		// e.g., If Filter is Reason="Damaged", Expired should be 0.
		BigDecimal expiredValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix, itemKeyword, "EXPIRED");
		BigDecimal damagedValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix, itemKeyword, "DAMAGED");
		BigDecimal otherWasteValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix, itemKeyword, "WASTE");
		
		metrics.put("totalItems", totalItems);
		metrics.put("totalWasteValue", totalWasteValue);
		metrics.put("expiredValue", expiredValue);
		metrics.put("damagedValue", damagedValue);
		metrics.put("wasteValue", otherWasteValue);
		
		return metrics;
	}
}