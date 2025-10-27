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
		'editAdminModal': mainElement.dataset.showEditAdminModal,
		'editProductModal': mainElement.dataset.showEditProductModal, // <-- Flag for edit product
		'addItemModal': mainElement.dataset.showAddItemModal,
		'manageUnitsModal': mainElement.dataset.showManageUnitsModal,
		'manageStockModal': mainElement.dataset.showManageStockModal
	};

	for (const modalId in modalsToReopen) {
		if (modalsToReopen[modalId] === 'true') {
			const modalElement = document.getElementById(modalId);
			if (modalElement) {
				if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
					try {
						const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
						modalInstance.show();
					} catch (e) { console.error(`Error showing modal ${modalId}:`, e); }
				} else { console.error('Bootstrap Modal component not found.'); }
			} else { console.warn(`Modal element ${modalId} not found.`); }
		}
	}

	// --- Logic for "View Customer" Modal ---
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
	const editCustomerModal = document.getElementById('editCustomerModal');
	if (editCustomerModal) {
		const form = editCustomerModal.querySelector('#editCustomerForm');
		const modalTitle = editCustomerModal.querySelector('#editCustomerModalLabel');

		editCustomerModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
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

	// --- Logic for Edit Admin Modal ---
	const editAdminModal = document.getElementById('editAdminModal');
	if (editAdminModal) {
		const form = editAdminModal.querySelector('#editAdminForm');
		const modalTitle = editAdminModal.querySelector('#editAdminModalLabel');

		editAdminModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('edit-admin-btn')) {
				return;
			}
			const dataset = button.dataset;
			modalTitle.textContent = 'Edit: ' + dataset.firstName + ' ' + dataset.lastName;
			form.querySelector('#id').value = dataset.id || '';
			form.querySelector('#editAdminFirstName').value = dataset.firstName || '';
			form.querySelector('#editAdminLastName').value = dataset.lastName || '';
			form.querySelector('#editAdminUsername').value = dataset.username || '';
			form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
		});

		editAdminModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditAdminModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert) errorAlert.remove();
			}
		});
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
			itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddItemModal !== 'true') {
				itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			}
		});
	}

	// --- Logic for Edit Product Modal ---
	const editProductModal = document.getElementById('editProductModal');
	if (editProductModal) {
		const form = editProductModal.querySelector('#editProductForm');
		const modalTitle = editProductModal.querySelector('#editProductModalLabel');
		const ingredientsContainer = editProductModal.querySelector('#editIngredientsContainerModal');
		const ingredientTemplate = document.getElementById('ingredientRowTemplateEditModal'); // Template FOR the edit modal

		editProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;

			if (!button || !button.classList.contains('edit-product-btn')) {
				return;
			}

			const dataset = button.dataset;
			modalTitle.textContent = 'Edit: ' + (dataset.name || 'Product');
			form.querySelector('#id').value = dataset.id || '';
			form.querySelector('#editProductNameModal').value = dataset.name || '';
			form.querySelector('#editProductCategoryModal').value = dataset.categoryId || '';
			form.querySelector('#editProductPriceModal').value = dataset.price || '0.00';
			form.querySelector('#editProductDescriptionModal').value = dataset.description || '';
			form.querySelector('#editProductImageUrlModal').value = dataset.imageUrl || '';
			form.querySelector('#editLowThresholdModal').value = dataset.lowStockThreshold || '0';
			form.querySelector('#editCriticalThresholdModal').value = dataset.criticalStockThreshold || '0';

			if (ingredientsContainer) {
				ingredientsContainer.innerHTML = ''; // Clear previous rows
			}

			let ingredients = [];
			if (dataset.ingredients && dataset.ingredients.length > 2) {
				try {
					ingredients = dataset.ingredients.slice(1, -1).split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							return { itemId: parts[0], quantity: parts[1] };
						});
				} catch (e) {
					console.error("Error parsing ingredients data:", e);
					ingredients = [];
				}
			}

			if (ingredientTemplate && ingredientsContainer) {
				ingredients.forEach((ingData, index) => {
					addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal', ingData); // Pass data to populate
				});
			} else {
				console.warn("Ingredient container or template not found for edit modal.");
			}

			form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			const errorAlert = form.querySelector('.alert.alert-danger[role="alert"]:not([th\\:if*="."])');
			if (errorAlert) errorAlert.remove();
		});

		editProductModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditProductModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert) errorAlert.remove();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
			}
		});
	}


	// --- Recipe Ingredient Management ---
	// Modified to accept optional data for population
	function addIngredientRow(containerId, templateId, data = null) {
		const template = document.getElementById(templateId);
		const containerDiv = document.getElementById(containerId);
		if (!template || !containerDiv) {
			console.warn("Cannot add ingredient row: container or template not found.", containerId, templateId);
			return;
		}

		const currentRowCount = containerDiv.querySelectorAll('.ingredient-row').length;
		const index = currentRowCount;

		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true);
		const newRowElement = fragment.querySelector('.ingredient-row');

		if (!newRowElement) {
			console.error("Template did not contain '.ingredient-row'");
			return;
		}

		newRowElement.querySelectorAll('[name]').forEach(input => {
			input.name = input.name.replace('[INDEX]', `[${index}]`);
		});

		// Populate if data is provided (used by Edit Product modal)
		if (data) {
			const select = newRowElement.querySelector('.ingredient-item');
			const quantityInput = newRowElement.querySelector('.ingredient-quantity');
			if (select) select.value = data.itemId;
			if (quantityInput) quantityInput.value = data.quantity;
		}


		containerDiv.appendChild(newRowElement);
	}


	function removeIngredientRow(button) {
		const rowToRemove = button.closest('.ingredient-row');
		if (rowToRemove) {
			rowToRemove.remove();
			// OPTIONAL: Renumber fields if strict indexing is required by backend
			// renumberIngredientRows(rowToRemove.parentElement);
		}
	}

	// Optional function to renumber rows after deletion
	function renumberIngredientRows(container) {
		if (!container) return;
		const rows = container.querySelectorAll('.ingredient-row');
		rows.forEach((row, index) => {
			row.querySelectorAll('[name]').forEach(input => {
				input.name = input.name.replace(/\[\d+\]/, `[${index}]`);
			});
		});
	}


	// Add Ingredient Button (Add Product Modal)
	const addIngredientBtn = document.getElementById('addIngredientBtn');
	if (addIngredientBtn) {
		addIngredientBtn.addEventListener('click', () => addIngredientRow('addIngredientsContainer', 'ingredientRowTemplate'));
	}

	// --- REMOVED OBSOLETE LISTENER ---
	// const addIngredientBtnEdit = document.getElementById('addIngredientBtnEdit');
	// if (addIngredientBtnEdit) {
	// 	addIngredientBtnEdit.addEventListener('click', () => addIngredientRow('editIngredientsContainer', 'ingredientRowTemplateEdit'));
	// }

	// --- CORRECTED LISTENER FOR EDIT MODAL ---
	const addIngredientBtnEditModal = document.getElementById('addIngredientBtnEditModal');
	if (addIngredientBtnEditModal) {
		// Use the correct container and template IDs for the EDIT modal
		addIngredientBtnEditModal.addEventListener('click', () => addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal'));
	}


	// Event delegation for Remove buttons
	document.addEventListener('click', function(event) {
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
		}
	});

});