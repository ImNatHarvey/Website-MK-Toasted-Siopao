document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-admins.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');
	if (!mainElement) {
		console.error("Main element #admin-content-wrapper not found in admin-admins.js!");
		return;
	}

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
			const role = dataset.roleName || 'N/A'; 

			viewAdminModal.querySelector('#viewAdminModalLabel').textContent = 'Details for ' + name;
			viewAdminModal.querySelector('#viewAdminName').textContent = name;
			viewAdminModal.querySelector('#viewAdminUsername').textContent = username;
			viewAdminModal.querySelector('#viewAdminEmail').textContent = email;
			viewAdminModal.querySelector('#viewAdminRole').textContent = role; 
			viewAdminModal.querySelector('#viewAdminCreatedAt').textContent = createdAt;

			const setPermissionIcon = (id, hasPermission) => {
				const el = viewAdminModal.querySelector(id);
				if (el) {
					const icon = el.querySelector('i');
					if (hasPermission === 'true') {
						icon.className = 'fa-solid fa-fw me-2 fa-check';
					} else {
						icon.className = 'fa-solid fa-fw me-2 fa-times';
					}
				}
			};

			setPermissionIcon('#viewPermCustomers', dataset.manageCustomers);
			setPermissionIcon('#viewPermAdmins', dataset.manageAdmins);
			setPermissionIcon('#viewPermOrders', dataset.manageOrders);
			setPermissionIcon('#viewPermProducts', dataset.manageProducts);
			setPermissionIcon('#viewPermInventory', dataset.manageInventory);
			setPermissionIcon('#viewPermTransactions', dataset.manageTransactions);
			setPermissionIcon('#viewPermSite', dataset.manageSite);
			setPermissionIcon('#viewPermActivityLog', dataset.manageActivityLog);
		});
	}

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

			const setCheckbox = (id, value) => {
				const el = form.querySelector(id);
				if (el) {
					el.checked = (value === 'true');
				}
			};

			form.querySelector('input[name="id"]').value = dataset.id || '';
			form.querySelector('#editAdminFirstName').value = dataset.firstName || '';
			form.querySelector('#editAdminLastName').value = dataset.lastName || '';
			form.querySelector('#editAdminUsername').value = dataset.username || '';
			form.querySelector('#editAdminEmail').value = dataset.email || '';

			form.querySelector('#editAdminRoleName').value = dataset.roleName || '';
			setCheckbox('#editManageCustomers', dataset.manageCustomers);
			setCheckbox('#editManageAdmins', dataset.manageAdmins);
			setCheckbox('#editManageOrders', dataset.manageOrders);
			setCheckbox('#editManageProducts', dataset.manageProducts);
			setCheckbox('#editManageInventory', dataset.manageInventory);
			setCheckbox('#editManageTransactions', dataset.manageTransactions);
			setCheckbox('#editManageSite', dataset.manageSite);
			setCheckbox('#editManageActivityLog', dataset.manageActivityLog);
			
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

	const editProfileModal = document.getElementById('editProfileModal');
	if (editProfileModal) {
		const form = editProfileModal.querySelector('#editProfileForm');

		editProfileModal.addEventListener('show.bs.modal', function(event) {

			const isValidationReopen = mainElement.dataset.showEditProfileModal === 'true';
			console.log("Edit Profile Modal show.bs.modal. Validation Reopen:", isValidationReopen);

			if (isValidationReopen) {
				console.log("Edit Profile modal reopening from validation.");
			} else {
				console.log("Populating Edit Profile modal.");
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			}
		});

		editProfileModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditProfileModal !== 'true') {
				console.log("Clearing Edit Profile modal on hide (not validation reopen).")
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-edit-profile-modal');
			}
		});
	}

});