/**
 * JavaScript specific to the Admin Products page (admin/products.html)
 * Handles modal population, recipe ingredients, max button, etc.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-products.js loaded"); // Confirm script is running

	// **** FIX IS HERE ****
	// We select the specific main element that has our data attribute,
	// not just the first <main> tag on the page.
	const mainElement = document.querySelector('main[data-inventory-stock-map]');
	// **** END OF FIX ****

	if (!mainElement) {
		console.error("Main element with data-inventory-stock-map not found in admin-products.js!");
		return;
	}

	// --- Logic for Edit Product Modal ---
	const editProductModal = document.getElementById('editProductModal');
	if (editProductModal) {
		const form = editProductModal.querySelector('#editProductForm');
		const modalTitle = editProductModal.querySelector('#editProductModalLabel');
		const ingredientsContainer = editProductModal.querySelector('#editIngredientsContainerModal');

		editProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			let dataset;

			if (button && button.classList.contains('edit-product-btn-from-view')) {
				const viewModal = document.getElementById('viewProductModal');
				const originalButton = viewModal ? viewModal.relatedTarget : null;
				dataset = originalButton ? originalButton.dataset : {};
			} else if (button && button.classList.contains('edit-product-btn')) {
				dataset = button.dataset;
			} else {
				console.warn("Edit Product modal opened without expected button source.");
				// Optionally reset form if opened unexpectedly
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				return;
			}

			console.log("Populating Edit Product Modal with data:", dataset); // Debug log

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
			if (dataset.ingredients && dataset.ingredients.length > 0) {
				try {
					ingredients = dataset.ingredients.split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							// Ensure quantity is parsed correctly, handle potential non-numeric values
							const quantity = parseFloat(parts[1]);
							return { itemId: parts[0], quantity: isNaN(quantity) ? '' : quantity };
						});
				} catch (e) {
					console.error("Error parsing ingredients data for edit:", e, "Data:", dataset.ingredients);
					ingredients = [];
				}
			}
			console.log("Parsed ingredients for Edit:", ingredients); // Debug log

			if (ingredientsContainer) {
				ingredients.forEach((ingData) => {
					addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal', ingData);
				});
			} else {
				console.warn("Ingredient container not found for edit modal.");
			}

			// Clear previous validation highlights unless it's being reopened by script.js
			// Check if the main element still has the data attribute flag
			if (mainElement.dataset.showEditProductModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger'); // General validation alert
				if (errorAlert && errorAlert.getAttribute('th:if') === null) { // Avoid removing Thymeleaf conditional alerts
					errorAlert.remove();
				}
			}
		});

		editProductModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if the modal wasn't explicitly flagged to stay open
			if (mainElement.dataset.showEditProductModal !== 'true') {
				console.log("Clearing Edit Product modal on hide (not validation reopen).") // Debug
				if (form) form.reset(); // Clear basic fields
				if (form) form.querySelector('#id').value = ''; // Clear hidden ID
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) { // Avoid removing Thymeleaf conditional alerts
					errorAlert.remove();
				}
				if (ingredientsContainer) ingredientsContainer.innerHTML = ''; // Clear ingredients
			} else {
				// If it WAS kept open, remove the flag for the next time it's closed normally
				console.log("Resetting showEditProductModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-edit-product-modal');
			}
		});
	}

	// --- Logic for View Product Modal ---
	const viewProductModal = document.getElementById('viewProductModal');
	if (viewProductModal) {
		viewProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-product-btn')) {
				console.warn("View modal triggered by non-view button?");
				return;
			}

			const dataset = button.dataset;
			viewProductModal.relatedTarget = button; // Store button for Edit button inside

			console.log("Populating View Product Modal with data:", dataset); // Debug log

			viewProductModal.querySelector('#viewProductModalLabel').textContent = 'Details for ' + (dataset.name || 'Product');
			viewProductModal.querySelector('#viewProductName').textContent = dataset.name || 'N/A';
			viewProductModal.querySelector('#viewProductCategory').textContent = dataset.categoryName || 'N/A';
			viewProductModal.querySelector('#viewProductPrice').textContent = dataset.price || '0.00';
			viewProductModal.querySelector('#viewProductDescription').textContent = dataset.description || 'No description available.';
			viewProductModal.querySelector('#viewProductImage').src = dataset.imageUrl || '/img/placeholder.jpg';

			const stockBadge = viewProductModal.querySelector('#viewProductStockStatusBadge');
			stockBadge.textContent = dataset.stockStatus || 'N/A';
			stockBadge.className = 'badge ms-2 ' + (dataset.stockStatusClass || 'bg-secondary');
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

			const ingredientsListDiv = viewProductModal.querySelector('#viewProductIngredientsList');
			ingredientsListDiv.innerHTML = '';
			let ingredientsDataView = [];
			if (dataset.ingredientsView && dataset.ingredientsView.length > 0) {
				try {
					ingredientsDataView = dataset.ingredientsView.split(',')
						.map(item => item.trim())
						.filter(item => item.includes(':'))
						.map(item => {
							const parts = item.split(':');
							return {
								name: parts[0] || 'N/A',
								quantity: parts[1] || 'N/A',
								unit: parts[2] || 'N/A'
							};
						});
				} catch (e) {
					console.error("Error parsing ingredients data for view:", e, "Data:", dataset.ingredientsView);
				}
			}
			console.log("Parsed ingredients for View:", ingredientsDataView); // Debug log

			if (ingredientsDataView.length > 0) {
				const table = document.createElement('table');
				table.className = 'table table-sm table-striped';
				table.innerHTML = `<thead><tr><th>Ingredient</th><th>Qty Needed</th><th>Unit</th></tr></thead><tbody></tbody>`;
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

			// Set up Delete form action inside view modal
			const deleteForm = viewProductModal.querySelector('.delete-product-form-from-view');
			const deleteInput = viewProductModal.querySelector('.view-product-id-for-delete'); // Assuming input exists for ID
			if (deleteForm && dataset.id) {
				deleteForm.action = `/admin/products/delete/${dataset.id}`; // Update action dynamically
				if (deleteInput) { // If using a hidden input for ID
					deleteInput.value = dataset.id;
				}
				console.log("Set delete form action to:", deleteForm.action);
			} else {
				console.warn("Could not set delete form action in view modal.");
			}
		});
	}

	// --- Logic for Add Product Modal (Clear on Hide) ---
	const addProductModal = document.getElementById('addProductModal');
	if (addProductModal) {
		const form = addProductModal.querySelector('#addProductForm');
		const ingredientsContainer = addProductModal.querySelector('#addIngredientsContainer');

		addProductModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if the modal wasn't explicitly flagged to stay open
			if (mainElement.dataset.showAddProductModal !== 'true') {
				console.log("Clearing Add Product modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
				if (ingredientsContainer) ingredientsContainer.innerHTML = ''; // Clear dynamic ingredients
			} else {
				// Reset the flag for the next time
				console.log("Resetting showAddProductModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-add-product-modal');
			}
		});
	}

	// --- Recipe Ingredient Management ---
	function addIngredientRow(containerId, templateId, data = null) {
		const template = document.getElementById(templateId);
		const containerDiv = document.getElementById(containerId);
		if (!template || !containerDiv) {
			console.warn("Cannot add ingredient row: container or template not found.", containerId, templateId);
			return;
		}

		// Use content of template if available
		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true).innerHTML; // Fallback for older browsers?
		const tempDiv = document.createElement('div'); // Create temporary div to append fragment/HTML string
		if (typeof fragment === 'string') {
			tempDiv.innerHTML = fragment;
		} else {
			tempDiv.appendChild(fragment);
		}
		const newRowElement = tempDiv.querySelector('.ingredient-row');


		if (!newRowElement) {
			console.error("Template did not contain '.ingredient-row' or content could not be parsed.");
			return;
		}

		const currentRowCount = containerDiv.querySelectorAll('.ingredient-row').length;
		const index = currentRowCount; // Index for the new row

		// Update name attributes with the correct index
		newRowElement.querySelectorAll('[name]').forEach(input => {
			if (input.name) { // Check if name attribute exists
				input.name = input.name.replace('[INDEX]', `[${index}]`);
			} else {
				console.warn("Input found without name attribute in template:", input);
			}
		});

		// Populate if data is provided (used by Edit Product modal)
		if (data) {
			const select = newRowElement.querySelector('.ingredient-item');
			const quantityInput = newRowElement.querySelector('.ingredient-quantity');
			if (select) select.value = data.itemId || ''; // Ensure setting to empty if no itemId
			if (quantityInput) quantityInput.value = data.quantity || ''; // Ensure setting to empty if no quantity
		}

		containerDiv.appendChild(newRowElement);
		console.log(`Added ingredient row to ${containerId} with index ${index}`); // Debug
	}

	function removeIngredientRow(button) {
		const rowToRemove = button.closest('.ingredient-row');
		if (rowToRemove) {
			const container = rowToRemove.parentElement;
			rowToRemove.remove();
			renumberIngredientRows(container); // Renumber after removing
		}
	}

	function renumberIngredientRows(container) {
		if (!container) return;
		const rows = container.querySelectorAll('.ingredient-row');
		rows.forEach((row, index) => {
			row.querySelectorAll('[name]').forEach(input => {
				if (input.name) {
					input.name = input.name.replace(/\[\d+\]/g, `[${index}]`);
				}
			});
		});
	}

	// Add Ingredient Button Listeners
	document.getElementById('addIngredientBtn')?.addEventListener('click', () => addIngredientRow('addIngredientsContainer', 'ingredientRowTemplate'));
	document.getElementById('addIngredientBtnEditModal')?.addEventListener('click', () => addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal'));

	// Event delegation for Remove buttons
	document.addEventListener('click', function(event) {
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
		}
	});

	// --- MAX BUTTON LOGIC (Copied from script.js, needs mainElement defined above) ---
	document.addEventListener('click', function(event) {
		const maxBtn = event.target.closest('.max-quantity-btn');
		if (!maxBtn) return;

		console.log("--- Max button clicked (product page) ---");

		const row = maxBtn.closest('tr'); // Assumes button is in a table row
		const quantityInput = row ? row.querySelector('.quantity-change-input') : null;
		const productId = row ? row.dataset.productId : null; // Get product ID from row

		if (!mainElement) {
			console.error("Main element not found inside Max button listener.");
			alert("Internal error: Configuration data missing.");
			return;
		}
		const inventoryStockJson = mainElement.dataset.inventoryStockMap; // JSON string '{"id":qty,...}'
		const ingredientsData = row ? row.dataset.productIngredients : null; // 'id:qty,id:qty' format

		// Robust checks
		if (!row) { /* ... error handling ... */ alert("Internal error: Could not identify product row."); return; }
		if (!quantityInput) { /* ... error handling ... */ alert("Internal error: Could not find quantity field."); return; }
		if (!productId) { /* ... error handling ... */ alert("Internal error: Product ID missing."); return; }
		if (!ingredientsData || ingredientsData.trim() === '') { /* ... error handling ... */ alert("Could not calculate maximum: Product recipe data is missing."); return; }
		if (!inventoryStockJson || inventoryStockJson.trim() === '') { /* ... error handling ... */ alert("Could not calculate maximum: Inventory stock data missing."); return; }


		try {
			// 1. Parse Product Ingredients
			console.log(`Parsing recipe for product ${productId}:`, ingredientsData);
			const recipe = ingredientsData.split(',')
				.map(item => item.trim())
				.filter(item => item.includes(':'))
				.map(item => {
					const [idStr, qtyNeededStr] = item.split(':');
					const parsedId = parseInt(idStr, 10);
					const parsedQty = parseFloat(qtyNeededStr);
					if (isNaN(parsedId) || isNaN(parsedQty) || parsedQty <= 0) {
						console.warn(`Invalid ingredient format skipped: id='${idStr}', qty='${qtyNeededStr}'`); return null;
					}
					return { itemId: parsedId, quantityNeeded: parsedQty };
				})
				.filter(item => item !== null);

			console.log("Parsed recipe:", recipe);
			if (recipe.length === 0) { /* ... error handling ... */ alert("Product has no valid ingredients."); quantityInput.value = 0; return; }

			// 2. Parse Inventory Stock Map
			console.log("Parsing inventory stock JSON:", inventoryStockJson);
			let inventoryStockMap = {};
			try {
				if (!inventoryStockJson.startsWith('{') || !inventoryStockJson.endsWith('}')) throw new Error("Invalid JSON format");
				const rawMap = JSON.parse(inventoryStockJson);
				for (const key in rawMap) {
					if (rawMap.hasOwnProperty(key)) {
						const numKey = parseInt(key, 10);
						const numVal = parseFloat(rawMap[key]);
						if (!isNaN(numKey) && !isNaN(numVal)) {
							inventoryStockMap[numKey] = numVal;
						} else { console.warn(`Skipping invalid inventory entry: key='${key}', value='${rawMap[key]}'`); }
					}
				}
			} catch (jsonError) { /* ... error handling ... */ console.error("Error parsing inventory stock JSON:", jsonError); alert("Inventory data error."); quantityInput.value = ''; return; }

			console.log("Parsed inventory map:", inventoryStockMap);
			if (Object.keys(inventoryStockMap).length === 0) { /* ... error handling ... */ alert("Inventory data appears empty."); quantityInput.value = 0; return; }


			// 3. Calculate Max Producable Quantity
			console.log("Calculating maximum...");
			let maxPossible = Infinity;
			for (const ingredient of recipe) {
				const availableStock = inventoryStockMap[ingredient.itemId];
				console.log(`Ingredient ID ${ingredient.itemId}: Need ${ingredient.quantityNeeded}, Available: ${availableStock}`);

				if (availableStock === undefined || availableStock === null || isNaN(availableStock)) {
					console.error(`Stock not found/invalid for ingredient ID ${ingredient.itemId}.`); maxPossible = 0; break;
				}
				if (availableStock <= 0 || availableStock < ingredient.quantityNeeded) { // Check if available is zero or less than needed
					console.log(`Insufficient stock for ingredient ID ${ingredient.itemId}.`); maxPossible = 0; break;
				}

				const possibleUnits = Math.floor(availableStock / ingredient.quantityNeeded);
				maxPossible = Math.min(maxPossible, possibleUnits);
			}

			console.log(`Max Possible = ${maxPossible}`);

			// 4. Update Input Field
			if (maxPossible === Infinity || maxPossible < 0) { // Handle empty recipe or calculation error
				console.warn("Max calculation result invalid, setting to 0.");
				quantityInput.value = 0;
			} else {
				quantityInput.value = maxPossible;
			}
			console.log(`Set quantity input to: ${quantityInput.value}`);

		} catch (error) { /* ... error handling ... */ console.error("Unexpected error during Max calculation:", error); alert("Calculation error."); quantityInput.value = ''; }
	}); // End Max Button Listener

}); // End DOMContentLoaded for admin-products.js