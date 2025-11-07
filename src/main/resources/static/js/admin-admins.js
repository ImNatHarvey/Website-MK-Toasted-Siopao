/**
 * JavaScript specific to the Admin Management page (admin/admins.html)
 * Handles modal population for View Admin, Edit Admin, Add Admin, and Edit Profile.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-admins.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');
	if (!mainElement) {
		console.error("Main element #admin-content-wrapper not found in admin-admins.js!");
		return;
	}

	// --- Logic for "View Admin" Modal ---
	const viewAdminModal = document.getElementById('viewAdminModal');
	if (viewAdminModal) {
		viewAdminModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const adminRow = button ? button.closest('tr') : null;
			if (!adminRow) {
				console.warn("View Admin Modal opened without a valid row source.");
				return;
			}
			const dataset = adminRow.dataset;
			console.log("Populating View Admin Modal with data:", dataset);

			const name = dataset.name || 'N/A';
			const username = dataset.username || 'N/A';
			const email = dataset.email || 'N/A';
			const createdAt = dataset.createdAt || 'N/A';
			const role = dataset.roleName || 'N/A'; // UPDATED: Use roleName

			viewAdminModal.querySelector('#viewAdminModalLabel').textContent = 'Details for ' + name;
			viewAdminModal.querySelector('#viewAdminName').textContent = name;
			viewAdminModal.querySelector('#viewAdminUsername').textContent = username;
			viewAdminModal.querySelector('#viewAdminEmail').textContent = email;
			viewAdminModal.querySelector('#viewAdminRole').textContent = role; // UPDATED
			viewAdminModal.querySelector('#viewAdminCreatedAt').textContent = createdAt;
		});
	}

	// --- Logic for "Edit Admin" Modal (Editing OTHERS) ---
	const editAdminModal = document.getElementById('editAdminModal');
	if (editAdminModal) {
		const form = editAdminModal.querySelector('#editAdminForm');
		const modalTitle = editAdminModal.querySelector('#editAdminModalLabel');

		editAdminModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const adminRow = button ? button.closest('tr') : null;

			if (!adminRow || !button.classList.contains('edit-admin-btn')) {
				console.warn("Edit Admin modal opened without edit button source.");
				if (form) form.reset();
				return;
			}
			const dataset = adminRow.dataset;
			console.log("Populating Edit Admin Modal with data:", dataset);

			modalTitle.textContent = 'Edit Admin: ' + (dataset.name || 'User');
			if (!form) { console.error("Edit admin form not found!"); return; }

			// --- Helper to set checkbox value ---
			const setCheckbox = (id, value) => {
				const el = form.querySelector(id);
				if (el) {
					el.checked = (value === 'true');
				}
			};
			// --- End Helper ---

			form.querySelector('input[name="id"]').value = dataset.id || '';
			form.querySelector('#editAdminFirstName').value = dataset.firstName || '';
			form.querySelector('#editAdminLastName').value = dataset.lastName || '';
			form.querySelector('#editAdminUsername').value = dataset.username || '';
			form.querySelector('#editAdminEmail').value = dataset.email || '';
			// form.querySelector('#editAdminRole').value = dataset.roleId || ''; // REMOVED

			// --- NEW: Populate Role Name and Permissions ---
			form.querySelector('#editAdminRoleName').value = dataset.roleName || '';
			setCheckbox('#editManageCustomers', dataset.manageCustomers);
			setCheckbox('#editManageAdmins', dataset.manageAdmins);
			setCheckbox('#editManageOrders', dataset.manageOrders);
			setCheckbox('#editManageProducts', dataset.manageProducts);
			setCheckbox('#editManageInventory', dataset.manageInventory);
			setCheckbox('#editManageTransactions', dataset.manageTransactions);
			setCheckbox('#editManageSite', dataset.manageSite);
			setCheckbox('#editManageActivityLog', dataset.manageActivityLog);
			// --- END NEW ---


			if (mainElement.dataset.showEditAdminModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			}
		});

		editAdminModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditAdminModal !== 'true') {
				console.log("Clearing Edit Admin modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-edit-admin-modal');
			}
		});
	}

	// --- Logic for "Add Admin" Modal ---
	const addAdminModal = document.getElementById('addAdminModal');
	if (addAdminModal) {
		const form = addAdminModal.querySelector('#addAdminForm');
		addAdminModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddAdminModal !== 'true') {
				console.log("Clearing Add Admin modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-add-admin-modal');
			}
		});
	}

	// --- Logic for "Edit My Profile" Modal (Editing SELF) ---
	const editProfileModal = document.getElementById('editProfileModal');
	if (editProfileModal) {
		const form = editProfileModal.querySelector('#editProfileForm');

		editProfileModal.addEventListener('show.bs.modal', function(event) {
			// This modal doesn't use a table row, it uses data from the current user
			// We'll assume the controller has populated `adminProfileDto`
			// with the current user's data if it's NOT a validation reopen.

			const isValidationReopen = mainElement.dataset.showEditProfileModal === 'true';
			console.log("Edit Profile Modal show.bs.modal. Validation Reopen:", isValidationReopen);

			if (isValidationReopen) {
				console.log("Edit Profile modal reopening from validation.");
				// Values are already populated by Thymeleaf
			} else {
				// We need to fetch the current admin's data and populate
				// This part is tricky without an API call.
				// Let's modify this to fetch from the controller-populated DTO
				// This assumes `adminProfileDto` is *always* populated with the logged-in admin's data
				console.log("Populating Edit Profile modal.");
				// This relies on `adminProfileDto` being correctly populated by the controller
				// We will add this logic to the controller in the next step.
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			}
		});

		editProfileModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditProfileModal !== 'true') {
				console.log("Clearing Edit Profile modal on hide (not validation reopen).")
				// We don't reset, as it should hold the user's data
				// if(form) form.reset(); 
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-edit-profile-modal');
			}
		});
	}

});