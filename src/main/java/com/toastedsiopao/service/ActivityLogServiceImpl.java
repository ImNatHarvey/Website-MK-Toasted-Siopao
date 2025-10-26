package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import com.toastedsiopao.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogServiceImpl.class);

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Override
    @Transactional // Ensure the save operation is transactional
    public void logAdminAction(String username, String action) {
        logAdminAction(username, action, null);
    }

    @Override
    @Transactional // Ensure the save operation is transactional
    public void logAdminAction(String username, String action, String details) {
        if (username == null || username.isBlank() || action == null || action.isBlank()) {
            log.warn("Attempted to log an invalid admin action (null/blank username or action)");
            return; // Don't save invalid logs
        }
        try {
            ActivityLogEntry entry = new ActivityLogEntry(username, action, details);
            activityLogRepository.save(entry);
            log.info("Logged admin action: User='{}', Action='{}'", username, action);
        } catch (Exception e) {
            log.error("Failed to save activity log entry for user '{}', action '{}': {}", username, action, e.getMessage());
            // Depending on requirements, you might re-throw or handle differently
        }
    }

    @Override
    public List<ActivityLogEntry> getAllLogs() {
        // Return logs newest first
        return activityLogRepository.findAllByOrderByTimestampDesc();
    }
}
