package com.toastedsiopao.config;

import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		// 1. Load the user from Google
		OAuth2User oAuth2User = super.loadUser(userRequest);

		// 2. Extract details
		Map<String, Object> attributes = oAuth2User.getAttributes();
		String email = (String) attributes.get("email");
		String firstName = (String) attributes.get("given_name");
		String lastName = (String) attributes.get("family_name");

		if (firstName == null)
			firstName = "User";
		if (lastName == null)
			lastName = "";

		// 3. Check if user exists in our DB by EMAIL
		Optional<User> userOptional = userRepository.findByEmail(email);
		User user;

		if (userOptional.isPresent()) {
			user = userOptional.get();

			// --- FIX: Handle Status Checks for Existing Users ---
			// Block DISABLED (Banned) users.
			if ("DISABLED".equals(user.getStatus())) {
				log.warn("Blocked login attempt for DISABLED user: {}", user.getUsername());
				throw new OAuth2AuthenticationException(new OAuth2Error("account_disabled"),
						"Your account has been disabled.");
			}
			// Allow INACTIVE users to proceed; the SuccessHandler will update their status
			// to ACTIVE.

			// Auto-verify PENDING users if they login via Google
			else if ("PENDING".equals(user.getStatus())) {
				user.setStatus("ACTIVE");
				user.setVerificationToken(null);
				userRepository.save(user);
				log.info("User {} account auto-verified via Google login.", user.getUsername());
			}
			// ---------------------------------------------------

			log.info("User {} logged in via Google.", user.getUsername());
		} else {
			// Register new user
			log.info("Registering new user via Google: {}", email);
			user = new User();
			user.setEmail(email);
			user.setFirstName(firstName);
			user.setLastName(lastName);

			String baseUsername = generateBaseUsername(firstName, lastName);
			String uniqueUsername = generateUniqueUsername(baseUsername);
			user.setUsername(uniqueUsername);

			user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
					.orElseThrow(() -> new RuntimeException("Default role ROLE_CUSTOMER not found"));
			user.setRole(customerRole);

			// Google users are automatically ACTIVE (verified)
			user.setStatus("ACTIVE");

			userRepository.save(user);
		}

		// 4. Return User
		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
			user.getRole().getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
		}

		Map<String, Object> extendedAttributes = new HashMap<>(attributes);
		extendedAttributes.put("db_username", user.getUsername());

		return new DefaultOAuth2User(authorities, extendedAttributes, "db_username");
	}

	private String generateBaseUsername(String firstName, String lastName) {
		String firstPart = firstName.trim().split("\\s+")[0];
		String lastPart = lastName.trim().replaceAll("\\s+", "");
		String rawUsername = (firstPart + lastPart).toLowerCase();
		return rawUsername.replaceAll("[^a-z0-9_]", "");
	}

	private String generateUniqueUsername(String baseUsername) {
		String candidateUsername = baseUsername;
		int counter = 1;
		while (userRepository.findByUsername(candidateUsername).isPresent()) {
			candidateUsername = baseUsername + counter;
			counter++;
		}
		return candidateUsername;
	}
}