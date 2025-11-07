package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ActivityLogService {

	void logAdminAction(String username, String action);

	void logAdminAction(String username, String action, String details);

	Page<ActivityLogEntry> getAllLogs(Pageable pageable);
}