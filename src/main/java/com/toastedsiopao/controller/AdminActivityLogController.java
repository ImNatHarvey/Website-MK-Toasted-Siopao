package com.toastedsiopao.controller;

import com.toastedsiopao.model.ActivityLogEntry;
import com.toastedsiopao.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminActivityLogController {

	private static final Logger log = LoggerFactory.getLogger(AdminActivityLogController.class);

	@Autowired
	private ActivityLogService activityLogService;

	@GetMapping("/activity-log")
	@PreAuthorize("hasAuthority('VIEW_ACTIVITY_LOG')") 
	public String showActivityLog(Model model, @RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) { 
		log.info("Fetching activity log, page {}, size {}", page, size);
		Pageable pageable = PageRequest.of(page, size);
		Page<ActivityLogEntry> activityLogPage = activityLogService.getAllLogs(pageable);

		model.addAttribute("activityLogPage", activityLogPage);
		model.addAttribute("activityLogs", activityLogPage.getContent());

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", activityLogPage.getTotalPages());
		model.addAttribute("totalItems", activityLogPage.getTotalElements());
		model.addAttribute("size", size);

		return "admin/activity-log";
	}
}