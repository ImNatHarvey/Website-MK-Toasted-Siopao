package com.toastedsiopao.service;

import com.toastedsiopao.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailServiceImpl implements EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	@Qualifier("emailTemplateEngine") // Use the specific template engine we'll create
	private TemplateEngine templateEngine;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Override
	@Async // Make email sending asynchronous so the user doesn't have to wait
	public void sendPasswordResetEmail(User user, String token, String resetUrl) throws MessagingException {
		if (user.getEmail() == null) {
			log.warn("Cannot send password reset email: User {} (ID: {}) has no email address.", user.getUsername(),
					user.getId());
			return;
		}

		log.info("Attempting to send password reset email to {}", user.getEmail());

		// 1. Create Thymeleaf context
		Context context = new Context();
		context.setVariable("name", user.getFirstName());
		context.setVariable("resetUrl", resetUrl);
		// You could also add the token if you want to display it (e.g., as an OTP)
		// context.setVariable("token", token);

		// 2. Process the HTML template
		String htmlBody = templateEngine.process("mail/password-reset", context);

		// 3. Create and send the email
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

		helper.setFrom(fromEmail);
		helper.setTo(user.getEmail());
		helper.setSubject("Your Password Reset Request - MK Toasted Siopao");
		helper.setText(htmlBody, true); // true = HTML email

		mailSender.send(message);
		log.info("Password reset email sent successfully to {}", user.getEmail());
	}
}