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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
	public void logWasteAction(String username, String action, String details, String itemName, BigDecimal quantity,
			BigDecimal costPerUnit) {

		BigDecimal totalValue = BigDecimal.ZERO;
		if (quantity != null && costPerUnit != null) {
			totalValue = quantity.multiply(costPerUnit);
		}

		try {
			ActivityLogEntry entry = new ActivityLogEntry(username, action, details, itemName, quantity, costPerUnit,
					totalValue);
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
		return searchWasteLogs(null, null, null, null, null, pageable);
	}

	// --- HELPER: Parse Date Strings to LocalDateTime ---
	private LocalDateTime[] parseDateRange(String startDate, String endDate) {
		LocalDateTime startDateTime = null;
		LocalDateTime endDateTime = null;

		try {
			if (StringUtils.hasText(startDate)) {
				startDateTime = LocalDate.parse(startDate).atStartOfDay();
			}
		} catch (Exception e) {
			log.warn("Invalid start date format: {}. Ignoring.", startDate);
		}

		try {
			if (StringUtils.hasText(endDate)) {
				endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);
			}
		} catch (Exception e) {
			log.warn("Invalid end date format: {}. Ignoring.", endDate);
		}
		return new LocalDateTime[] { startDateTime, endDateTime };
	}

	@Override
	public Page<ActivityLogEntry> searchWasteLogs(String keyword, String reasonCategory, String wasteType,
			String startDate, String endDate, Pageable pageable) {
		String itemKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		String reasonSuffix = StringUtils.hasText(reasonCategory) ? reasonCategory.trim().toUpperCase() : null;
		String typeFilter = StringUtils.hasText(wasteType) ? wasteType.trim().toUpperCase() : null;

		LocalDateTime[] dates = parseDateRange(startDate, endDate);

		return activityLogRepository.searchWasteLogs(typeFilter, reasonSuffix, itemKeyword, dates[0], dates[1],
				pageable);
	}

	// --- NEW: General Search Logic ---
	@Override
	public Page<ActivityLogEntry> searchLogs(String keyword, String startDate, String endDate, Pageable pageable) {
		String searchKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		LocalDateTime[] dates = parseDateRange(startDate, endDate);
		return activityLogRepository.searchLogs(searchKeyword, dates[0], dates[1], pageable);
	}
	// --- END NEW ---

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getWasteMetrics(String keyword, String reasonCategory, String wasteType,
			String startDate, String endDate) {
		Map<String, Object> metrics = new HashMap<>();

		String itemKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
		String reasonSuffix = StringUtils.hasText(reasonCategory) ? reasonCategory.trim().toUpperCase() : null;
		String typeFilter = StringUtils.hasText(wasteType) ? wasteType.trim().toUpperCase() : null;

		LocalDateTime[] dates = parseDateRange(startDate, endDate);
		LocalDateTime start = dates[0];
		LocalDateTime end = dates[1];

		long totalItems = activityLogRepository.countFilteredWaste(typeFilter, reasonSuffix, itemKeyword, start, end);
		BigDecimal totalWasteValue = activityLogRepository.sumFilteredWasteValue(typeFilter, reasonSuffix, itemKeyword,
				start, end);

		BigDecimal expiredValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix,
				itemKeyword, start, end, "EXPIRED");
		BigDecimal damagedValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix,
				itemKeyword, start, end, "DAMAGED");
		BigDecimal otherWasteValue = activityLogRepository.sumFilteredWasteValueByReason(typeFilter, reasonSuffix,
				itemKeyword, start, end, "WASTE");

		metrics.put("totalItems", totalItems);
		metrics.put("totalWasteValue", totalWasteValue);
		metrics.put("expiredValue", expiredValue);
		metrics.put("damagedValue", damagedValue);
		metrics.put("wasteValue", otherWasteValue);

		return metrics;
	}
}