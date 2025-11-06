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
		// **** UPDATED: Get slider inputs ****
		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		// **** END UPDATE ****
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
				itemStockInput.value = button.dataset.currentStock || '0.00';
				// Use parseFloat for initial population from dataset, as it might be decimal
				itemLowThresholdInput.value = parseFloat(button.dataset.lowThreshold || '0').toFixed(0);
				itemCriticalThresholdInput.value = parseFloat(button.dataset.criticalThreshold || '0').toFixed(0);
				// UPDATED: Now required, so default to empty string if missing (shouldn't be)
				itemCostInput.value = button.dataset.cost || '';
			} else if (!isEdit && !isValidationReopen) {
				console.log("Populating modal for ADD (resetting form)."); // Debug
				modalTitle.textContent = 'Add New Inventory Item';
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; // Ensure ID is cleared for Add
				// --- UPDATED: Set to blank instead of '0' ---
				itemLowThresholdInput.value = ''; // Default to blank
				itemCriticalThresholdInput.value = ''; // Default to blank
				// --- END UPDATE ---
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				// We don't reset the form, Thymeleaf has already repopulated it.
				// We just need to ensure the title is correct.
				if (itemIdInput.value) {
					modalTitle.textContent = 'Edit Inventory Item';
				} else {
					modalTitle.textContent = 'Add New Inventory Item';
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

			// **** NEW: Initialize sliders ****
			initThresholdSliders(addItemModal);
			// **** END NEW ****
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


	// **** NEW FUNCTION: Initialize threshold sliders and inputs ****
	// **** MODIFIED to use parseInt and new critical logic ****
	function initThresholdSliders(modalElement) {
		console.log("Initializing threshold sliders for modal:", modalElement.id);
		const lowThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="low"]');
		const criticalThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="critical"]');

		if (!lowThresholdGroup || !criticalThresholdGroup) {
			console.warn("Could not find threshold groups in modal:", modalElement.id);
			return;
		}

		// IDs from inventory-modals.html
		const lowInput = lowThresholdGroup.querySelector('#itemLowThreshold');
		const lowSlider = lowThresholdGroup.querySelector('#itemLowThresholdSlider');
		const criticalInput = criticalThresholdGroup.querySelector('#itemCriticalThreshold');
		const criticalSlider = criticalThresholdGroup.querySelector('#itemCriticalThresholdSlider');


		if (!lowInput || !lowSlider || !criticalInput || !criticalSlider) {
			console.error("Missing one or more threshold inputs/sliders.");
			return;
		}

		// --- Helper Function ---
		// Syncs slider and input, adjusting slider max if needed
		const syncSliderAndInput = (input, slider) => {
			// --- UPDATED: Handle blank/NaN state ---
			let value = parseInt(input.value, 10); // Use parseInt
			let isBlank = isNaN(value);
			if (isBlank) {
				slider.value = 0; // Set slider to 0 if input is blank
				return; // Don't proceed
			}
			// --- END UPDATE ---

			// Adjust slider's max range if input value exceeds it (e.g., user types "200")
			let sliderMax = parseInt(slider.max, 10);
			if (value > sliderMax) {
				slider.max = value;
			}
			// But don't let slider max go below a sensible default like 100
			if (value < 100 && sliderMax > 100) {
				slider.max = 100;
			}

			slider.value = value;
			input.value = value; // Ensure no decimals
		};

		// --- Helper Function ---
		// Enforces Critical < Low
		const enforceThresholdLogic = () => {
			let lowValue = parseInt(lowInput.value, 10);
			let criticalValue = parseInt(criticalInput.value, 10);

			// --- UPDATED: If low is blank/NaN, don't do anything ---
			if (isNaN(lowValue)) {
				lowValue = 0; // Treat as 0 for max calculation
			}
			// --- END UPDATE ---
			if (isNaN(criticalValue)) {
				criticalValue = 0; // Treat as 0 for capping
			}

			// **** MODIFIED LOGIC ****
			// Critical max should be one less than low, but not less than 0.
			let criticalMax = (lowValue > 0) ? lowValue - 1 : 0;
			// **** END MODIFIED LOGIC ****

			// Set the max attribute for the critical input and slider
			criticalInput.max = criticalMax; // Set max on number input
			criticalSlider.max = criticalMax; // Set max on range slider

			// If critical is now higher than its new max, cap it
			if (criticalValue > criticalMax) {
				criticalInput.value = criticalMax;
				criticalSlider.value = criticalMax;
			}
		};

		// --- Initial Sync on Modal Show ---
		syncSliderAndInput(lowInput, lowSlider);
		syncSliderAndInput(criticalInput, criticalSlider);
		enforceThresholdLogic(); // Enforce logic right away

		// --- Event Listeners ---
		// Slider updates Input
		lowSlider.addEventListener('input', () => {
			lowInput.value = lowSlider.value;
			enforceThresholdLogic(); // Check logic
		});
		criticalSlider.addEventListener('input', () => {
			criticalInput.value = criticalSlider.value;
			// No need to check logic here, slider is already capped
		});

		// Input updates Slider
		lowInput.addEventListener('input', () => {
			syncSliderAndInput(lowInput, lowSlider);
			enforceThresholdLogic(); // Check logic
		});
		criticalInput.addEventListener('input', () => {
			syncSliderAndInput(criticalInput, criticalSlider);
			enforceThresholdLogic(); // Check logic (in case user types > low)
		});
	}
	// **** END NEW FUNCTION ****


}); // End DOMContentLoaded for admin-inventory.js