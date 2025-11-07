/**
 * Main script file - Handles modal reopening based on URL parameters.
 * NEW: Handles mobile sidebar toggle.
 * NEW: Handles global delete and save confirmation modals.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("Main script.js loaded.");

	// Force remove ALL backdrops and modal state from body
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

	// --- Logic to re-open modal based on URL parameter ---
	const urlParams = new URLSearchParams(window.location.search);
	const modalToShow = urlParams.get('showModal'); // e.g., 'manageStockModal'

	if (modalToShow) {
		console.log(`URL parameter 'showModal' found: ${modalToShow}`);
		forceRemoveAllModalState(); // Initial cleanup attempt

		const checkBootstrapAndShow = () => {
			if (typeof bootstrap !== 'undefined' && bootstrap.Modal && bootstrap.Modal.getInstance) {
				console.log(`Bootstrap ready. Finding: #${modalToShow}`);
				const modalElement = document.getElementById(modalToShow);

				if (modalElement) {
					console.log(`Element #${modalToShow} found.`);
					try {
						forceRemoveAllModalState(); // Cleanup again before getting instance

						const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement);

						if (modalInstance) {
							console.log(`Instance for #${modalToShow} obtained.`);

							// **** MODIFIED: Conditional Listener for Manage Stock ****
							// Standard cleanup listener for MOST modals
							const standardCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for #${modalToShow}. Cleaning up.`);
								// Short delay might still be needed sometimes
								setTimeout(forceRemoveAllModalState, 50);
								modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							};

							// Aggressive cleanup listener SPECIFICALLY for manageStockModal
							const manageStockCleanupOnHide = () => {
								console.log(`hidden.bs.modal triggered for manageStockModal. AGGRESSIVE Cleaning up.`);
								// Force remove multiple times with delays - overkill, but targets the stubborn issue
								setTimeout(forceRemoveAllModalState, 10);
								setTimeout(forceRemoveAllModalState, 60);
								setTimeout(forceRemoveAllModalState, 120);
								modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);
							};

							// Remove any previous listeners first
							modalElement.removeEventListener('hidden.bs.modal', standardCleanupOnHide);
							modalElement.removeEventListener('hidden.bs.modal', manageStockCleanupOnHide);

							// Apply the appropriate listener
							if (modalToShow === 'manageStockModal') {
								modalElement.addEventListener('hidden.bs.modal', manageStockCleanupOnHide);
								console.log("Attached AGGRESSIVE cleanup listener to manageStockModal.");
							} else {
								modalElement.addEventListener('hidden.bs.modal', standardCleanupOnHide);
								console.log("Attached standard cleanup listener.");
							}
							// **** END MODIFICATION ****


							// --- UPDATED: New logic to manually apply 'is-invalid' ---
							modalElement.addEventListener('shown.bs.modal', () => {
								console.log(`#${modalToShow} fully shown. Applying manual validation styles...`);
								// Find all invalid-feedback divs that have content
								const errorMessages = modalElement.querySelectorAll('.invalid-feedback');

								errorMessages.forEach(msg => {
									// Check if Thymeleaf has added error text and it's not just whitespace
									if (msg.textContent.trim() !== '') {
										console.log("Found error message:", msg.textContent.trim());

										// --- FIX: Make the error message visible ---
										// Bootstrap hides .invalid-feedback by default. We must manually show it.
										msg.classList.add('d-block');
										// --- END FIX ---

										// Get the element right before the error message
										let el = msg.previousElementSibling;

										// --- NEW LOGIC to handle threshold fields ---
										// If the sibling is a '.form-text', look one more sibling up
										if (el && el.classList.contains('form-text')) {
											el = el.previousElementSibling;
										}
										// --- END NEW LOGIC ---

										if (el) {
											if (el.classList.contains('form-control') || el.classList.contains('form-select')) {
												// Simple case: <input> or <select>
												el.classList.add('is-invalid');
												console.log("Applying 'is-invalid' to:", el);
											} else if (el.classList.contains('input-group')) {
												// Complex case: <div class="input-group">
												el.classList.add('is-invalid'); // Add to group wrapper
												const inputInside = el.querySelector('.form-control, .form-select');
												if (inputInside) {
													inputInside.classList.add('is-invalid'); // Add to input inside
													console.log("Applying 'is-invalid' to group and input inside:", el);
												}
											} else if (el.classList.contains('d-flex')) {
												// --- NEW CASE: Handle threshold 'd-flex' container ---
												const inputInside = el.querySelector('.form-control.threshold-input');
												if (inputInside) {
													inputInside.classList.add('is-invalid');
													console.log("Applying 'is-invalid' to threshold input:", inputInside);
												}
												// --- END NEW CASE ---
											} else {
												console.warn("Could not find matching input for error message:", msg.textContent.trim());
											}
										} else {
											console.warn("No previous sibling found for error message:", msg.textContent.trim());
										}
									}
								});
							}, { once: true });
							// --- END UPDATED LOGIC ---

							// Show the modal
							console.log(`Calling show() for #${modalToShow}...`);
							modalInstance.show();
							console.log(`show() called for #${modalToShow}.`);

							// Clean up URL
							try { /* ... URL cleanup ... */
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
		setTimeout(checkBootstrapAndShow, 50); // Keep initial delay short

	} else {
		// This block is now correctly empty.
		// The old, buggy version had a "setTimeout(forceRemoveAllModalState, 150);" here.
		console.log("No 'showModal' URL parameter found.");
	}

	// =================================================
	// == NEW: Admin Sidebar Toggle Logic ==
	// =================================================
	const sidebarToggle = document.getElementById('sidebarToggle');
	const sidebarOverlay = document.getElementById('sidebarOverlay');
	const adminSidebar = document.getElementById('admin-sidebar');
	const adminBody = document.getElementById('admin-body');

	if (sidebarToggle && sidebarOverlay && adminSidebar && adminBody) {
		console.log("Mobile sidebar elements found. Attaching listeners.");

		// 1. Click the hamburger icon
		sidebarToggle.addEventListener('click', function() {
			adminBody.classList.toggle('sidebar-toggled');
		});

		// 2. Click the dark overlay
		sidebarOverlay.addEventListener('click', function() {
			adminBody.classList.remove('sidebar-toggled');
		});

		// 3. Click a link *inside* the sidebar
		const sidebarLinks = adminSidebar.querySelectorAll('.nav-link');
		sidebarLinks.forEach(link => {
			link.addEventListener('click', function() {
				// Only close if the sidebar is actually open (in mobile view)
				if (adminBody.classList.contains('sidebar-toggled')) {
					adminBody.classList.remove('sidebar-toggled');
				}
			});
		});

	} else {
		console.log("Mobile sidebar elements not found (this is normal on non-admin pages).");
	}
	// =================================================
	// == END: Admin Sidebar Toggle Logic ==
	// =================================================


	// =================================================
	// == UPDATED: Global Confirmation Modal Logic ==
	// =================================================
	let formToSubmit = null; // Variable to hold the form

	// --- Logic for DELETE Modal ---
	const confirmDeleteModalEl = document.getElementById('confirmDeleteModal');
	if (confirmDeleteModalEl) {
		console.log("Delete confirmation modal found. Attaching listeners.");
		const confirmDeleteModal = new bootstrap.Modal(confirmDeleteModalEl);
		const confirmDeleteButton = document.getElementById('confirmDeleteButton');
		const confirmDeleteMessage = document.getElementById('confirmDeleteMessage');

		// 2. Listen for the confirm button click
		confirmDeleteButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null; // Clear after submitting
			}
		});

		// 3. Clear form on hide (if not submitted)
		confirmDeleteModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});

		// 1. Intercept form submissions for DELETE
		document.addEventListener('submit', function(event) {
			const form = event.target;
			// Check if it's a form that needs confirmation
			if (form.dataset.confirmMessage) {
				event.preventDefault(); // Stop the form from submitting
				formToSubmit = form; // Store the form
				const message = form.dataset.confirmMessage || 'Are you sure?';
				confirmDeleteMessage.textContent = message;
				confirmDeleteModal.show();
			}
		});

	} else {
		console.log("Delete confirmation modal not found (this is normal on non-admin pages).");
	}

	// --- NEW: Logic for SAVE Modal ---
	const confirmSaveModalEl = document.getElementById('confirmSaveModal');
	if (confirmSaveModalEl) {
		console.log("Save confirmation modal found. Attaching listeners.");
		const confirmSaveModal = new bootstrap.Modal(confirmSaveModalEl);
		const confirmSaveButton = document.getElementById('confirmSaveButton');
		const confirmSaveMessage = document.getElementById('confirmSaveMessage');

		// 2. Listen for the confirm button click
		confirmSaveButton.addEventListener('click', function() {
			if (formToSubmit) {
				formToSubmit.submit();
				formToSubmit = null; // Clear after submitting
			}
		});

		// 3. Clear form on hide (if not submitted)
		confirmSaveModalEl.addEventListener('hidden.bs.modal', function() {
			formToSubmit = null;
		});

		// 1. Intercept form submissions for SAVE
		document.addEventListener('submit', function(event) {
			const form = event.target;
			// Check if it's a form that needs confirmation
			if (form.dataset.confirmSaveMessage) {
				event.preventDefault(); // Stop the form from submitting
				formToSubmit = form; // Store the form
				const message = form.dataset.confirmSaveMessage || 'Are you sure?';
				confirmSaveMessage.textContent = message;
				confirmSaveModal.show();
			}
		});

	} else {
		console.log("Save confirmation modal not found (this is normal on non-admin pages).");
	}
	// =================================================
	// == END: Global Confirmation Modal Logic ==
	// =================================================

});