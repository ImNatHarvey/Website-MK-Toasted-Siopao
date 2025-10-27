/**
 * Main script file for admin portal features.
 */
document.addEventListener('DOMContentLoaded', function() {

	const mainElement = document.querySelector('main');
	if (!mainElement) return;

	// --- Logic to re-open modal on validation error ---
	const modalsToReopen = {
		'manageCategoriesModal': mainElement.dataset.showManageCategoriesModal,
		'addProductModal': mainElement.dataset.showAddProductModal,
		'manageAdminsModal': mainElement.dataset.showManageAdminsModal,
		'addCustomerModal': mainElement.dataset.showAddCustomerModal,
		'editCustomerModal': mainElement.dataset.showEditCustomerModal,
		'editAdminModal': mainElement.dataset.showEditAdminModal, // <-- ADDED THIS
		'addItemModal': mainElement.dataset.showAddItemModal, // Inventory Item Add/Edit
		// Note: manageCategoriesModal is duplicated, ensure correct mapping if needed
		'manageUnitsModal': mainElement.dataset.showManageUnitsModal,
		'manageStockModal': mainElement.dataset.showManageStockModal
	};

	for (const modalId in modalsToReopen) {
		if (modalsToReopen[modalId] === 'true') {
			const modalElement = document.getElementById(modalId);
			if (modalElement) {
				if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
					try {
						// Use getInstance to avoid issues if modal was already initialized
						const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
						modalInstance.show();
					} catch (e) { console.error(`Error showing modal ${modalId}:`, e); }
				} else { console.error('Bootstrap Modal component not found.'); }
			} else { console.warn(`Modal element ${modalId} not found.`); }
		}
	}

	// --- Logic for "View Customer" Modal ---
	// ... (unchanged) ...
	const viewCustomerModal = document.getElementById('viewCustomerModal');
	if (viewCustomerModal) {
		viewCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const customerRow = button.closest('tr');
			if (!customerRow) return;

			const name = customerRow.dataset.name || 'N/A';
			const username = customerRow.dataset.username || 'N/A';
			const phone = customerRow.dataset.phone || 'N/A';
			const address1 = customerRow.dataset.addressLine1 || '';
			const address2 = customerRow.dataset.addressLine2 || '';

			viewCustomerModal.querySelector('#viewCustomerModalLabel').textContent = 'Details for ' + name;
			viewCustomerModal.querySelector('#viewCustomerName').textContent = name;
			viewCustomerModal.querySelector('#viewCustomerUsername').textContent = username;
			viewCustomerModal.querySelector('#viewCustomerPhone').textContent = phone;
			viewCustomerModal.querySelector('#viewCustomerAddress1').textContent = address1.trim();
			const address2Trimmed = address2.replace(/,\s*,/g, ',').replace(/^,\s*|,\s*$/g, '').trim();
			viewCustomerModal.querySelector('#viewCustomerAddress2').textContent = address2Trimmed.length > 0 ? address2Trimmed : 'No address provided.';
		});
	}

	// --- Logic for Edit Customer Modal ---
	// ... (unchanged) ...
	const editCustomerModal = document.getElementById('editCustomerModal');
	if (editCustomerModal) {
		const form = editCustomerModal.querySelector('#editCustomerForm');
		const modalTitle = editCustomerModal.querySelector('#editCustomerModalLabel');

		editCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget; // Button that triggered the modal
			if (!button || !button.classList.contains('edit-customer-btn')) {
				return;
			}

			const dataset = button.dataset;

			modalTitle.textContent = 'Edit: ' + dataset.firstName + ' ' + dataset.lastName;

			form.querySelector('#id').value = dataset.id || '';
			form.querySelector('#firstName').value = dataset.firstName || '';
			form.querySelector('#lastName').value = dataset.lastName || '';
			form.querySelector('#username').value = dataset.username || '';
			form.querySelector('#phone').value = dataset.phone || '';
			form.querySelector('#houseNo').value = dataset.houseNo || '';
			form.querySelector('#lotNo').value = dataset.lotNo || '';
			form.querySelector('#blockNo').value = dataset.blockNo || '';
			form.querySelector('#street').value = dataset.street || '';
			form.querySelector('#barangay').value = dataset.barangay || '';
			form.querySelector('#municipality').value = dataset.municipality || '';
			form.querySelector('#province').value = dataset.province || '';

			form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
		});

		editCustomerModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditCustomerModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert) errorAlert.remove();
			}
		});
	}

	// --- NEW: Logic for Edit Admin Modal ---
	const editAdminModal = document.getElementById('editAdminModal');
	if (editAdminModal) {
		const form = editAdminModal.querySelector('#editAdminForm');
		const modalTitle = editAdminModal.querySelector('#editAdminModalLabel');

		editAdminModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget; // Button that triggered the modal
			if (!button || !button.classList.contains('edit-admin-btn')) {
				// Only populate if triggered by the edit button
				return;
			}

			const dataset = button.dataset;

			// Set modal title
			modalTitle.textContent = 'Edit: ' + dataset.firstName + ' ' + dataset.lastName;

			// Populate form fields (Ensure the IDs match those in the modal form)
			form.querySelector('#id').value = dataset.id || ''; // Hidden field
			form.querySelector('#editAdminFirstName').value = dataset.firstName || '';
			form.querySelector('#editAdminLastName').value = dataset.lastName || '';
			form.querySelector('#editAdminUsername').value = dataset.username || '';

			// Clear any previous validation classes
			form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
		});

		// Clear validation on hide, unless shown due to validation
		editAdminModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditAdminModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert) errorAlert.remove();
			}
		});
	}

	// --- Logic for Add/Edit Inventory Item Modal ---
	// ... (unchanged) ...
	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');
		const itemStockInput = addItemModal.querySelector('#itemCurrentStock');
		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget; // Button that triggered the modal
			const isEdit = button && button.classList.contains('edit-item-btn');

			if (isEdit && button.dataset) {
				modalTitle.textContent = 'Edit Inventory Item';
				itemIdInput.value = button.dataset.id || '';
				itemNameInput.value = button.dataset.name || '';
				itemCategorySelect.value = button.dataset.categoryId || '';
				itemUnitSelect.value = button.dataset.unitId || '';
				itemStockInput.value = button.dataset.currentStock || '0.00';
				itemLowThresholdInput.value = button.dataset.lowThreshold || '0.00';
				itemCriticalThresholdInput.value = button.dataset.criticalThreshold || '0.00';
				itemCostInput.value = button.dataset.cost || '';
			} else {
				modalTitle.textContent = 'Add New Inventory Item';
				if (itemForm) itemForm.reset();
				itemIdInput.value = '';
			}
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
		});
	}

	// --- Recipe Ingredient Management ---
	// ... (unchanged) ...
	function addIngredientRow(containerId, templateId) {
		const template = document.getElementById(templateId);
		const containerDiv = document.getElementById(containerId);
		if (!template || !containerDiv) return;

		const currentRowCount = containerDiv.querySelectorAll('.ingredient-row').length;
		const index = currentRowCount;

		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true);
		const newRowElement = fragment.querySelector('.ingredient-row');

		if (!newRowElement) return;

		newRowElement.querySelectorAll('[name]').forEach(input => {
			input.name = input.name.replace('[INDEX]', `[${index}]`);
		});

		containerDiv.appendChild(newRowElement);
	}

	function removeIngredientRow(button) {
		const rowToRemove = button.closest('.ingredient-row');
		if (rowToRemove) {
			rowToRemove.remove();
		}
	}

	const addIngredientBtn = document.getElementById('addIngredientBtn');
	if (addIngredientBtn) {
		addIngredientBtn.addEventListener('click', () => addIngredientRow('addIngredientsContainer', 'ingredientRowTemplate'));
	}

	const addIngredientBtnEdit = document.getElementById('addIngredientBtnEdit');
	if (addIngredientBtnEdit) {
		addIngredientBtnEdit.addEventListener('click', () => addIngredientRow('editIngredientsContainer', 'ingredientRowTemplateEdit'));
	}

	document.addEventListener('click', function(event) {
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
		}
	});

});