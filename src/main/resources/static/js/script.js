/**
 * Main script file for admin portal features.
 */
document.addEventListener('DOMContentLoaded', function() {

	const mainElement = document.querySelector('main');
	if (!mainElement) {
		console.error("Main element not found!");
		return;
	}

	// --- Logic to re-open modal on validation error ---
	// ... (code remains the same) ...
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
	// ... (code remains the same) ...
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
	// ... (code remains the same) ...
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
			} else {
				mainElement.removeAttribute('data-show-edit-customer-modal');
			}
		});
	}


	// --- Logic for Edit Admin Modal ---
	// ... (code remains the same) ...
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
			} else {
				mainElement.removeAttribute('data-show-edit-admin-modal');
			}
		});
	}


	// --- Logic for Add/Edit Inventory Item Modal ---
	// ... (code remains the same) ...
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
			// Clear validation highlights only if not reopened due to server error
			if (mainElement.dataset.showAddItemModal !== 'true') {
				itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			}
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if the modal wasn't explicitly flagged to stay open (due to validation)
			if (mainElement.dataset.showAddItemModal !== 'true') {
				itemForm.reset();
				itemIdInput.value = ''; // Ensure ID is cleared
				itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				// If it was kept open due to error, reset the flag for next time
				mainElement.removeAttribute('data-show-add-item-modal');
			}
		});
	}


	// --- Logic for Edit Product Modal ---
	// ... (code remains the same) ...
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
			// Updated parsing logic to match th:attr format 'id:qty,id:qty'
			if (dataset.ingredients && dataset.ingredients.length > 0) {
				try {
					ingredients = dataset.ingredients.split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							return { itemId: parts[0], quantity: parts[1] }; // itemID and quantity
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

			// Clear validation highlights only if not reopened due to server error
			if (mainElement.dataset.showEditProductModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger[role="alert"]:not([th\\:if*="."])');
				if (errorAlert) errorAlert.remove();
			}
		});

		editProductModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditProductModal !== 'true') {
				form.reset(); // Clear basic fields
				form.querySelector('#id').value = ''; // Clear hidden ID
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert) errorAlert.remove();
				if (ingredientsContainer) ingredientsContainer.innerHTML = ''; // Clear ingredients
			} else {
				mainElement.removeAttribute('data-show-edit-product-modal');
			}
		});
	}


	// --- Logic for View Product Modal ---
	// ... (code remains the same) ...
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

			// Populate Ingredients (using data-ingredients-view)
			const ingredientsListDiv = viewProductModal.querySelector('#viewProductIngredientsList');
			ingredientsListDiv.innerHTML = ''; // Clear previous
			let ingredientsDataView = [];
			// Use the data-ingredients-view attribute now
			if (dataset.ingredientsView && dataset.ingredientsView.length > 0) {
				try {
					// Ingredients format: 'Name:Qty:Unit,Name:Qty:Unit'
					ingredientsDataView = dataset.ingredientsView.split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							// Ensure we handle potential missing parts gracefully
							return {
								name: parts[0] || 'N/A',
								quantity: parts[1] || 'N/A',
								unit: parts[2] || 'N/A'
							};
						});
				} catch (e) {
					console.error("Error parsing ingredients data for view:", e, "Data:", dataset.ingredientsView);
					ingredientsDataView = [];
				}
			}


			if (ingredientsDataView.length > 0) {
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
				ingredientsDataView.forEach(ing => {
					const tr = document.createElement('tr');
					tr.innerHTML = `<td>${ing.name}</td><td>${ing.quantity}</td><td>${ing.unit}</td>`;
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
	// Helper function to add ingredient row (used by Add and Edit modals)
	function addIngredientRow(containerId, templateId, data = null) {
		// ... (function remains the same) ...
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

		// Update name attributes with the correct index
		newRowElement.querySelectorAll('[name]').forEach(input => {
			input.name = input.name.replace('[INDEX]', `[${index}]`);
		});

		// Populate if data is provided (used by Edit Product modal)
		if (data) {
			const select = newRowElement.querySelector('.ingredient-item');
			const quantityInput = newRowElement.querySelector('.ingredient-quantity');
			if (select) select.value = data.itemId; // itemID from parsed data
			if (quantityInput) quantityInput.value = data.quantity; // quantity from parsed data
		}

		containerDiv.appendChild(newRowElement);
	}


	// Helper function to remove ingredient row
	function removeIngredientRow(button) {
		// ... (function remains the same) ...
		const rowToRemove = button.closest('.ingredient-row');
		if (rowToRemove) {
			const container = rowToRemove.parentElement;
			rowToRemove.remove();
			renumberIngredientRows(container); // Renumber after removing
		}
	}

	// Helper function to renumber ingredient rows after deletion
	function renumberIngredientRows(container) {
		// ... (function remains the same) ...
		if (!container) return;
		const rows = container.querySelectorAll('.ingredient-row');
		rows.forEach((row, index) => {
			row.querySelectorAll('[name]').forEach(input => {
				// Regex to replace any digits within brackets [] with the new index
				input.name = input.name.replace(/\[\d+\]/g, `[${index}]`);
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


	// Event delegation for Remove buttons (covers both Add and Edit modals)
	document.addEventListener('click', function(event) {
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
		}
	});

	// --- REVISED AGAIN: MAX BUTTON LOGIC ---
	document.addEventListener('click', function(event) {
		const maxBtn = event.target.closest('.max-quantity-btn');
		if (!maxBtn) return; // Exit if the clicked element wasn't the Max button

		console.log("--- Max button clicked ---");

		const row = maxBtn.closest('tr');
		const quantityInput = row ? row.querySelector('.quantity-change-input') : null; // Check if row exists first

		// Ensure mainElement exists (should always exist based on check at top)
		if (!mainElement) {
			console.error("Main element not found inside Max button listener.");
			alert("Internal error: Configuration data missing.");
			return;
		}
		const inventoryStockJson = mainElement.dataset.inventoryStockMap; // Expects JSON '{"id":qty, "id":qty}'

		// More robust checks for elements
		if (!row) {
			console.error("Could not find parent table row (tr) for the Max button.");
			alert("Internal error: Could not identify product row.");
			return;
		}
		if (!quantityInput) {
			console.error("Could not find quantity input (.quantity-change-input) within the row.");
			alert("Internal error: Could not find quantity field.");
			return;
		}

		const ingredientsData = row.dataset.productIngredients; // 'id:qty,id:qty'

		// Log the raw data being read, *before* checking if they exist
		console.log("Raw ingredients data from row:", ingredientsData);
		console.log("Raw inventory stock JSON from main:", inventoryStockJson);

		// Check specifically if the required data attributes exist and are not empty strings
		if (!ingredientsData || ingredientsData.trim() === '') {
			console.error("Missing or empty product ingredients data (data-product-ingredients attribute) on the table row:", row);
			alert("Could not calculate maximum: Product recipe data is missing or empty.");
			return;
		}
		if (!inventoryStockJson || inventoryStockJson.trim() === '') {
			console.error("Missing or empty inventory stock map data (data-inventory-stock-map attribute) on the main element:", mainElement);
			alert("Could not calculate maximum: Inventory stock data is missing or empty.");
			return;
		}


		try {
			// 1. Parse Product Ingredients (id:qty,id:qty)
			console.log("Attempting to parse recipe:", ingredientsData);
			const recipe = ingredientsData.split(',')
				.map(item => item.trim())
				.filter(item => item.includes(':')) // Ensure it has the separator
				.map(item => {
					const [idStr, qtyNeededStr] = item.split(':');
					const parsedId = parseInt(idStr, 10);
					const parsedQty = parseFloat(qtyNeededStr);
					// Add more robust NaN checks
					if (isNaN(parsedId) || isNaN(parsedQty) || parsedQty <= 0) { // Also check qty > 0 here
						console.warn(`Invalid ingredient format skipped during parsing: id='${idStr}', qty='${qtyNeededStr}'`);
						return null;
					}
					return {
						itemId: parsedId,
						quantityNeeded: parsedQty
					};
				})
				.filter(item => item !== null); // Filter out the nulls from invalid entries

			console.log("Parsed recipe:", recipe);

			if (recipe.length === 0) {
				console.warn("Product recipe resulted in 0 valid ingredients after parsing.");
				alert("Product has no valid ingredients defined or ingredient quantities are invalid.");
				quantityInput.value = 0; // Set to 0 if no ingredients
				return;
			}

			// 2. Parse Inventory Stock Map (from JSON string)
			console.log("Attempting to parse inventory stock JSON:", inventoryStockJson);
			let inventoryStockMap = {};
			try {
				// Check if the string looks like a valid JSON object before parsing
				if (!inventoryStockJson.startsWith('{') || !inventoryStockJson.endsWith('}')) {
					console.error("Inventory stock map data is not a valid JSON object string:", inventoryStockJson);
					throw new Error("Invalid JSON format"); // Force catch block
				}
				inventoryStockMap = JSON.parse(inventoryStockJson);
				console.log("Parsed inventory stock map (raw keys):", inventoryStockMap);

				// Convert string keys from JSON to numbers and ensure values are numbers
				const numericKeyMap = {};
				let conversionError = false;
				for (const key in inventoryStockMap) {
					if (inventoryStockMap.hasOwnProperty(key)) {
						const numericKey = parseInt(key, 10);
						const numericValue = parseFloat(inventoryStockMap[key]); // Ensure value is a number too
						if (!isNaN(numericKey) && !isNaN(numericValue)) {
							numericKeyMap[numericKey] = numericValue;
						} else {
							console.warn(`Skipping invalid key or value in inventory map: key='${key}', value='${inventoryStockMap[key]}'`);
							conversionError = true; // Flag if any conversion failed
						}
					}
				}
				if (conversionError) {
					console.error("Inventory map contained non-numeric keys or values.");
					// Optionally alert the user or just proceed with valid data
				}
				inventoryStockMap = numericKeyMap; // Replace with the map having numeric keys and values
				console.log("Inventory stock map (numeric keys/values):", inventoryStockMap);

			} catch (jsonError) {
				console.error("Error parsing inventory stock JSON:", jsonError, "JSON String:", inventoryStockJson);
				alert("Could not calculate maximum: inventory data format is invalid. Check console for details.");
				quantityInput.value = ''; // Clear input on error
				return;
			}

			// Check if map is empty *after* successful parsing and key conversion
			if (Object.keys(inventoryStockMap).length === 0) {
				console.warn("Parsed inventory stock map resulted in an empty object. Original JSON:", inventoryStockJson);
				alert("Could not calculate maximum: Inventory data appears empty or invalid after processing.");
				quantityInput.value = 0;
				return;
			}


			// 3. Calculate Max Producable Quantity
			console.log("Calculating maximum possible units...");
			let maxPossible = Infinity;
			let limitingIngredientId = null; // Track which ingredient limits production

			for (const ingredient of recipe) {
				const availableStock = inventoryStockMap[ingredient.itemId];
				console.log(`Checking ingredient ID ${ingredient.itemId}: Need ${ingredient.quantityNeeded}, Available: ${availableStock}`);

				// Check if stock exists for this required ingredient
				if (availableStock === undefined || availableStock === null || isNaN(availableStock)) {
					console.error(`Inventory stock not found or invalid (NaN) for required ingredient ID: ${ingredient.itemId}. Cannot produce.`);
					maxPossible = 0; // Cannot make any if a required ingredient is missing from inventory map
					limitingIngredientId = ingredient.itemId;
					break;
				}

				// Check if stock is sufficient (already covered by Math.floor logic, but explicit check helps)
				if (availableStock < ingredient.quantityNeeded) {
					console.log(`Ingredient ID ${ingredient.itemId} stock (${availableStock}) is less than needed (${ingredient.quantityNeeded}).`);
					maxPossible = 0;
					limitingIngredientId = ingredient.itemId;
					break; // Not enough stock for even one unit
				}

				// Calculate how many *full* products can be made based *only* on this ingredient's stock
				const possibleUnits = Math.floor(availableStock / ingredient.quantityNeeded);
				console.log(`Ingredient ID ${ingredient.itemId} allows for ${possibleUnits} units.`);

				// Update maxPossible only if the current ingredient is more limiting
				if (possibleUnits < maxPossible) {
					maxPossible = possibleUnits;
					limitingIngredientId = ingredient.itemId;
				}
			}

			console.log(`Calculation complete: Max Possible = ${maxPossible}, Limited by Ingredient ID = ${limitingIngredientId}`);

			// 4. Update Input Field
			if (maxPossible === Infinity) { // This happens if recipe was empty or only had invalid ingredients
				console.warn("Max possible remained Infinity, likely due to empty/invalid recipe.");
				quantityInput.value = 0;
				alert("Could not determine maximum production quantity. Product recipe might be empty or invalid.");
			} else if (maxPossible < 0) { // Should not happen with Math.floor, but safeguard
				console.error(`Calculated max possible is negative (${maxPossible}), setting to 0.`);
				quantityInput.value = 0;
				alert("Calculation error resulted in negative quantity. Setting to 0.");
			}
			else {
				quantityInput.value = maxPossible; // Set the calculated maximum
				console.log(`Set quantity input to: ${maxPossible}`);
			}

		} catch (error) {
			console.error("Unexpected error during Max button calculation process:", error);
			alert("An unexpected error occurred while calculating the maximum quantity. Check console for details.");
			quantityInput.value = ''; // Clear input on unexpected error
		}
	});
	// --- END MAX BUTTON LOGIC REVISION ---

}); // End DOMContentLoaded

