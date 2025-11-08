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
			const role = dataset.roleName.replace('ROLE_', '') || 'N/A'; // Clean up role name

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

	initializeModalForm({
		modalId: 'addAdminModal',
		formId: 'addAdminForm',
		validationAttribute: 'data-show-add-admin-modal',
		wrapperId: 'admin-content-wrapper'
	});

	initializeModalForm({
		modalId: 'editAdminModal',
		formId: 'editAdminForm',
		validationAttribute: 'data-show-edit-admin-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-admin-btn',
		modalTitleSelector: '#editAdminModalLabel',
		titlePrefix: 'Edit Admin: ',
		titleDatasetKey: 'name',
		onShow: function(form, dataset, isEdit, isValidationReopen) {
			// This populates the checkboxes based on the user's *individual* overrides
			if (isEdit && dataset && !isValidationReopen) {
				const setCheckbox = (id, value) => {
					const el = form.querySelector(id);
					if (el) {
						el.checked = (value === 'true');
					}
				};
				setCheckbox('#editManageCustomers', dataset.manageCustomers);
				setCheckbox('#editManageAdmins', dataset.manageAdmins);
				setCheckbox('#editManageOrders', dataset.manageOrders);
				setCheckbox('#editManageProducts', dataset.manageProducts);
				setCheckbox('#editManageInventory', dataset.manageInventory);
				setCheckbox('#editManageTransactions', dataset.manageTransactions);
				setCheckbox('#editManageSite', dataset.manageSite);
				setCheckbox('#editManageActivityLog', dataset.manageActivityLog);
			}
		}
	});

	initializeModalForm({
		modalId: 'editProfileModal',
		formId: 'editProfileForm',
		validationAttribute: 'data-show-edit-profile-modal',
		wrapperId: 'admin-content-wrapper'
	});


	initializeModalForm({
		modalId: 'manageRolesModal',
		formId: 'addRoleForm',
		validationAttribute: 'data-show-manage-roles-modal',
		wrapperId: 'admin-content-wrapper'
	});

	initializeModalForm({
		modalId: 'editRoleModal',
		formId: 'editRoleForm',
		validationAttribute: 'data-show-edit-role-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-role-btn',
		modalTitleSelector: '#editRoleModalLabel',
		titlePrefix: 'Edit Role: ',
		titleDatasetKey: 'name',
		onShow: function(form, dataset, isEdit, isValidationReopen) {
			// ALL LOGIC REMOVED FROM HERE, as there are no checkboxes to populate
		}
	});

});