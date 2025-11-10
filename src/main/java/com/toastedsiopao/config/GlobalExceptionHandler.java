package com.toastedsiopao.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

	/**
	 * A catch-all handler for any other unexpected runtime exceptions. This
	 * prevents users from seeing a white-label error page.
	 */
	@ExceptionHandler(Exception.class)
	public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "An unexpected server error occurred. Please try again later or contact support.";

		// We log this as an ERROR because it was not an expected exception
		log.error("Unhandled exception for request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/dashboard");
	}
}