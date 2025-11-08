document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-inventory.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element not found in admin-inventory.js!");
		return;
	}

	// --- addItemModal (Complex) - Refactored 'show.bs.modal' listener for Bug 2 ---
	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');

		const itemStockInfoContainer = addItemModal.querySelector('#itemStockInfoContainer');

		const itemStockHiddenInput = addItemModal.querySelector('#itemCurrentStockHidden');

		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');

			// This is the flag set by Thymeleaf on validation error
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';
			console.log("Add/Edit Item Modal 'show.bs.modal' event. IsEdit:", isEdit, "IsValidationReopen:", isValidationReopen);

			// --- BUG FIX 2 (ENHANCED) START ---
			// We must explicitly clear validation and reset the form on ANY fresh open
			// (i.e., any open that is NOT a validation reopen)
			if (!isValidationReopen) {
				console.log("Fresh open (Not validation reopen). Forcing form reset and clearing validation.");
				if (itemForm) {
					itemForm.reset();
					// 1. Remove invalid classes
					itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));

					// 2. Erase old error message text
					// This prevents script.js from re-adding 'is-invalid' on 'shown.bs.modal'
					itemForm.querySelectorAll('.invalid-feedback').forEach(el => {
						if (el.getAttribute('th:if') === null) { // Don't clear Thymeleaf-generated errors
							el.textContent = '';
							el.classList.remove('d-block');
						}
					});
				}
				// 3. Also clear any global error alerts that might be lingering
				const globalError = itemForm ? itemForm.querySelector('.alert.alert-danger') : null;
				if (globalError && globalError.getAttribute('th:if') === null) {
					globalError.remove();
				}
			}
			// --- BUG FIX 2 (ENHANCED) END ---

			if (isEdit && button.dataset) {
				// This is a fresh Edit, or a validation reopen of an Edit
				console.log("Populating modal for EDIT with data:", button.dataset);
				modalTitle.textContent = 'Edit Inventory Item';

				if (!isValidationReopen) {
					// Populate form only if it's a fresh edit
					// (if validation reopen, Thymeleaf already populated it)
					itemIdInput.value = button.dataset.id || '';
					itemNameInput.value = button.dataset.name || '';
					itemCategorySelect.value = button.dataset.categoryId || '';
					itemUnitSelect.value = button.dataset.unitId || '';
					if (itemStockHiddenInput) itemStockHiddenInput.value = button.dataset.currentStock || '0.00';
					itemLowThresholdInput.value = parseFloat(button.dataset.lowThreshold || '0').toFixed(0);
					itemCriticalThresholdInput.value = parseFloat(button.dataset.criticalThreshold || '0').toFixed(0);
					itemCostInput.value = button.dataset.cost || '';
				}

				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'block';

			} else {
				// This is a fresh Add, or a validation reopen of an Add
				console.log("Populating modal for ADD.");
				modalTitle.textContent = 'Add New Inventory Item';
				itemIdInput.value = ''; // Ensure ID is clear

				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'none';

				if (isValidationReopen) {
					console.log("Modal is reopening from ADD validation.");
					// Values are already set by Thymeleaf
				} else {
					// Fresh Add, form was already reset
					itemLowThresholdInput.value = '';
					itemCriticalThresholdInput.value = '';
				}
			}

			initThresholdSliders(addItemModal);
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';

			if (isValidationReopen) {
				console.log("Resetting showAddItemModal flag on hide.")
				mainElement.removeAttribute('data-show-add-item-modal');
			}

			// We no longer need to reset the form here,
			// the 'show.bs.modal' listener does it robustly before showing.
		});
	}

	// --- manageCategoriesModal (Complex) - Left as-is ---
	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addInvCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				console.log("Clearing Manage Categories modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showManageCategoriesModal flag on hide.")
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}

	// --- editInvCategoryModal (Simple) - REFACTORED ---
	initializeModalForm({
		modalId: 'editInvCategoryModal',
		formId: 'editInvCategoryForm',
		validationAttribute: 'data-show-edit-inv-category-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-inv-category-btn'
	});

	// --- manageUnitsModal (Complex) - Left as-is ---
	const manageUnitsModal = document.getElementById('manageUnitsModal');
	if (manageUnitsModal) {
		const form = manageUnitsModal.querySelector('#addUnitForm');

		manageUnitsModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageUnitsModal !== 'true') {
				console.log("Clearing Manage Units modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = form ? form.querySelector('.invalid-feedback.d-block') : null;
				if (globalError) globalError.textContent = '';
			} else {
				console.log("Resetting showManageUnitsModal flag on hide.")
				mainElement.removeAttribute('data-show-manage-units-modal');
			}
		});
	}

	// --- editUnitModal (Simple) - REFACTORED ---
	initializeModalForm({
		modalId: 'editUnitModal',
		formId: 'editUnitForm',
		validationAttribute: 'data-show-edit-unit-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-unit-btn'
	});

	// --- manageStockModal (Complex) - Left as-is ---
	const manageStockModal = document.getElementById('manageStockModal');
	if (manageStockModal) {
		manageStockModal.addEventListener('hidden.bs.modal', function() {
			console.log("Clearing Manage Stock (Inventory) modal inputs on hide.")
			manageStockModal.querySelectorAll('.stock-adjust-form input[type="number"]').forEach(input => {
				input.value = '';
			});
			mainElement.removeAttribute('data-show-manage-stock-modal');
		});
	}

	// --- viewItemModal (Custom) - Left as-is ---
	const viewItemModal = document.getElementById('viewItemModal');
	if (viewItemModal) {
		viewItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-item-btn')) {
				console.warn("View Item Modal opened without a valid row source.");
				return;
			}

			const dataset = button.dataset;
			console.log("Populating View Item Modal with data:", dataset);

			const setText = (id, value) => {
				const el = viewItemModal.querySelector(id);
				if (el) {
					el.textContent = value || 'N/A';
				} else {
					console.warn(`Element ${id} not found in viewItemModal.`);
				}
			};

			setText('#viewItemName', dataset.name);
			setText('#viewItemCategory', dataset.categoryName);
			setText('#viewItemUnit', dataset.unitName);
			setText('#viewItemCurrentStock', dataset.currentStock);
			setText('#viewItemCostPerUnit', '₱' + (dataset.costPerUnit || '0.00'));
			setText('#viewItemTotalCostValue', '₱' + (dataset.totalCostValue || '0.00'));
			setText('#viewItemLowThreshold', dataset.lowThreshold);
			setText('#viewItemCriticalThreshold', dataset.criticalThreshold);
			setText('#viewItemLastUpdated', 'Last Updated: ' + (dataset.lastUpdated || 'N/A'));

			const statusBadge = viewItemModal.querySelector('#viewItemStatusBadge');
			if (statusBadge) {
				statusBadge.textContent = dataset.stockStatus || 'N/Access-Denied';
				statusBadge.className = 'status-badge';
				if (dataset.stockStatusClass) {
					statusBadge.classList.add(dataset.stockStatusClass);
				}
			} else {
				console.warn("Element #viewItemStatusBadge not found.");
			}
		});
	}
});