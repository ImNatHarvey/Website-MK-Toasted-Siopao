package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import jakarta.mail.MessagingException;

public interface EmailService {

	/**
	 * Sends a password reset email to a user.
	 *
	 * @param user     The user account to send the email to.
	 * @param token    The unique password reset token.
	 * @param resetUrl The full URL the user will click to reset their password.
	 * @throws MessagingException if there's an error sending the mail.
	 */
	void sendPasswordResetEmail(User user, String token, String resetUrl) throws MessagingException;

	/**
	 * Sends an order status update email to a user.
	 *
	 * @param order   The order that was updated.
	 * @param subject The subject line for the email (e.g., "Your order is on the way!")
	 * @param message The main message to display in the email body.
	 * @throws MessagingException if there's an error sending the mail.
	 */
	void sendOrderStatusUpdateEmail(Order order, String subject, String message) throws MessagingException;
}