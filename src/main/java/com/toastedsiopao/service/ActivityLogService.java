package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import java.util.List;

public interface ActivityLogService {

    void logAdminAction(String username, String action);

    void logAdminAction(String username, String action, String details);

    List<ActivityLogEntry> getAllLogs();
}
