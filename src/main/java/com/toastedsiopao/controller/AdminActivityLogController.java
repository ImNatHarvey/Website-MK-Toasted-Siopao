package com.toastedsiopao.controller;

import com.toastedsiopao.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle the Admin Activity Log page.
 */
@Controller
@RequestMapping("/admin")
public class AdminActivityLogController {

	private static final Logger log = LoggerFactory.getLogger(AdminActivityLogController.class);

	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		log.info("Fetching activity log");
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log"; // Renders activity-log.html
	}
}