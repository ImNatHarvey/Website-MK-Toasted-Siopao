package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import jakarta.mail.MessagingException;

public interface EmailService {

	void sendPasswordResetEmail(User user, String token, String resetUrl) throws MessagingException;

	void sendOrderStatusUpdateEmail(Order order, String subject, String message) throws MessagingException;
}