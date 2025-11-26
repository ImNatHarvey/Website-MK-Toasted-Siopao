document.addEventListener('DOMContentLoaded', function() {
	console.log("Main global-script.js loaded.");

	const forceRemoveAllModalState = () => {
		const backdrops = document.querySelectorAll('.modal-backdrop');
		let removedCount = 0;
		if (backdrops.length > 0) {
			backdrops.forEach(backdrop => { backdrop.remove(); removedCount++; });
		}
		let bodyHadClass = false;
		if (document.body.classList.contains('modal-open')) {
			document.body.classList.remove('modal-open'); bodyHadClass = true;
		}
		document.body.style.overflow = ''; document.body.style.paddingRight = '';
		if (removedCount > 0 || bodyHadClass) { console.warn(`Force cleanup: Removed ${removedCount} backdrops. Reset body state.`); }
	};

	const urlParams = new URLSearchParams(window.location.search);
	const modalToShow = urlParams.get('showModal');
	
	// ========================================================================
	// == START: NEW PASSWORD TOGGLE LOGIC ==
	// ========================================================================

	/**
	 * Initializes a password visibility toggle icon for a given input field.
	 * @param {HTMLInputElement} inputElement - The password input field.
	 */
	function initPasswordToggle(inputElement) {
		// Check if it's a password field
		if (!inputElement || inputElement.type !== 'password') {
			return;
		}

		const parent = inputElement.parentElement;

		// Prevent double-init
		if (parent.querySelector('.password-toggle-icon')) {
			return;
		}
		
		// Create the icon
		const icon = document.createElement('i');
		icon.classList.add('fa-solid', 'fa-eye', 'password-toggle-icon');
		icon.setAttribute('title', 'Show password');
		
		// Add click listener
		icon.addEventListener('click', () => {
			if (inputElement.type === 'password') {
				inputElement.type = 'text';
				icon.classList.remove('fa-eye');
				icon.classList.add('fa-eye-slash');
				icon.setAttribute('title', 'Hide password');
			} else {
				inputElement.type = 'password';
				icon.classList.remove('fa-eye-slash');
				icon.classList.add('fa-eye');
				icon.setAttribute('title', 'Show password');
			}
		});

		// Check if parent is an input-group
		if (parent.classList.contains('input-group')) {
			// If it's an input group, just append the icon.
			// The input-group itself provides the relative positioning.
			parent.appendChild(icon);
		} else {
			// If it's a standalone input, create a wrapper
			const wrapper = document.createElement('div');
			wrapper.classList.add('password-toggle-wrapper');
			
			// Replace the input with the wrapper
			parent.insertBefore(wrapper, inputElement);
			
			// Move the input inside the wrapper
			wrapper.appendChild(inputElement);
			
			// Append the icon inside the wrapper
			wrapper.appendChild(icon);
		}
	}

	/**
	 * Finds and initializes all password toggles within a given container (or the whole document).
	 * @param {HTMLElement} container - The element to search within.
	 */
	function initAllPasswordToggles(container) {
		const passwordFields = (container || document).querySelectorAll('input[type="password"]');
		passwordFields.forEach(initPasswordToggle);
	}

	// --- Initialize for all non-modal fields on page load ---
	initAllPasswordToggles(document.body);
	
	// ========================================================================
	// == END: NEW PASSWORD TOGGLE LOGIC ==
	// ========================================================================

	// ========================================================================
	// == START: POST-REDIRECT VALIDATION DISPLAY LOGIC (MOVED FROM PROFILE.JS) ==
	// ========================================================================
	/**
	 * Manually makes Thymeleaf-rendered invalid-feedback messages visible
	 * after a redirect-based validation failure on a non-modal form.
	 * Target forms: #passwordForm (profile), #signupForm, #resetPasswordForm.
	 */
	function initPostRedirectValidation() {
		// Forms that rely on flash attributes and redirects for validation errors
		const formsWithErrors = ['passwordForm', 'signupForm', 'resetPasswordForm'];

		formsWithErrors.forEach(formId => {
			const form = document.getElementById(formId);
			if (form) {
				console.log(`Checking form #${formId} for existing errors...`);
				const invalidFeedbacks = form.querySelectorAll('.invalid-feedback');
				
				invalidFeedbacks.forEach(feedback => {
					// Check if the feedback element has content (meaning Thymeleaf wrote an error)
					if (feedback.textContent && feedback.textContent.trim() !== '') {
						console.log(`Found error content in #${formId}. Activating display.`);
						
						// 1. Show the error message
						feedback.classList.add('d-block');
						
						// 2. Ensure the associated input field is marked as invalid (for red border)
						// Find the correct element to apply 'is-invalid' to
						let input = feedback.previousElementSibling;
						
						// Handle password toggle wrapper, as the invalid class needs to be on the input
						if (input && input.classList.contains('password-toggle-wrapper')) {
							input = input.querySelector('input[type="password"], input[type="text"]');
						}
						
						if (input && !input.classList.contains('is-invalid')) {
							input.classList.add('is-invalid');
						}
					}
				});
			}
		});
	}
	// Call the new validation re-display logic on page load
	initPostRedirectValidation();
	// ========================================================================
	// == END: POST-REDIRECT VALIDATION DISPLAY LOGIC ==
	// ========================================================================


	if (modalToShow) {
		console.log(`URL parameter 'showModal' found: ${modalToShow}`);
		forceRemoveAllModalState();

		const checkBootstrapAndShow = () => {
			if (typeof bootstrap !== 'undefined' && bootstrap.Modal && bootstrap.Modal.getInstance) {
				console.log(`Bootstrap ready. Finding: #${modalToShow}`);
				const modalElement = document.getElementById(modalToShow);

				if (modalElement) {
					console.log(`Element #${modalToShow} found.`);
					try {
						forceRemoveAllModalState();

						const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement);

						if (modalInstance) {
							console.log(`Instance for #${modalToShow} obtained.`);

							const standardCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for #${modalToShow}. Cleaning up.`);
								setTimeout(forceRemoveAllModalState, 50);
								modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							};

							const manageStockCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for manageStockModal. AGGRESSIVE Cleaning up.`);
								setTimeout(forceRemoveAllModalState, 10);
								setTimeout(forceRemoveAllModalState, 60);
								setTimeout(forceRemoveAllModalState, 120);
								modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);
							};

							modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);

							if (modalToShow === 'manageStockModal') {
								modalElement.addEventListener('hidden.bs.modal', manageStockCleanupOnHide);
								console.log("Attached AGGRESSIVE cleanup listener to manageStockModal.");
							} else {
								modalElement.addEventListener('hidden.bs.modal', standardCleanupOnHide);
								console.log("Attached standard cleanup listener.");
							}

							modalElement.addEventListener('shown.bs.modal', () => {
								console.log(`#${modalToShow} fully shown. Applying manual validation styles...`);
								
								// --- ADDED: Initialize password toggles inside the modal ---
								initAllPasswordToggles(modalElement);
								
								const errorMessages = modalElement.querySelectorAll('.invalid-feedback');

								errorMessages.forEach(msg => {
									if (msg.textContent.trim() !== '') {
										console.log("Found error message:", msg.textContent.trim());

										msg.classList.add('d-block');

										let el = msg.previousElementSibling;

										if (el && el.classList.contains('form-text')) {
											el = el.previousElementSibling;
										}

										if (el) {
											if (el.classList.contains('form-control') || el.classList.contains('form-select')) {
												el.classList.add('is-invalid');
												console.log("Applying 'is-invalid' to:", el);
											} else if (el.classList.contains('input-group')) {
												el.classList.add('is-invalid');
												const inputInside = el.querySelector('.form-control, .form-select');
												if (inputInside) {
													inputInside.classList.add('is-invalid');
													console.log("Applying 'is-invalid' to group and input inside:", el);
												}
											} else if (el.classList.contains('d-flex')) {
												const inputInside = el.querySelector('.form-control.threshold-input');
												if (inputInside) {
													inputInside.classList.add('is-invalid');
													console.log("Applying 'is-invalid' to threshold input:", inputInside);
												}
											} else {
												console.warn("Could not find matching input for error message:", msg.textContent.trim());
											}
										} else {
											console.warn("No previous sibling found for error message:", msg.textContent.trim());
										}
									}
								});
							}, { once: true });

							console.log(`Calling show() for #${modalToShow}...`);
							modalInstance.show();
							console.log(`show() called for #${modalToShow}.`);

							try {
								const nextURL = window.location.pathname + window.location.search.replace(/[\?&]showModal=[^&]+/, '').replace(/^&/, '?');
								history.replaceState(null, '', nextURL);
								console.log("Cleaned URL parameter.");
							} catch (e) { console.warn("Could not clean URL", e); }
						} else { console.error(`Failed getOrCreateInstance for #${modalToShow}`); }
					} catch (e) { console.error(`Error showing modal ${modalToShow}:`, e); }
				} else { console.warn(`Element #${modalToShow} NOT FOUND.`); }
			} else {
				console.warn("Bootstrap API not ready, retrying...");
				setTimeout(checkBootstrapAndShow, 100);
			}
		};
		setTimeout(checkBootstrapAndShow, 50);

	} else {
		console.log("No 'showModal' URL parameter found.");
	}

	// ========================================================================
	// == MODIFIED: CONFIRMATION MODAL LOGIC (NOW HANDLES 3 MODALS) ==
	// ========================================================================
	let formToSubmit = null;

	// --- 1. Simple Delete Modal ---
	const confirmDeleteModalEl = document.getElementById('confirmDeleteModal');
	if (confirmDeleteModalEl) {
		console.log("Delete confirmation modal found. Attaching listeners.");
		const confirmDeleteModal = new bootstrap.Modal(confirmDeleteModalEl);
		const confirmDeleteButton = document.getElementById('confirmDeleteButton');
		const confirmDeleteMessage = document.getElementById('confirmDeleteMessage');

		confirmDeleteButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null;
			}
		});

		confirmDeleteModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});
	} else {
		console.log("Standard delete confirmation modal not found.");
	}

	// --- 2. Simple Save Modal ---
	const confirmSaveModalEl = document.getElementById('confirmSaveModal');
	if (confirmSaveModalEl) {
		console.log("Save confirmation modal found. Attaching listeners.");
		const confirmSaveModal = new bootstrap.Modal(confirmSaveModalEl);
		const confirmSaveButton = document.getElementById('confirmSaveButton');
		const confirmSaveMessage = document.getElementById('confirmSaveMessage');

		confirmSaveButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null;
			}
		});

		confirmSaveModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});
	} else {
		console.log("Save confirmation modal not found.");
	}

	// --- 3. NEW: Secure Password Delete Modal ---
	const passwordConfirmDeleteModalEl = document.getElementById('passwordConfirmDeleteModal');
	if (passwordConfirmDeleteModalEl) {
		console.log("Secure password delete modal found. Attaching listeners.");
		const passwordConfirmDeleteModal = new bootstrap.Modal(passwordConfirmDeleteModalEl);
		const passwordConfirmDeleteButton = document.getElementById('passwordConfirmDeleteButton');
		const passwordConfirmDeleteMessage = document.getElementById('passwordConfirmDeleteMessage');
		const passwordInput = document.getElementById('secureDeletePassword');
		const passwordFeedback = document.getElementById('secureDeletePasswordFeedback');

		passwordConfirmDeleteButton.addEventListener('click', function() {
			const password = passwordInput.value;
			if (!password) {
				passwordInput.classList.add('is-invalid');
				passwordFeedback.style.display = 'block';
				return;
			}

			if (formToSubmit) {
				// Create and append the hidden password input
				const hiddenPasswordInput = document.createElement('input');
				hiddenPasswordInput.type = 'hidden';
				hiddenPasswordInput.name = 'password';
				hiddenPasswordInput.value = password;
				formToSubmit.appendChild(hiddenPasswordInput);

				// Disable button to prevent double-submit
				passwordConfirmDeleteButton.disabled = true;
				passwordConfirmDeleteButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Deleting...';

				// Submit the form
				formToSubmit.submit();
				formToSubmit = null;
			}
		});

		passwordConfirmDeleteModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
			// Reset modal state
			passwordInput.value = '';
			passwordInput.classList.remove('is-invalid');
			passwordFeedback.style.display = 'none';
			passwordConfirmDeleteButton.disabled = false;
			passwordConfirmDeleteButton.innerHTML = '<i class="fa-solid fa-trash me-1"></i> Confirm Delete';
		});
	} else {
		console.log("Secure password delete modal not found.");
	}

	// --- 4. Main Submit Interceptor ---
	document.addEventListener('submit', function(event) {
		const form = event.target;
		
		const secureDeleteMessage = form.dataset.secureDeleteMessage;
		const simpleDeleteMessage = form.dataset.confirmMessage;
		const simpleSaveMessage = form.dataset.confirmSaveMessage;

		if (secureDeleteMessage && passwordConfirmDeleteModalEl) {
			// --- Handle Secure Delete ---
			event.preventDefault();
			formToSubmit = form;
			// ===============================================
			// == THIS IS THE FIX ==
			// ===============================================
			const modal = bootstrap.Modal.getOrCreateInstance(passwordConfirmDeleteModalEl);
			// ===============================================
			// == END FIX ==
			// ===============================================
			const msgEl = document.getElementById('passwordConfirmDeleteMessage');
			if(msgEl) msgEl.textContent = secureDeleteMessage;
			modal.show();
			
		} else if (simpleDeleteMessage && confirmDeleteModalEl) {
			// --- Handle Simple Delete ---
			event.preventDefault();
			formToSubmit = form;
			const modal = bootstrap.Modal.getInstance(confirmDeleteModalEl);
			const msgEl = document.getElementById('confirmDeleteMessage');
			if(msgEl) msgEl.textContent = simpleDeleteMessage;
			modal.show();
			
		} else if (simpleSaveMessage && confirmSaveModalEl) {
			// --- Handle Simple Save ---
			event.preventDefault();
			formToSubmit = form;
			const modal = bootstrap.Modal.getInstance(confirmSaveModalEl);
			const msgEl = document.getElementById('confirmSaveMessage');
			if(msgEl) msgEl.textContent = simpleSaveMessage;
			modal.show();
		}
	});

});