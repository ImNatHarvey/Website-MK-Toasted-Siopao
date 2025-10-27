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
		'editProductModal': mainElement.dataset.showEditProductModal,
		'addItemModal': mainElement.dataset.showAddItemModal,
		'manageUnitsModal': mainElement.dataset.showManageUnitsModal,
		'manageStockModal': mainElement.dataset.showManageStockModal,
		'viewProductModal': mainElement.dataset.showViewProductModal // Added view product
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
	// ... (remains the same) ...
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
	// ... (remains the same) ...
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
	// ... (remains the same) ...
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
	// ... (remains the same) ...
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
	// ... (remains the same) ...
	const editProductModal = document.getElementById('editProductModal');
	if (editProductModal) {
		const form = editProductModal.querySelector('#editProductForm');
		const modalTitle = editProductModal.querySelector('#editProductModalLabel');
		const ingredientsContainer = editProductModal.querySelector('#editIngredientsContainerModal');
		const ingredientTemplate = document.getElementById('ingredientRowTemplateEditModal'); // Template FOR the edit modal

		editProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;

			// Handle opening from View Modal's Edit button
			let dataset;
			if (button && button.classList.contains('edit-product-btn-from-view')) {
				// Find the original button that opened the view modal
				const viewModal = document.getElementById('viewProductModal');
				const originalButton = viewModal ? viewModal.relatedTarget : null;
				dataset = originalButton ? originalButton.dataset : {};
			} else if (button && button.classList.contains('edit-product-btn')) {
				dataset = button.dataset; // Original Edit button
			} else {
				return; // Not triggered by a known edit button
			}


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
							// The ingredient data from edit button uses IDs, adjust if needed
							return { itemId: parts[0], quantity: parts[1] };
						});
				} catch (e) {
					console.error("Error parsing ingredients data for edit:", e, "Data:", dataset.ingredients);
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


	// --- NEW: Logic for View Product Modal ---
	const viewProductModal = document.getElementById('viewProductModal');
	if (viewProductModal) {
		viewProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget; // Button that triggered the modal
			if (!button || !button.classList.contains('view-product-btn')) {
				console.warn("View modal triggered by non-view button?");
				return; // Exit if not triggered by the correct button
			}

			const dataset = button.dataset;
			viewProductModal.relatedTarget = button; // Store the button for the Edit button inside the modal

			// Populate basic info
			viewProductModal.querySelector('#viewProductModalLabel').textContent = 'Details for ' + (dataset.name || 'Product');
			viewProductModal.querySelector('#viewProductName').textContent = dataset.name || 'N/A';
			viewProductModal.querySelector('#viewProductCategory').textContent = dataset.categoryName || 'N/A';
			viewProductModal.querySelector('#viewProductPrice').textContent = dataset.price || '0.00';
			viewProductModal.querySelector('#viewProductDescription').textContent = dataset.description || 'No description available.';
			viewProductModal.querySelector('#viewProductImage').src = dataset.imageUrl || '/img/placeholder.jpg';

			// Populate stock info
			const stockBadge = viewProductModal.querySelector('#viewProductStockStatusBadge');
			stockBadge.textContent = dataset.stockStatus || 'N/A';
			stockBadge.className = 'badge ms-2 ' + (dataset.stockStatusClass || 'bg-secondary'); // Reset classes
			viewProductModal.querySelector('#viewProductCurrentStock').textContent = dataset.currentStock || '0';
			viewProductModal.querySelector('#viewProductLowThreshold').textContent = dataset.lowStockThreshold || '0';
			viewProductModal.querySelector('#viewProductCriticalThreshold').textContent = dataset.criticalStockThreshold || '0';
			const lastUpdatedEl = viewProductModal.querySelector('#viewProductStockLastUpdated');
			if (dataset.stockLastUpdated) {
				lastUpdatedEl.textContent = 'Stock last updated: ' + dataset.stockLastUpdated;
				lastUpdatedEl.style.display = 'block';
			} else {
				lastUpdatedEl.style.display = 'none';
			}

			// Populate Ingredients
			const ingredientsListDiv = viewProductModal.querySelector('#viewProductIngredientsList');
			ingredientsListDiv.innerHTML = ''; // Clear previous
			let ingredientsData = [];
			if (dataset.ingredients && dataset.ingredients.length > 2) {
				try {
					// Ingredients format: '[IngredientName:Quantity:Unit, ...]'
					ingredientsData = dataset.ingredients.slice(1, -1).split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							return { name: parts[0], quantity: parts[1], unit: parts[2] };
						});
				} catch (e) {
					console.error("Error parsing ingredients data for view:", e, "Data:", dataset.ingredients);
					ingredientsData = [];
				}
			}


			if (ingredientsData.length > 0) {
				const table = document.createElement('table');
				table.className = 'table table-sm table-striped';
				table.innerHTML = `
                    <thead>
                        <tr>
                            <th>Ingredient Item</th>
                            <th>Quantity Needed</th>
                            <th>Unit</th>
                        </tr>
                    </thead>
                    <tbody>
                    </tbody>`;
				const tbody = table.querySelector('tbody');
				ingredientsData.forEach(ing => {
					const tr = document.createElement('tr');
					tr.innerHTML = `<td>${ing.name || 'N/A'}</td><td>${ing.quantity || 'N/A'}</td><td>${ing.unit || 'N/A'}</td>`;
					tbody.appendChild(tr);
				});
				ingredientsListDiv.appendChild(table);
			} else {
				ingredientsListDiv.innerHTML = '<p class="text-muted small">No ingredients assigned.</p>';
			}

			// Set up Edit button inside modal
			const editButtonFromView = viewProductModal.querySelector('.edit-product-btn-from-view');
			if (editButtonFromView) {
				// No need to copy data attributes here, the 'show.bs.modal' listener for editProductModal will handle it
			}

			// Set up Delete form inside modal
			const deleteForm = viewProductModal.querySelector('.delete-product-form-from-view');
			const deleteInput = viewProductModal.querySelector('.view-product-id-for-delete');
			if (deleteForm && deleteInput) {
				deleteInput.value = dataset.id || '';
				deleteForm.action = `/admin/products/delete/${dataset.id || 0}`; // Update form action
			}


		});
	}
	// --- END VIEW PRODUCT LOGIC ---


	// --- Recipe Ingredient Management ---
	// ... (addIngredientRow, removeIngredientRow, renumberIngredientRows remain the same) ...
	function addIngredientRow(containerId, templateId, data = null) {
		const template = document.getElementById(templateId);
		const containerDiv = document.getElementById(containerId);
		if (!template || !containerDiv) {
			console.warn("Cannot add ingredient row: container or template not found.", containerId, templateId);
			return;
		}

		const currentRowCount = containerDiv.querySelectorAll('.ingredient-row').length;
		const index = currentRowCount;

		// Use template.content if available, otherwise clone the template itself
		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true);
		// Find the .ingredient-row within the cloned fragment/node
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


		containerDiv.appendChild(newRowElement); // Append the actual .ingredient-row element
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


	// Add Ingredient Button (Edit Product Modal)
	const addIngredientBtnEditModal = document.getElementById('addIngredientBtnEditModal');
	if (addIngredientBtnEditModal) {
		addIngredientBtnEditModal.addEventListener('click', () => addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal'));
	}


	// Event delegation for Remove buttons
	document.addEventListener('click', function(event) {
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
		}
	});

}); // End DOMContentLoaded