/**
 * Main script file for admin portal features.
 */
document.addEventListener('DOMContentLoaded', function() {

	const mainElement = document.querySelector('main');
	if (!mainElement) return; // Exit if no main element found

	// --- Logic to re-open modal on validation error ---
	const modalsToReopen = {
		'manageCategoriesModal': mainElement.dataset.showManageCategoriesModal, // Product categories
		'addProductModal': mainElement.dataset.showAddProductModal,
		'manageAdminsModal': mainElement.dataset.showManageAdminsModal,
		'addCustomerModal': mainElement.dataset.showAddCustomerModal,
		// NEW: Inventory Modals
		'addItemModal': mainElement.dataset.showAddItemModal,
		'manageCategoriesModal': mainElement.dataset.showManageCategoriesModal, // Inventory categories (shared ID, careful if separated later)
		'manageUnitsModal': mainElement.dataset.showManageUnitsModal
	};

	for (const modalId in modalsToReopen) {
		if (modalsToReopen[modalId] === 'true') {
			const modalElement = document.getElementById(modalId);
			if (modalElement) {
				const modalInstance = new bootstrap.Modal(modalElement);
				modalInstance.show();
			}
		}
	}


	// --- Logic for "View Customer" Modal ---
	const viewCustomerModal = document.getElementById('viewCustomerModal');
	if (viewCustomerModal) {
		viewCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const customerRow = button.closest('tr');
			if (!customerRow) return; // Exit if row not found

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
			viewCustomerModal.querySelector('#viewCustomerAddress2').textContent = address2.trim().length > 1 ? address2.trim() : 'No address provided.';
		});
	}

	// --- NEW: Logic for Add/Edit Inventory Item Modal ---
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

			if (isEdit) {
				// Populate form for editing
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
				// Reset form for adding
				modalTitle.textContent = 'Add New Inventory Item';
				itemForm.reset(); // Reset all form fields
				itemIdInput.value = ''; // Ensure hidden ID is empty
			}
		});
	}

});