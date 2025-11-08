document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-products.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element #admin-content-wrapper not found in admin-products.js!");
		return;
	}

	setupImageUploader('addImageUploader');
	setupImageUploader('editImageUploader');

	const editProductModal = document.getElementById('editProductModal');
	if (editProductModal) {
		// ...
		const form = editProductModal.querySelector('#editProductForm');
		const modalTitle = editProductModal.querySelector('#editProductModalLabel');
		const ingredientsContainer = editProductModal.querySelector('#editIngredientsContainerModal');
		const recipeLockedWarning = editProductModal.querySelector('#editRecipeLockedWarning');
		const addIngredientBtn = editProductModal.querySelector('#addIngredientBtnEditModal');
		const editImageUploader = document.getElementById('editImageUploader');


		editProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			let dataset;

			const isValidationReopen = mainElement.dataset.showEditProductModal === 'true';
			console.log("Edit Product Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen);

			if (button && button.classList.contains('edit-product-btn-from-view')) {
				const viewModal = document.getElementById('viewProductModal');
				const originalButton = viewModal ? viewModal.relatedTarget : null;
				dataset = originalButton ? originalButton.dataset : {};
			} else if (button && button.classList.contains('edit-product-btn')) {
				dataset = button.dataset;
			} else if (!isValidationReopen) {
				console.warn("Edit Product modal opened without expected button source.");
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (editImageUploader) editImageUploader.resetUploader();
				return;
			}

			if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				const nameInput = form.querySelector('#editProductNameModal');
				modalTitle.textContent = 'Edit: ' + (nameInput.value || 'Product');

				const existingImageUrl = form.querySelector('#editProductImageUrlHidden').value;
				const removeImage = form.querySelector('#editProductRemoveImageHidden').value === 'true';

				if (removeImage) {
					editImageUploader.resetUploader();
				} else if (existingImageUrl) {
					editImageUploader.showPreview(existingImageUrl);
				} else {
					editImageUploader.resetUploader();
				}

			} else if (dataset) {
				console.log("Populating Edit Product Modal with data:", dataset);

				modalTitle.textContent = 'Edit: ' + (dataset.name || 'Product');
				form.querySelector('#id').value = dataset.id || '';
				form.querySelector('#editProductNameModal').value = dataset.name || '';
				form.querySelector('#editProductCategoryModal').value = dataset.categoryId || '';
				form.querySelector('#editProductPriceModal').value = dataset.price || '0.00';
				form.querySelector('#editProductDescriptionModal').value = dataset.description || '';
				form.querySelector('#editLowThresholdInput').value = dataset.lowStockThreshold || '0';
				form.querySelector('#editCriticalThresholdInput').value = dataset.criticalStockThreshold || '0';

				const imageUrl = dataset.imageUrl;

				editImageUploader.showPreview(imageUrl);

				if (ingredientsContainer) {
					ingredientsContainer.innerHTML = '';
				}

				let ingredients = [];
				if (dataset.ingredients && dataset.ingredients.length > 0) {
					try {
						ingredients = dataset.ingredients.split(',')
							.map(item => item.trim())
							.filter(item => item.includes(':'))
							.map(item => {
								const parts = item.split(':');
								const quantity = parseFloat(parts[1]);
								return { itemId: parts[0], quantity: isNaN(quantity) ? '' : quantity };
							});
					} catch (e) {
						console.error("Error parsing ingredients data for edit:", e, "Data:", dataset.ingredients);
						ingredients = [];
					}
				}
				console.log("Parsed ingredients for Edit:", ingredients);

				if (ingredientsContainer) {
					ingredients.forEach((ingData) => {
						addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal', ingData);
					});
				} else {
					console.warn("Ingredient container not found for edit modal.");
				}
			}

			let isRecipeLocked = (dataset && dataset.recipeLocked === 'true');

			if (isValidationReopen && !dataset) {
				if (form.querySelector('#id').value) {
					console.log("Validation reopen: Assuming recipe is locked because product exists.");
					isRecipeLocked = true;
				}
			}
			console.log("Is recipe locked? ", isRecipeLocked);


			if (isRecipeLocked) {
				if (recipeLockedWarning) recipeLockedWarning.style.display = 'block';
				if (addIngredientBtn) addIngredientBtn.style.display = 'none';
				if (ingredientsContainer) {
					ingredientsContainer.querySelectorAll('select, input').forEach(el => {
						el.readOnly = true;
						el.classList.add('form-control-readonly');
					});
					ingredientsContainer.querySelectorAll('.remove-ingredient-btn').forEach(btn => {
						btn.disabled = true;
						btn.style.display = 'none';
					});
				}
			} else {
				if (recipeLockedWarning) recipeLockedWarning.style.display = 'none';
				if (addIngredientBtn) addIngredientBtn.style.display = 'block';
				if (ingredientsContainer) {
					ingredientsContainer.querySelectorAll('select, input').forEach(el => {
						el.readOnly = false;
						el.classList.remove('form-control-readonly');
					});
					ingredientsContainer.querySelectorAll('.remove-ingredient-btn').forEach(btn => {
						btn.disabled = false;
						btn.style.display = 'block';
					});
				}
			}

			if (mainElement.dataset.showEditProductModal !== 'true') {
				form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form.querySelector('.alert.alert-danger');
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
			}

			initThresholdSliders(editProductModal);
		});

		editProductModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditProductModal !== 'true') {
				console.log("Clearing Edit Product modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelector('#id').value = '';
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (editImageUploader) editImageUploader.resetUploader();
			} else {
				console.log("Resetting showEditProductModal flag on hide.")
				mainElement.removeAttribute('data-show-edit-product-modal');
			}
		});
	}

	const viewProductModal = document.getElementById('viewProductModal');
	if (viewProductModal) {
		viewProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-product-btn')) {
				console.warn("View modal triggered by non-view button?");
				return;
			}

			const dataset = button.dataset;
			viewProductModal.relatedTarget = button;

			console.log("Populating View Product Modal with data:", dataset);

			viewProductModal.querySelector('#viewProductModalLabel').textContent = 'Details for ' + (dataset.name || 'Product');
			viewProductModal.querySelector('#viewProductName').textContent = dataset.name || 'N/A';
			viewProductModal.querySelector('#viewProductCategory').textContent = dataset.categoryName || 'N/A';
			viewProductModal.querySelector('#viewProductPrice').textContent = dataset.price || '0.00';
			viewProductModal.querySelector('#viewProductDescription').textContent = dataset.description || 'No description available.';
			viewProductModal.querySelector('#viewProductImage').src = dataset.imageUrl || '/img/placeholder.jpg';

			const stockBadge = viewProductModal.querySelector('#viewProductStockStatusBadge');
			stockBadge.textContent = dataset.stockStatus || 'N/A';
			stockBadge.className = 'status-badge ms-2 ' + (dataset.stockStatusClass || 'status-no_stock');

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
			const ingredientsHeading = viewProductModal.querySelector('#viewProductIngredientsHeading');

			const existingBadge = ingredientsHeading ? ingredientsHeading.querySelector('.badge') : null;
			if (existingBadge) {
				existingBadge.remove();
			}

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
			console.log("Parsed ingredients for View:", ingredientsDataView);

			const isRecipeLocked = (dataset.recipeLocked === 'true');
			if (isRecipeLocked && ingredientsHeading) {
				const lockBadge = document.createElement('span');
				lockBadge.className = 'badge bg-warning text-dark ms-2';
				lockBadge.textContent = 'Recipe Locked';
				ingredientsHeading.appendChild(lockBadge);
			}

			if (ingredientsDataView.length > 0) {
				const table = document.createElement('table');
				table.className = 'table table-sm table-striped mt-2';
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

			const deleteForm = viewProductModal.querySelector('.delete-product-form-from-view');
			const deleteInput = viewProductModal.querySelector('.view-product-id-for-delete');
			if (deleteForm && dataset.id) {
				deleteForm.action = `/admin/products/delete/${dataset.id}`;
				if (deleteInput) {
					deleteInput.value = dataset.id;
				}
				console.log("Set delete form action to:", deleteForm.action);
			} else {
				console.warn("Could not set delete form action in view modal.");
			}
		});
	}

	const addProductModal = document.getElementById('addProductModal');
	if (addProductModal) {
		const form = addProductModal.querySelector('#addProductForm');
		const ingredientsContainer = addProductModal.querySelector('#addIngredientsContainer');
		const addImageUploader = document.getElementById('addImageUploader');

		addProductModal.addEventListener('show.bs.modal', function() {
			const isValidationReopen = mainElement.dataset.showAddProductModal === 'true';
			console.log("Add Product Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen);

			if (!isValidationReopen) {
				console.log("Populating modal for ADD (resetting form).");
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader();

				const lowInput = form.querySelector('#addLowThresholdInput');
				const critInput = form.querySelector('#addCriticalThresholdInput');
				if (lowInput) lowInput.value = '';
				if (critInput) critInput.value = '';

				initThresholdSliders(addProductModal);
			} else {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				initThresholdSliders(addProductModal);
				if (addImageUploader) addImageUploader.resetUploader();
			}
		});

		addProductModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddProductModal !== 'true') {
				console.log("Clearing Add Product modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader();
			} else {
				console.log("Resetting showAddProductModal flag on hide.")
				mainElement.removeAttribute('data-show-add-product-modal');
			}
		});
	}

	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				console.log("Clearing Manage Categories (Add) form on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showManageCategoriesModal flag on hide.")
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}

	initializeModalForm({
		modalId: 'editCategoryModal',
		formId: 'editCategoryForm',
		validationAttribute: 'data-show-edit-category-modal',
		wrapperId: 'admin-content-wrapper',
		editTriggerClass: 'edit-category-btn'
	});

	function addIngredientRow(containerId, templateId, data = null) {
		const template = document.getElementById(templateId);
		const containerDiv = document.getElementById(containerId);
		if (!template || !containerDiv) {
			console.warn("Cannot add ingredient row: container or template not found.", containerId, templateId);
			return;
		}

		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true).innerHTML;
		const tempDiv = document.createElement('div');
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
		const index = currentRowCount;

		newRowElement.querySelectorAll('[name]').forEach(input => {
			if (input.name) {
				input.name = input.name.replace('[INDEX]', `[${index}]`);
			} else {
				console.warn("Input found without name attribute in template:", input);
			}
		});

		if (data) {
			const select = newRowElement.querySelector('.ingredient-item');
			const quantityInput = newRowElement.querySelector('.ingredient-quantity');
			if (select) select.value = data.itemId || '';
			if (quantityInput) quantityInput.value = data.quantity || '';
		}

		containerDiv.appendChild(newRowElement);
		console.log(`Added ingredient row to ${containerId} with index ${index}`);
	}

	function removeIngredientRow(button) {
		const rowToRemove = button.closest('.ingredient-row');
		if (rowToRemove) {
			const container = rowToRemove.parentElement;
			rowToRemove.remove();
			renumberIngredientRows(container);
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

	document.getElementById('addIngredientBtn')?.addEventListener('click', () => addIngredientRow('addIngredientsContainer', 'ingredientRowTemplate'));
	document.getElementById('addIngredientBtnEditModal')?.addEventListener('click', () => addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal'));

	document.addEventListener('click', function(event) {

		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
			return;
		}

		const maxBtn = event.target.closest('.max-quantity-btn');
		if (!maxBtn) return;

		console.log("--- Max button clicked (product page) ---");

		const row = maxBtn.closest('tr');
		const quantityInput = row ? row.querySelector('.quantity-change-input') : null;
		const productId = row ? row.dataset.productId : null;

		if (!row) { alert("Internal error: Could not identify product row."); return; }
		if (!quantityInput) { alert("Internal error: Could not find quantity field."); return; }
		if (!productId) { alert("Internal error: Product ID missing."); return; }

		maxBtn.disabled = true;
		maxBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

		fetch(`/admin/products/calculate-max/${productId}`)
			.then(response => {
				if (!response.ok) {
					throw new Error(`Server error: ${response.statusText}`);
				}
				return response.json();
			})
			.then(data => {
				if (data && data.maxQuantity !== undefined) {
					console.log(`Received max quantity for product ${productId}: ${data.maxQuantity}`);
					quantityInput.value = data.maxQuantity;
				} else {
					throw new Error("Invalid JSON response from server.");
				}
			})
			.catch(error => {
				console.error("Error fetching max quantity:", error);
				alert(`Could not calculate maximum: ${error.message}`);
				quantityInput.value = 0;
			})
			.finally(() => {
				maxBtn.disabled = false;
				maxBtn.textContent = 'Max';
			});
	});
});