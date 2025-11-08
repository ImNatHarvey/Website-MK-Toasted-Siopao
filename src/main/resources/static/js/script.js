document.addEventListener('DOMContentLoaded', function() {
	console.log("Main script.js loaded.");

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

	const sidebarToggle = document.getElementById('sidebarToggle');
	const sidebarOverlay = document.getElementById('sidebarOverlay');
	const adminSidebar = document.getElementById('admin-sidebar');
	const adminBody = document.getElementById('admin-body');

	if (sidebarToggle && sidebarOverlay && adminSidebar && adminBody) {
		console.log("Mobile sidebar elements found. Attaching listeners.");

		sidebarToggle.addEventListener('click', function() {
			adminBody.classList.toggle('sidebar-toggled');
		});

		sidebarOverlay.addEventListener('click', function() {
			adminBody.classList.remove('sidebar-toggled');
		});

		const sidebarLinks = adminSidebar.querySelectorAll('.nav-link');
		sidebarLinks.forEach(link => {
			link.addEventListener('click', function() {
				if (adminBody.classList.contains('sidebar-toggled')) {
					adminBody.classList.remove('sidebar-toggled');
				}
			});
		});

	} else {
		console.log("Mobile sidebar elements not found (this is normal on non-admin pages).");
	}
	let formToSubmit = null; 

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

		document.addEventListener('submit', function(event) {
			const form = event.target;
			if (form.dataset.confirmMessage) {
				event.preventDefault(); 
				formToSubmit = form; 
				const message = form.dataset.confirmMessage || 'Are you sure?';
				confirmDeleteMessage.textContent = message;
				confirmDeleteModal.show();
			}
		});

	} else {
		console.log("Delete confirmation modal not found (this is normal on non-admin pages).");
	}
	
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

		document.addEventListener('submit', function(event) {
			const form = event.target;
			
			if (form.dataset.confirmSaveMessage) {
				event.preventDefault(); 
				formToSubmit = form; 
				const message = form.dataset.confirmSaveMessage || 'Are you sure?';
				confirmSaveMessage.textContent = message;
				confirmSaveModal.show();
			}
		});

	} else {
		console.log("Save confirmation modal not found (this is normal on non-admin pages).");
	}
});