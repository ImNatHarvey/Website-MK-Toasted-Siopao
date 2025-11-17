package com.toastedsiopao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // IMPORTED
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.toastedsiopao.model.User;
import com.toastedsiopao.model.Permission;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.repository.UserRepository;

@SpringBootApplication
@EnableScheduling
public class MKToastedSiopaoWebsiteApplication {

	private static final Logger log = LoggerFactory.getLogger(MKToastedSiopaoWebsiteApplication.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${mk.admin.username}")
	private String adminUsername;

	@Value("${mk.admin.password}")
	private String adminPassword;

	public static void main(String[] args) {
		SpringApplication.run(MKToastedSiopaoWebsiteApplication.class, args);
	}

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Manila"));
	}

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			
			// --- START: NEW REFACTOR FOR OWNER ROLE ---
			final Role ownerRole; // Make it final from the start
			
			Optional<Role> ownerRoleOpt = roleRepository.findByName("ROLE_OWNER");
			boolean needsSave = false;
			Role roleToSave; // Use a temp variable for modification

			if (ownerRoleOpt.isEmpty()) {
				// --- Path 1: Role does not exist, create it ---
				log.info(">>> Creating 'ROLE_OWNER' role...");
				roleToSave = new Role("ROLE_OWNER");
				Arrays.stream(Permission.values()).forEach(permission -> roleToSave.addPermission(permission.name()));
				needsSave = true;
			} else {
				// --- Path 2: Role exists, check for missing permissions ---
				roleToSave = ownerRoleOpt.get();
				long missingPerms = Arrays.stream(Permission.values())
					.filter(permission -> !roleToSave.getPermissions().contains(permission.name()))
					.count();
				
				if (missingPerms > 0) {
					log.warn(">>> ROLE_OWNER is missing {} permissions. Adding them now...", missingPerms);
					Arrays.stream(Permission.values()).forEach(permission -> roleToSave.addPermission(permission.name()));
					needsSave = true;
				}
			}

			if (needsSave) {
				log.info(">>> Saving updates to ROLE_OWNER...");
				ownerRole = roleRepository.save(roleToSave); // Assign to final variable
			} else {
				ownerRole = roleToSave; // Assign existing to final variable
			}
			// --- END: NEW REFACTOR ---

			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_CUSTOMER' role...");
				Role newCustomerRole = new Role("ROLE_CUSTOMER");
				return roleRepository.save(newCustomerRole);
			});

			// Admin logic
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);
			if (existingAdminOptional.isEmpty()) {
				log.info(">>> Creating admin user '{}'", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole(ownerRole); // Use final variable
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				userRepository.save(adminUser);
				log.info(">>> Admin user created.");
			} else {
				User adminUser = existingAdminOptional.get();
				boolean needsUpdate = false;
				
				if (adminUser.getRole() == null || !adminUser.getRole().getName().equals("ROLE_OWNER")) {
					adminUser.setRole(ownerRole); // Use final variable
					needsUpdate = true;
				}
				
				if (adminUser.getCreatedAt() == null) {
					adminUser.setCreatedAt(LocalDateTime.now().minusDays(1));
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
			}

			// Customer logic
			String customerUsername = "testcustomer";
			String customerPassword = "password123";
			Optional<User> existingCustomerOptional = userRepository.findByUsername(customerUsername);

			if (existingCustomerOptional.isEmpty()) {
				log.info(">>> Creating test customer user '{}'", customerUsername);
				User customerUser = new User();
				customerUser.setUsername(customerUsername);
				customerUser.setPassword(passwordEncoder.encode(customerPassword));
				customerUser.setRole(customerRole);
				customerUser.setFirstName("Test");
				customerUser.setLastName("Customer");
				userRepository.save(customerUser);
				log.info(">>> Test customer user created.");
			} else {
				User customerUser = existingCustomerOptional.get();
				boolean needsUpdate = false;
				if (customerUser.getRole() == null) {
					customerUser.setRole(customerRole);
					needsUpdate = true;
				}
				if (customerUser.getCreatedAt() == null) {
					customerUser.setCreatedAt(LocalDateTime.now().minusDays(1));
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
			}
		};
	}
}