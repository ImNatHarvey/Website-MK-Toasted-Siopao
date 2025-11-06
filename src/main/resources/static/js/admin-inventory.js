/**
 * JavaScript specific to the Admin Inventory page (admin/inventory.html)
 * Handles modal population for Add/Edit Item.
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
		const itemStockInput = addItemModal.querySelector('#itemCurrentStock');
		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');

			console.log("Add/Edit Item Modal 'show.bs.modal' event triggered."); // Debug

			if (isEdit && button.dataset) {
				console.log("Populating modal for EDIT with data:", button.dataset); // Debug
				modalTitle.textContent = 'Edit Inventory Item';
				itemIdInput.value = button.dataset.id || '';
				itemNameInput.value = button.dataset.name || '';
				itemCategorySelect.value = button.dataset.categoryId || '';
				itemUnitSelect.value = button.dataset.unitId || '';
				itemStockInput.value = button.dataset.currentStock || '0.00';
				itemLowThresholdInput.value = button.dataset.lowThreshold || '0.00';
				itemCriticalThresholdInput.value = button.dataset.criticalThreshold || '0.00';
				// UPDATED: Now required, so default to empty string if missing (shouldn't be)
				itemCostInput.value = button.dataset.cost || '';
			} else {
				console.log("Populating modal for ADD (resetting form)."); // Debug
				modalTitle.textContent = 'Add New Inventory Item';
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; // Ensure ID is cleared for Add
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
				statusBadge.textContent = dataset.stockStatus || 'N/A';
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

}); // End DOMContentLoaded for admin-inventory.js