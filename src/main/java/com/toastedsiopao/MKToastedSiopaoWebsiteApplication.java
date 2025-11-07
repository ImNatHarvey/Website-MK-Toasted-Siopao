package com.toastedsiopao;

import java.time.Clock; // Import Clock
import java.time.LocalDateTime; // Import LocalDateTime
import java.time.ZoneId; // Import ZoneId
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // Import EnableScheduling
import org.springframework.security.crypto.password.PasswordEncoder;

import com.toastedsiopao.model.User;
import com.toastedsiopao.model.Permission; // NEW IMPORT
import com.toastedsiopao.model.Role; // NEW IMPORT
import com.toastedsiopao.repository.RoleRepository; // NEW IMPORT
import com.toastedsiopao.repository.UserRepository;

@SpringBootApplication
@EnableScheduling // --- NEW: Enable scheduled tasks ---
public class MKToastedSiopaoWebsiteApplication {

	private static final Logger log = LoggerFactory.getLogger(MKToastedSiopaoWebsiteApplication.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository; // NEW INJECTION

	@Autowired
	private PasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(MKToastedSiopaoWebsiteApplication.class, args);
	}

	// --- NEW: Clock Bean ---
	/**
	 * Creates a Clock bean set to "Asia/Manila" timezone. This ensures all
	 * time-sensitive logic (like "Sales Today") is consistent. * @return A Clock
	 * bean.
	 */
	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Manila"));
	}
	// --- END NEW ---

	// This bean runs once on application startup
	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			// --- NEW: Create Roles ---
			Role ownerRole = roleRepository.findByName("ROLE_OWNER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_OWNER' role...");
				Role newOwnerRole = new Role("ROLE_OWNER");
				// Add all permissions from the enum
				Arrays.stream(Permission.values()).forEach(newOwnerRole::addPermission);
				return roleRepository.save(newOwnerRole);
			});

			Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_ADMIN' role...");
				Role newAdminRole = new Role("ROLE_ADMIN");
				// Add some default permissions for a basic admin (can be edited later)
				newAdminRole.addPermission(Permission.VIEW_DASHBOARD);
				newAdminRole.addPermission(Permission.VIEW_ORDERS);
				newAdminRole.addPermission(Permission.VIEW_CUSTOMERS);
				newAdminRole.addPermission(Permission.VIEW_PRODUCTS);
				newAdminRole.addPermission(Permission.VIEW_INVENTORY);
				return roleRepository.save(newAdminRole);
			});

			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_CUSTOMER' role...");
				Role newCustomerRole = new Role("ROLE_CUSTOMER");
				// Customers have no admin-panel permissions
				return roleRepository.save(newCustomerRole);
			});
			// --- END NEW ROLES ---

			// --- Admin User (OWNER) ---
			String adminUsername = "mktoastedadmin";
			String adminPassword = "mktoasted123";
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);

			if (existingAdminOptional.isEmpty()) {
				log.info(">>> Creating admin user '{}'", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole(ownerRole); // UPDATED
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				// Status, CreatedAt, LastActivity will be set by @PrePersist
				userRepository.save(adminUser);
				log.info(">>> Admin user created.");
			} else {
				// --- NEW: Patch existing user if needed ---
				User adminUser = existingAdminOptional.get();
				boolean needsUpdate = false;
				if (adminUser.getRole() == null) { // Check if role is missing
					adminUser.setRole(ownerRole); // Assign owner role
					needsUpdate = true;
				}
				if (adminUser.getCreatedAt() == null) {
					adminUser.setCreatedAt(LocalDateTime.now().minusDays(1)); // Set to arbitrary past date
					needsUpdate = true;
				}
				if (adminUser.getLastActivity() == null) {
					adminUser.setLastActivity(LocalDateTime.now());
					needsUpdate = true;
				}
				if (adminUser.getStatus() == null) {
					adminUser.setStatus("ACTIVE");
					needsUpdate = true;
				}
				if (needsUpdate) {
					log.info(">>> Patching existing admin user '{}' with default role/status/activity dates.",
							adminUsername);
					userRepository.save(adminUser);
				} else {
					log.info(">>> Admin user '{}' already exists and is up-to-date.", adminUsername);
				}
				// --- END NEW ---
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
				customerUser.setRole(customerRole); // UPDATED
				customerUser.setFirstName("Test");
				customerUser.setLastName("Customer");
				// Status, CreatedAt, LastActivity will be set by @PrePersist
				userRepository.save(customerUser);
				log.info(">>> Test customer user created.");
			} else {
				// --- NEW: Patch existing user if needed ---
				User customerUser = existingCustomerOptional.get();
				boolean needsUpdate = false;
				if (customerUser.getRole() == null) { // Check if role is missing
					customerUser.setRole(customerRole); // Assign customer role
					needsUpdate = true;
				}
				if (customerUser.getCreatedAt() == null) {
					customerUser.setCreatedAt(LocalDateTime.now().minusDays(1)); // Set to arbitrary past date
					needsUpdate = true;
				}
				if (customerUser.getLastActivity() == null) {
					customerUser.setLastActivity(LocalDateTime.now());
					needsUpdate = true;
				}
				if (customerUser.getStatus() == null) {
					customerUser.setStatus("ACTIVE");
					needsUpdate = true;
				}
				if (needsUpdate) {
					log.info(">>> Patching existing customer user '{}' with default role/status/activity dates.",
							customerUsername);
					userRepository.save(customerUser);
				} else {
					log.info(">>> Test customer user '{}' already exists and is up-to-date.", customerUsername);
				}
				// --- END NEW ---
			}
		};
	}
}