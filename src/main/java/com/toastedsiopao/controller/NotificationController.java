package com.toastedsiopao.controller;

import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CustomerService customerService;

	@PostMapping("/admin/read/{id}")
	@PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
	public ResponseEntity<?> markAdminNotificationAsRead(@PathVariable Long id, Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			notificationService.markAdminNotificationAsRead(id);
			log.info("Admin user {} marked notification {} as read.", principal.getName(), id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Error marking admin notification {} as read: {}", id, e.getMessage());
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/user/read/{id}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<?> markUserNotificationAsRead(@PathVariable Long id, Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			notificationService.markUserNotificationAsRead(id, user);
			log.info("Customer user {} marked notification {} as read.", principal.getName(), id);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Error marking user notification {} as read: {}", id, e.getMessage());
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/admin/read-all")
	@PreAuthorize("hasAuthority('VIEW_DASHBOARD')")
	public ResponseEntity<?> markAllAdminNotificationsAsRead(Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			notificationService.markAllAdminNotificationsAsRead();
			log.info("Admin user {} marked ALL notifications as read.", principal.getName());
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Error marking all admin notifications as read: {}", e.getMessage());
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/user/read-all")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<?> markAllUserNotificationsAsRead(Principal principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}
		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return ResponseEntity.status(401).build();
		}
		try {
			notificationService.markAllUserNotificationsAsRead(user);
			log.info("Customer user {} marked ALL notifications as read.", principal.getName());
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			log.error("Error marking all user notifications as read: {}", e.getMessage());
			return ResponseEntity.internalServerError().build();
		}
	}
}