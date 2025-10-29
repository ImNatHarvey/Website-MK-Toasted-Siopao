/**
 * Main script file - Handles modal reopening based on URL parameters.
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


							// Apply manual styling after shown (Keep this)
							modalElement.addEventListener('shown.bs.modal', () => {
								/* ... JS code to manually add 'is-invalid' class ... */
								console.log(`#${modalToShow} fully shown. Applying manual styles if needed.`);
								const errorMessages = modalElement.querySelectorAll('.invalid-feedback');
								errorMessages.forEach(msg => {
									if (msg.textContent.trim() !== '' && msg.style.display !== 'none') {
										let inputField = msg.previousElementSibling;
										if (inputField && inputField.classList.contains('input-group')) { inputField = inputField.querySelector('input, select, textarea'); }
										else if (inputField && inputField.tagName !== 'INPUT' && inputField.tagName !== 'SELECT' && inputField.tagName !== 'TEXTAREA') { const parentDiv = msg.closest('div'); if (parentDiv) inputField = parentDiv.querySelector('input, select, textarea'); }
										if (inputField && (inputField.tagName === 'INPUT' || inputField.tagName === 'SELECT' || inputField.tagName === 'TEXTAREA')) {
											inputField.classList.add('is-invalid');
											const inputGroup = inputField.closest('.input-group'); if (inputGroup) { inputGroup.classList.add('is-invalid'); }
										}
									}
								});
							}, { once: true });

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
		console.log("No 'showModal' URL parameter found.");
		// General cleanup on normal page load, delayed
		setTimeout(forceRemoveAllModalState, 150);
	}
});