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
			const role = dataset.roleName.replace('ROLE_', '') || 'N/A';
			const permissionsString = dataset.permissions || ''; // Get the string of permissions

			viewAdminModal.querySelector('#viewAdminModalLabel').textContent = 'Details for ' + name;
			viewAdminModal.querySelector('#viewAdminName').textContent = name;
			viewAdminModal.querySelector('#viewAdminUsername').textContent = username;
			viewAdminModal.querySelector('#viewAdminEmail').textContent = email;
			viewAdminModal.querySelector('#viewAdminRole').textContent = role;
			viewAdminModal.querySelector('#viewAdminCreatedAt').textContent = createdAt;

			// --- UPDATED LOGIC FOR POPULATING 8 MANAGEMENT MODULES ---
			const permissionGrid = viewAdminModal.querySelector('#viewAdminPermissionGrid');
			permissionGrid.innerHTML = ''; // Clear old content

			// Create a Set for quick lookup of assigned permissions
			const assignedPermSet = new Set(permissionsString.split(',').filter(p => p.trim() !== ''));

			// Define the management modules and their associated permissions
			const managementModules = [
				{ name: 'Customer Management', perms: ['VIEW_CUSTOMERS', 'ADD_CUSTOMERS', 'EDIT_CUSTOMERS', 'DELETE_CUSTOMERS'] },
				{ name: 'Admin Management', perms: ['VIEW_ADMINS', 'ADD_ADMINS', 'EDIT_ADMINS', 'DELETE_ADMINS'] },
				{ name: 'Order Management', perms: ['VIEW_ORDERS', 'EDIT_ORDERS'] },
				{ name: 'Product Management', perms: ['VIEW_PRODUCTS', 'ADD_PRODUCTS', 'EDIT_PRODUCTS', 'DELETE_PRODUCTS', 'ADJUST_PRODUCT_STOCK'] },
				{ name: 'Inventory Management', perms: ['VIEW_INVENTORY', 'ADD_INVENTORY_ITEMS', 'EDIT_INVENTORY_ITEMS', 'DELETE_INVENTORY_ITEMS', 'ADJUST_INVENTORY_STOCK', 'MANAGE_INVENTORY_CATEGORIES', 'MANAGE_UNITS'] },
				{ name: 'Transaction History', perms: ['VIEW_TRANSACTIONS'] },
				{ name: 'Site Management', perms: ['EDIT_SITE_SETTINGS'] },
				{ name: 'Activity Log', perms: ['VIEW_ACTIVITY_LOG'] }
			];

			// Iterate over the management modules
			managementModules.forEach(module => {
				// Check if ANY of the module's permissions are in the user's permission set
				const hasPermission = module.perms.some(permKey => assignedPermSet.has(permKey));
				const friendlyName = module.name;
	
				const permItem = document.createElement('div');
				permItem.className = 'd-flex align-items-center gap-2 mb-2';
				// Use col-6 to make it two columns
				permItem.style.flexBasis = 'calc(50% - 0.5rem)';
	
				const icon = document.createElement('i');
				icon.className = hasPermission ? 'fa-solid fa-check text-success' : 'fa-solid fa-times text-danger';
				icon.style.width = '20px'; // Ensure alignment
	
				const text = document.createElement('span');
				text.textContent = friendlyName;
	
				permItem.appendChild(icon);
				permItem.appendChild(text);
				permissionGrid.appendChild(permItem);
			});

			// Handle case where no module permissions are found
			const hasAnyModulePermission = managementModules.some(module => module.perms.some(permKey => assignedPermSet.has(permKey)));
			
			if (!hasAnyModulePermission) {
				const noPermsDiv = document.createElement('div');
				noPermsDiv.className = 'text-muted col-12';
				// All admins have VIEW_DASHBOARD, so we just note no *management* modules.
				noPermsDiv.textContent = 'No management modules assigned to this role (only base dashboard access).';
				permissionGrid.appendChild(noPermsDiv);
			}
			// --- END UPDATED LOGIC ---
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
			// --- REMOVED ALL CHECKBOX POPULATION LOGIC ---
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
			// --- ADDED: Logic to populate permission checkboxes for editing a ROLE ---
			if (isEdit && dataset && !isValidationReopen) {
				const setCheckbox = (id, value) => {
					const el = form.querySelector(id);
					if (el) {
						el.checked = (value === 'true');
					}
				};
				console.log("Populating Edit Role modal with permissions:", dataset);
				setCheckbox('#editRoleManageCustomers', dataset.manageCustomers);
				setCheckbox('#editRoleManageAdmins', dataset.manageAdmins);
				setCheckbox('#editRoleManageOrders', dataset.manageOrders);
				setCheckbox('#editRoleManageProducts', dataset.manageProducts);
				setCheckbox('#editRoleManageInventory', dataset.manageInventory);
				setCheckbox('#editRoleManageTransactions', dataset.manageTransactions);
				setCheckbox('#editRoleManageSite', dataset.manageSite);
				setCheckbox('#editRoleManageActivityLog', dataset.manageActivityLog);
			}
		}
	});

	// --- ADDED ---
	initializeModalForm({
		modalId: 'changePasswordModal',
		formId: 'changePasswordForm',
		validationAttribute: 'data-show-change-password-modal',
		wrapperId: 'admin-content-wrapper'
	});
	// --- END ADDED ---

});