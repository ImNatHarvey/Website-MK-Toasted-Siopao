/**
 * JavaScript specific to the Admin Customers page (admin/customers.html)
 * Handles modal population for View Customer, Edit Customer.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-customers.js loaded"); // Confirm script is running

	// **** FIX IS HERE ****
	// Select the specific <div> from customers.html
	const mainElement = document.getElementById('admin-content-wrapper');
	// **** END OF FIX ****

	if (!mainElement) {
		console.error("Main element not found in admin-customers.js!");
		return;
	}

	// --- Logic for "View Customer" Modal ---
	const viewCustomerModal = document.getElementById('viewCustomerModal');
	if (viewCustomerModal) {
		viewCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const customerRow = button ? button.closest('tr') : null; // Ensure button exists
			if (!customerRow) {
				console.warn("View Customer Modal opened without a valid row source.");
				return;
			}

			const dataset = customerRow.dataset;
			console.log("Populating View Customer Modal with data:", dataset); // Debug

			const name = dataset.name || 'N/A';
			const username = dataset.username || 'N/A';
			const email = dataset.email || 'N/A'; // NEW
			const phone = dataset.phone || 'N/A';
			const address1 = dataset.addressLine1 || '';
			const address2 = dataset.addressLine2 || '';
			const status = dataset.status || 'N/A'; // NEW
			const createdAt = dataset.createdAt || 'N/A'; // NEW
			const lastActivity = dataset.lastActivity || 'N/A'; // NEW

			viewCustomerModal.querySelector('#viewCustomerModalLabel').textContent = 'Details for ' + name;
			viewCustomerModal.querySelector('#viewCustomerName').textContent = name;
			viewCustomerModal.querySelector('#viewCustomerUsername').textContent = username;
			viewCustomerModal.querySelector('#viewCustomerEmail').textContent = email; // NEW
			viewCustomerModal.querySelector('#viewCustomerPhone').textContent = phone;
			viewCustomerModal.querySelector('#viewCustomerAddress1').textContent = address1.trim();
			// Clean up address line 2 formatting
			const address2Trimmed = address2.replace(/,\s*,/g, ',').replace(/^,\s*|,\s*$/g, '').trim();
			viewCustomerModal.querySelector('#viewCustomerAddress2').textContent = address2Trimmed.length > 0 ? address2Trimmed : 'No address details provided.';

			// NEW: Populate status badge
			const statusBadge = viewCustomerModal.querySelector('#viewCustomerStatusBadge');
			statusBadge.textContent = status;
			statusBadge.className = 'status-badge'; // Reset classes
			if (status === 'ACTIVE') {
				statusBadge.classList.add('status-active');
			} else {
				statusBadge.classList.add('status-cancelled'); // Using 'cancelled' style for 'inactive'
			}

			// NEW: Populate dates
			viewCustomerModal.querySelector('#viewCustomerCreatedAt').textContent = createdAt;
			viewCustomerModal.querySelector('#viewCustomerLastActivity').textContent = lastActivity;
		});
	}

	// --- Logic for Edit Customer Modal ---
	const editCustomerModal = document.getElementById('editCustomerModal');
	if (editCustomerModal) {
		const form = editCustomerModal.querySelector('#editCustomerForm');
		const modalTitle = editCustomerModal.querySelector('#editCustomerModalLabel');

		editCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('edit-customer-btn')) {
				console.warn("Edit Customer modal opened without edit button source.");
				// Optionally reset form if opened unexpectedly
				if (form) form.reset();
				return;
			}
			const dataset = button.dataset;
			console.log("Populating Edit Customer Modal with data:", dataset); // Debug

			modalTitle.textContent = 'Edit Customer: ' + (dataset.firstName || '') + ' ' + (dataset.lastName || '');
			if (!form) { console.error("Edit customer form not found!"); return; }

			form.querySelector('#id').value = dataset.id || '';
			form.querySelector('#firstName').value = dataset.firstName || '';
			form.querySelector('#lastName').value = dataset.lastName || '';
			form.querySelector('#username').value = dataset.username || '';
			form.querySelector('#email').value = dataset.email || '';
			form.querySelector('#phone').value = dataset.phone || '';
			form.querySelector('#houseNo').value = dataset.houseNo || '';
			form.querySelector('#lotNo').value = dataset.lotNo || '';
			form.querySelector('#blockNo').value = dataset.blockNo || '';
			form.querySelector('#street').value = dataset.street || '';
			form.querySelector('#barangay').value = dataset.barangay || '';
			form.querySelector('#municipality').value = dataset.municipality || '';
			form.querySelector('#province').value = dataset.province || '';
			form.querySelector('#status').value = dataset.status || 'ACTIVE'; // NEW: Populate status

			// Clear previous validation highlights unless reopening
			if (mainElement.dataset.showEditCustomerModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); // Debug
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger'); // General validation alert
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); // Debug
			}
		});

		editCustomerModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if not flagged to stay open
			if (mainElement.dataset.showEditCustomerModal !== 'true') {
				console.log("Clearing Edit Customer modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
			} else {
				console.log("Resetting showEditCustomerModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-edit-customer-modal');
			}
		});
	}


	// --- Logic for Add Customer Modal (Clear on Hide) ---
	const addCustomerModal = document.getElementById('addCustomerModal');
	if (addCustomerModal) {
		const form = addCustomerModal.querySelector('#addCustomerForm');

		addCustomerModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddCustomerModal !== 'true') {
				console.log("Clearing Add Customer modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showAddCustomerModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-add-customer-modal');
			}
		});
	}

	// --- REMOVED manageAdminsModal, editAdminModal, viewAdminModal ---

}); // End DOMContentLoaded for admin-customers.js