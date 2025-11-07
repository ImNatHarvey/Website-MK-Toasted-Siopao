/**
 * JavaScript specific to the Admin Inventory page (admin/inventory.html)
 * Handles modal population for Add/Edit Item.
 *
 * Relies on global functions from admin-utils.js:
 * - initThresholdSliders(modalElement)
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-inventory.js loaded"); // Confirm script is running

	// **** FIX IS HERE ****
	// Select the specific <div> from inventory.html
	const mainElement = document.getElementById('admin-content-wrapper');
	// **** END OF FIX ****

	if (!mainElement) {
		console.error("Main element not found in admin-inventory.js!");
		return;
	}

	// --- Logic for Add/Edit Inventory Item Modal ---
	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');

		// --- UPDATED: Get containers and inputs for stock ---
		// const itemStockInputContainer = addItemModal.querySelector('#itemStockInputContainer'); // REMOVED
		const itemStockInfoContainer = addItemModal.querySelector('#itemStockInfoContainer'); // KEPT
		// const itemStockInput = addItemModal.querySelector('#itemCurrentStock'); // REMOVED
		const itemStockHiddenInput = addItemModal.querySelector('#itemCurrentStockHidden'); // KEPT
		// --- END UPDATE ---

		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');

			// --- UPDATED: Check if we are reopening from validation ---
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';
			console.log("Add/Edit Item Modal 'show.bs.modal' event. IsEdit:", isEdit, "IsValidationReopen:", isValidationReopen); // Debug

			if (isEdit && button.dataset && !isValidationReopen) {
				console.log("Populating modal for EDIT with data:", button.dataset); // Debug
				modalTitle.textContent = 'Edit Inventory Item';
				itemIdInput.value = button.dataset.id || '';
				itemNameInput.value = button.dataset.name || '';
				itemCategorySelect.value = button.dataset.categoryId || '';
				itemUnitSelect.value = button.dataset.unitId || '';

				// --- UPDATED: Show/hide logic ---
				// if (itemStockInputContainer) itemStockInputContainer.style.display = 'none'; // REMOVED
				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'block'; // SHOW info box
				// Populate the *hidden* input for submission
				if (itemStockHiddenInput) itemStockHiddenInput.value = button.dataset.currentStock || '0.00';
				// --- END UPDATE ---

				itemLowThresholdInput.value = parseFloat(button.dataset.lowThreshold || '0').toFixed(0);
				itemCriticalThresholdInput.value = parseFloat(button.dataset.criticalThreshold || '0').toFixed(0);
				itemCostInput.value = button.dataset.cost || '';

			} else if (!isEdit && !isValidationReopen) {
				console.log("Populating modal for ADD (resetting form)."); // Debug
				modalTitle.textContent = 'Add New Inventory Item';
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; // Ensure ID is cleared for Add

				// --- UPDATED: Show/hide logic ---
				// if (itemStockInputContainer) itemStockInputContainer.style.display = 'block'; // REMOVED
				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'none'; // HIDE info box
				// --- END UPDATE ---

				itemLowThresholdInput.value = ''; // Default to blank
				itemCriticalThresholdInput.value = ''; // Default to blank

			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				// Thymeleaf has repopulated the form. We just need to fix the title
				// and the container visibility, which Thymeleaf *can't* do.
				if (itemIdInput.value) {
					modalTitle.textContent = 'Edit Inventory Item';
					// --- UPDATED: Show/hide logic for validation reopen ---
					// if (itemStockInputContainer) itemStockInputContainer.style.display = 'none'; // REMOVED
					if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'block'; // SHOW info box
					// --- END UPDATE ---
				} else {
					modalTitle.textContent = 'Add New Inventory Item';
					// --- UPDATED: Show/hide logic for validation reopen ---
					// if (itemStockInputContainer) itemStockInputContainer.style.display = 'block'; // REMOVED
					if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'none'; // HIDE info box
					// --- END UPDATE ---
				}
			}

			// Clear previous validation highlights unless it's being reopened by script.js
			if (mainElement.dataset.showAddItemModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); // Debug
				if (itemForm) itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = itemForm ? itemForm.querySelector('.alert.alert-danger') : null; // Assuming a general error display might exist
				if (globalError && globalError.getAttribute('th:if') === null) {
					globalError.remove();
				}
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); // Debug
			}

			// **** CALL GLOBAL SLIDER FUNCTION ****
			initThresholdSliders(addItemModal);
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state ONLY if the modal wasn't flagged to stay open (due to validation)
			if (mainElement.dataset.showAddItemModal !== 'true') {
				console.log("Clearing Add/Edit Item modal on hide (not validation reopen).") // Debug
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; // Ensure ID is cleared fully
				if (itemForm) itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = itemForm ? itemForm.querySelector('.alert.alert-danger') : null;
				if (globalError && globalError.getAttribute('th:if') === null) { // Avoid removing Thymeleaf conditional alerts
					globalError.remove();
				}
			} else {
				// If it was kept open due to error, reset the flag for the next time
				console.log("Resetting showAddItemModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-add-item-modal');
			}
		});
	}

	// --- Logic for Manage Categories Modal (Clear on Hide) ---
	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addInvCategoryForm'); // Assuming this ID

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				console.log("Clearing Manage Categories modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showManageCategoriesModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}

	// --- NEW: Logic for Edit Inventory Category Modal ---
	const editInvCategoryModal = document.getElementById('editInvCategoryModal');
	if (editInvCategoryModal) {
		const form = editInvCategoryModal.querySelector('#editInvCategoryForm');

		editInvCategoryModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isValidationReopen = mainElement.dataset.showEditInvCategoryModal === 'true';
			console.log("Edit Inv Category Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); // Debug

			if (button && button.classList.contains('edit-inv-category-btn') && !isValidationReopen) {
				const dataset = button.dataset;
				console.log("Populating Edit Inv Category Modal with data:", dataset); // Debug
				if (form) {
					form.querySelector('#editInvCategoryId').value = dataset.id || '';
					form.querySelector('#editInvCategoryName').value = dataset.name || '';
				}
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
			}

			// Clear previous validation highlights unless reopening
			if (mainElement.dataset.showEditInvCategoryModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); // Debug
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); // Debug
			}
		});

		editInvCategoryModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if not flagged to stay open
			if (mainElement.dataset.showEditInvCategoryModal !== 'true') {
				console.log("Clearing Edit Inv Category modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showEditInvCategoryModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-edit-inv-category-modal');
			}
		});
	}
	// --- END NEW ---


	// --- Logic for Manage Units Modal (Clear on Hide) ---
	const manageUnitsModal = document.getElementById('manageUnitsModal');
	if (manageUnitsModal) {
		const form = manageUnitsModal.querySelector('#addUnitForm'); // Assuming this ID

		manageUnitsModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageUnitsModal !== 'true') {
				console.log("Clearing Manage Units modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = form ? form.querySelector('.invalid-feedback.d-block') : null;
				if (globalError) globalError.textContent = ''; // Clear global error text
			} else {
				console.log("Resetting showManageUnitsModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-manage-units-modal');
			}
		});
	}

	// --- NEW: Logic for Edit Unit Modal ---
	const editUnitModal = document.getElementById('editUnitModal');
	if (editUnitModal) {
		const form = editUnitModal.querySelector('#editUnitForm');

		editUnitModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isValidationReopen = mainElement.dataset.showEditUnitModal === 'true';
			console.log("Edit Unit Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); // Debug

			if (button && button.classList.contains('edit-unit-btn') && !isValidationReopen) {
				const dataset = button.dataset;
				console.log("Populating Edit Unit Modal with data:", dataset); // Debug
				if (form) {
					form.querySelector('#editUnitId').value = dataset.id || '';
					form.querySelector('#editUnitName').value = dataset.name || '';
					form.querySelector('#editUnitAbbreviation').value = dataset.abbreviation || '';
				}
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
			}

			// Clear previous validation highlights unless reopening
			if (mainElement.dataset.showEditUnitModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); // Debug
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); // Debug
			}
		});

		editUnitModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if not flagged to stay open
			if (mainElement.dataset.showEditUnitModal !== 'true') {
				console.log("Clearing Edit Unit modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showEditUnitModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-edit-unit-modal');
			}
		});
	}
	// --- END NEW ---


	// --- Logic for Manage Stock Modal (Clear Inputs on Hide) ---
	const manageStockModal = document.getElementById('manageStockModal');
	if (manageStockModal) {
		manageStockModal.addEventListener('hidden.bs.modal', function() {
			// Always clear inputs when this modal closes, as it's less likely to have complex validation needs
			console.log("Clearing Manage Stock (Inventory) modal inputs on hide.") // Debug
			manageStockModal.querySelectorAll('.stock-adjust-form input[type="number"]').forEach(input => {
				input.value = '';
			});
			// Note: We don't need to check the data attribute here unless specific validation requires it.
			mainElement.removeAttribute('data-show-manage-stock-modal'); // Clear flag if it was set
		});
	}

	// --- NEW: Logic for "View Item" Modal ---
	const viewItemModal = document.getElementById('viewItemModal');
	if (viewItemModal) {
		viewItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-item-btn')) {
				console.warn("View Item Modal opened without a valid row source.");
				return;
			}

			const dataset = button.dataset;
			console.log("Populating View Item Modal with data:", dataset); // Debug

			// Helper function to set text content
			const setText = (id, value) => {
				const el = viewItemModal.querySelector(id);
				if (el) {
					el.textContent = value || 'N/A';
				} else {
					console.warn(`Element ${id} not found in viewItemModal.`);
				}
			};

			// Populate fields
			setText('#viewItemName', dataset.name);
			setText('#viewItemCategory', dataset.categoryName);
			setText('#viewItemUnit', dataset.unitName);
			setText('#viewItemCurrentStock', dataset.currentStock);
			setText('#viewItemCostPerUnit', '₱' + (dataset.costPerUnit || '0.00'));
			setText('#viewItemTotalCostValue', '₱' + (dataset.totalCostValue || '0.00'));
			setText('#viewItemLowThreshold', dataset.lowThreshold);
			setText('#viewItemCriticalThreshold', dataset.criticalThreshold);
			setText('#viewItemLastUpdated', 'Last Updated: ' + (dataset.lastUpdated || 'N/A'));

			// Populate status badge
			const statusBadge = viewItemModal.querySelector('#viewItemStatusBadge');
			if (statusBadge) {
				statusBadge.textContent = dataset.stockStatus || 'N/Access-Denied';
				statusBadge.className = 'status-badge'; // Reset classes
				if (dataset.stockStatusClass) {
					statusBadge.classList.add(dataset.stockStatusClass);
				}
			} else {
				console.warn("Element #viewItemStatusBadge not found.");
			}
		});
	}
	// --- END NEW ---

});