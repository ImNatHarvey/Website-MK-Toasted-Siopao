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
			String adminUsername = "mktoastedadmin";
			String adminPassword = "mktoasted123"; // The password we want to set

			// Try to find the admin user
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);

			if (existingAdminOptional.isPresent()) {
				log.info(">>> Admin user '{}' found. Forcing password update...", adminUsername);
				User adminUser = existingAdminOptional.get();
				// Re-encode and set the password
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				userRepository.save(adminUser); // Save the updated user
				log.info(">>> Password for admin user '{}' has been reset/updated.", adminUsername);

			} else {
				log.info(">>> No admin user '{}' found, creating one...", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole("ROLE_ADMIN");
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				adminUser.setPhone("0000000000");

				userRepository.save(adminUser);
				log.info(">>> Admin user '{}' created successfully with password '{}'", adminUsername, adminPassword);
			}
		};
	}
}
