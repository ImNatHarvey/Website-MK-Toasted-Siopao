package com.toastedsiopao;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

@SpringBootApplication
public class MKToastedSiopaoWebsiteApplication {

	private static final Logger log = LoggerFactory.getLogger(MKToastedSiopaoWebsiteApplication.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(MKToastedSiopaoWebsiteApplication.class, args);
	}

	// This bean runs once on application startup
	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			// --- Admin User ---
			String adminUsername = "mktoastedadmin";
			String adminPassword = "mktoasted123";
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);
			if (existingAdminOptional.isEmpty()) {
				log.info(">>> Creating admin user '{}'", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole("ROLE_ADMIN");
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				userRepository.save(adminUser);
				log.info(">>> Admin user created.");
			} else {
				log.info(">>> Admin user '{}' already exists.", adminUsername);
			}

			// --- Test Customer User ---
			String customerUsername = "testcustomer";
			String customerPassword = "password123";
			Optional<User> existingCustomerOptional = userRepository.findByUsername(customerUsername);
			if (existingCustomerOptional.isEmpty()) {
				log.info(">>> Creating test customer user '{}'", customerUsername);
				User customerUser = new User();
				customerUser.setUsername(customerUsername);
				customerUser.setPassword(passwordEncoder.encode(customerPassword));
				customerUser.setRole("ROLE_CUSTOMER");
				customerUser.setFirstName("Test");
				customerUser.setLastName("Customer");
				userRepository.save(customerUser);
				log.info(">>> Test customer user created.");
			} else {
				log.info(">>> Test customer user '{}' already exists.", customerUsername);
			}
		};
	}
}