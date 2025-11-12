package com.toastedsiopao.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity; // --- ADDED ---
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException; // --- ADDED ---
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handles database integrity violations, such as unique constraint failures
	 * (e.g., duplicate name) or foreign key constraint failures (e.g., deleting a
	 * category that is still in use by a product).
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public String handleDataIntegrityViolation(DataIntegrityViolationException ex,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "Operation failed: The name you entered may already exist, or the item is still in use (e.g., a category is in use by a product).";

		log.warn("Data integrity violation for request [{}]: {}. Sending user-friendly message: {}",
				request.getRequestURI(), ex.getMessage(), message);

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/dashboard");
	}

	// --- MODIFIED: Split the multipart handler into two ---

	/**
	 * Handles file upload errors, specifically when a file exceeds the 20MB limit
	 * defined in application.properties.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class) // --- SPECIFICALLY for size ---
	public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "File upload error: File size exceeds the 20MB limit.";

		log.warn("File size limit exceeded for request [{}]: {}.", request.getRequestURI(), ex.getMessage());

		redirectAttributes.addFlashAttribute("globalError", message); // Use globalError for the toast

		// Redirect back to the settings page, or the referer
		return "redirect:" + (referer != null ? referer : "/admin/settings");
	}

	/**
	 * Handles other generic multipart errors. With resolve-lazily=true, this should
	 * be rare, but it's good practice.
	 */
	@ExceptionHandler(MultipartException.class) // --- For OTHER multipart errors ---
	public String handleGenericMultipartException(MultipartException ex, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "A file-related error occurred. Please try again or contact support.";

		log.warn("Generic multipart error for request [{}]: {}.", request.getRequestURI(), ex.getMessage(), ex);

		redirectAttributes.addFlashAttribute("globalError", message); // Use globalError for the toast

		return "redirect:" + (referer != null ? referer : "/admin/settings");
	}
	// --- END MODIFIED ---

	/**
	 * A catch-all handler for any other unexpected runtime exceptions. This
	 * prevents users from seeing a white-label error page.
	 */
	@ExceptionHandler(Exception.class)
	public Object handleGenericException(Exception ex, RedirectAttributes redirectAttributes, // --- MODIFIED: Return
																								// type changed to
																								// Object ---
			HttpServletRequest request) {

		// --- MODIFIED: START ---
		// Handle 404 NoResourceFoundException gracefully to avoid ERROR spam
		if (ex instanceof NoResourceFoundException) {
			if (request.getRequestURI() != null && request.getRequestURI().equals("/favicon.ico")) {
				log.warn("Favicon not found (harmless): {}", ex.getMessage());
			} else {
				log.warn("Static resource not found (404): {}", ex.getMessage());
			}
			// Return a 404 response directly instead of trying to render a page
			return ResponseEntity.notFound().build();
		}
		// --- MODIFIED: END ---

		String referer = request.getHeader("Referer");
		String message = "An unexpected server error occurred. Please try again later or contact support.";

		// We log this as an ERROR because it was not an expected exception
		log.error("Unhandled exception for request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/dashboard");
	}
}