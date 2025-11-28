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

		// Fallback if details are missing
		if (firstName == null)
			firstName = "User";
		if (lastName == null)
			lastName = "";

		// 3. Check if user exists in our DB by EMAIL
		Optional<User> userOptional = userRepository.findByEmail(email);
		User user;

		if (userOptional.isPresent()) {
			// Update existing user info
			user = userOptional.get();
			// Optional: Update name from Google if you want to keep it synced
			// user.setFirstName(firstName);
			// user.setLastName(lastName);
			// userRepository.save(user);
			log.info("User {} logged in via Google.", user.getUsername());
		} else {
			// Register new user
			log.info("Registering new user via Google: {}", email);
			user = new User();
			user.setEmail(email);
			user.setFirstName(firstName);
			user.setLastName(lastName);

			// Generate Unique Username based on Name
			String baseUsername = generateBaseUsername(firstName, lastName);
			String uniqueUsername = generateUniqueUsername(baseUsername);
			user.setUsername(uniqueUsername);

			// Generate a random dummy password
			user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

			// Assign default Customer role
			Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
					.orElseThrow(() -> new RuntimeException("Default role ROLE_CUSTOMER not found"));
			user.setRole(customerRole);

			user.setStatus("ACTIVE");

			userRepository.save(user);
		}

		// 4. Return the OAuth2User with our DB Authorities (Role) attached
		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
			user.getRole().getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
		}

		// --- FIX: Use the Database Username as the Principal Name ---
		// We create a mutable copy of the attributes to add our custom key
		Map<String, Object> extendedAttributes = new HashMap<>(attributes);
		extendedAttributes.put("db_username", user.getUsername());

		// We pass "db_username" as the key.
		// Now principal.getName() will return 'joshcrisologo' instead of
		// 'email@gmail.com'
		return new DefaultOAuth2User(authorities, extendedAttributes, "db_username");
	}

	/**
	 * Generates a base username from First Name (1st word) + Last Name. Example:
	 * "Josh Harvey", "Crisologo" -> "josh" + "crisologo" -> "joshcrisologo"
	 */
	private String generateBaseUsername(String firstName, String lastName) {
		// Get the first word of the first name
		String firstPart = firstName.trim().split("\\s+")[0];

		// Remove spaces from last name to make it one string
		String lastPart = lastName.trim().replaceAll("\\s+", "");

		// Combine, lowercase, and strip non-alphanumeric characters (just to be safe)
		String rawUsername = (firstPart + lastPart).toLowerCase();
		return rawUsername.replaceAll("[^a-z0-9_]", "");
	}

	/**
	 * Checks if the username exists and appends a counter if necessary. Example:
	 * "joshcrisologo" -> "joshcrisologo1" -> "joshcrisologo2"
	 */
	private String generateUniqueUsername(String baseUsername) {
		String candidateUsername = baseUsername;
		int counter = 1;

		// Loop until we find a username that doesn't exist
		while (userRepository.findByUsername(candidateUsername).isPresent()) {
			candidateUsername = baseUsername + counter;
			counter++;
		}

		return candidateUsername;
	}
}