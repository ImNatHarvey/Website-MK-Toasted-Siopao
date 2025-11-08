package com.toastedsiopao;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
			Role ownerRole = roleRepository.findByName("ROLE_OWNER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_OWNER' role...");
				Role newOwnerRole = new Role("ROLE_OWNER");
				Arrays.stream(Permission.values()).forEach(permission -> newOwnerRole.addPermission(permission.name()));
				return roleRepository.save(newOwnerRole);
			});

			Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_ADMIN' role...");
				Role newAdminRole = new Role("ROLE_ADMIN");

				newAdminRole.addPermission(Permission.VIEW_DASHBOARD.name());
				newAdminRole.addPermission(Permission.VIEW_ORDERS.name());
				newAdminRole.addPermission(Permission.VIEW_CUSTOMERS.name());
				newAdminRole.addPermission(Permission.VIEW_PRODUCTS.name());
				newAdminRole.addPermission(Permission.VIEW_INVENTORY.name());
				return roleRepository.save(newAdminRole);
			});

			// --- NEW ROLES ---
			Role managerRole = roleRepository.findByName("ROLE_MANAGER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_MANAGER' role...");
				Role newRole = new Role("ROLE_MANAGER");
				newRole.addPermission(Permission.VIEW_DASHBOARD.name());
				newRole.addPermission(Permission.VIEW_ORDERS.name());
				newRole.addPermission(Permission.EDIT_ORDERS.name());
				newRole.addPermission(Permission.VIEW_CUSTOMERS.name());
				newRole.addPermission(Permission.VIEW_PRODUCTS.name());
				newRole.addPermission(Permission.ADJUST_PRODUCT_STOCK.name());
				newRole.addPermission(Permission.VIEW_INVENTORY.name());
				newRole.addPermission(Permission.ADJUST_INVENTORY_STOCK.name());
				newRole.addPermission(Permission.VIEW_TRANSACTIONS.name());
				return roleRepository.save(newRole);
			});

			Role staffRole = roleRepository.findByName("ROLE_STAFF").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_STAFF' role...");
				Role newRole = new Role("ROLE_STAFF");
				newRole.addPermission(Permission.VIEW_DASHBOARD.name());
				newRole.addPermission(Permission.VIEW_ORDERS.name());
				newRole.addPermission(Permission.EDIT_ORDERS.name());
				newRole.addPermission(Permission.VIEW_PRODUCTS.name());
				newRole.addPermission(Permission.ADJUST_PRODUCT_STOCK.name());
				newRole.addPermission(Permission.VIEW_INVENTORY.name());
				newRole.addPermission(Permission.ADJUST_INVENTORY_STOCK.name());
				return roleRepository.save(newRole);
			});
			// --- END NEW ROLES ---

			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER").orElseGet(() -> {
				log.info(">>> Creating 'ROLE_CUSTOMER' role...");
				Role newCustomerRole = new Role("ROLE_CUSTOMER");
				return roleRepository.save(newCustomerRole);
			});
			String adminUsername = "mktoastedadmin";
			String adminPassword = "mktoasted123";
			Optional<User> existingAdminOptional = userRepository.findByUsername(adminUsername);

			if (existingAdminOptional.isEmpty()) {
				log.info(">>> Creating admin user '{}'", adminUsername);
				User adminUser = new User();
				adminUser.setUsername(adminUsername);
				adminUser.setPassword(passwordEncoder.encode(adminPassword));
				adminUser.setRole(ownerRole);
				adminUser.setFirstName("Admin");
				adminUser.setLastName("User");
				userRepository.save(adminUser);
				log.info(">>> Admin user created.");
			} else {
				User adminUser = existingAdminOptional.get();
				boolean needsUpdate = false;
				if (adminUser.getRole() == null) {
					adminUser.setRole(ownerRole);
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