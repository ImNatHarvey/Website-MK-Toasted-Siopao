package com.toastedsiopao.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(DataIntegrityViolationException.class)
	public String handleDataIntegrityViolation(DataIntegrityViolationException ex,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		String referer = request.getHeader("Referer");

		String message = "Operation failed. The item may be in use (e.g., by a product, order, or user) or the name you entered may already exist.";

		log.warn("Data integrity violation for request [{}]: {}. Sending user-friendly message: {}",
				request.getRequestURI(), ex.getMessage(), message);

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/dashboard");
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "File upload error: File size exceeds the 20MB limit.";

		log.warn("File size limit exceeded for request [{}]: {}.", request.getRequestURI(), ex.getMessage());

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/settings");
	}

	@ExceptionHandler(MultipartException.class)
	public String handleGenericMultipartException(MultipartException ex, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		String referer = request.getHeader("Referer");
		String message = "A file-related error occurred. Please try again or contact support.";

		log.warn("Generic multipart error for request [{}]: {}.", request.getRequestURI(), ex.getMessage(), ex);

		redirectAttributes.addFlashAttribute("globalError", message); // Use globalError for the toast

		return "redirect:" + (referer != null ? referer : "/admin/settings");
	}

	@ExceptionHandler(Exception.class)
	public Object handleGenericException(Exception ex, RedirectAttributes redirectAttributes,
			HttpServletRequest request) {

		if (ex instanceof NoResourceFoundException) {
			if (request.getRequestURI() != null && request.getRequestURI().equals("/favicon.ico")) {
				log.warn("Favicon not found (harmless): {}", ex.getMessage());
			} else {
				log.warn("Static resource not found (404): {}", ex.getMessage());
			}
			
			return ResponseEntity.notFound().build();
		}

		String referer = request.getHeader("Referer");
		String message = "An unexpected server error occurred. Please try again later or contact support.";

		log.error("Unhandled exception for request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

		redirectAttributes.addFlashAttribute("globalError", message);

		return "redirect:" + (referer != null ? referer : "/admin/dashboard");
	}
}