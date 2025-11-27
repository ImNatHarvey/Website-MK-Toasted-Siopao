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

			if (button && button.classList.contains('edit-product-btn-from-view')) {
				const viewModal = document.getElementById('viewProductModal');
				const originalButton = viewModal ? viewModal.relatedTarget : null;
				dataset = originalButton ? originalButton.dataset : {};
			} else if (button && button.classList.contains('edit-product-btn')) {
				dataset = button.dataset;
			} else if (!isValidationReopen) {
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (editImageUploader) editImageUploader.resetUploader();
				return;
			}

			if (isValidationReopen) {
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
				modalTitle.textContent = 'Edit: ' + (dataset.name || 'Product');
				form.querySelector('#id').value = dataset.id || '';
				form.querySelector('#editProductNameModal').value = dataset.name || '';
				form.querySelector('#editProductCategoryModal').value = dataset.categoryId || '';
				form.querySelector('#editProductPriceModal').value = dataset.price || '0.00';
				form.querySelector('#editProductDescriptionModal').value = dataset.description || '';
				form.querySelector('#editLowThresholdInput').value = dataset.lowStockThreshold || '0';
				form.querySelector('#editCriticalThresholdInput').value = dataset.criticalStockThreshold || '0';
				
				// Populate Date Fields
				form.querySelector('#editCreatedDate').value = dataset.createdDate || '';
				form.querySelector('#editExpirationDays').value = dataset.expirationDays || '0';
				
				const productStatusSelect = form.querySelector('#editProductStatus');
				if (productStatusSelect) {
					productStatusSelect.value = dataset.productStatus || 'ACTIVE';
				}

				const imageUrl = dataset.imageUrl;
				editImageUploader.showPreview(imageUrl);

				if (ingredientsContainer) ingredientsContainer.innerHTML = '';

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
						console.error("Error parsing ingredients data:", e);
						ingredients = [];
					}
				}

				if (ingredientsContainer) {
					ingredients.forEach((ingData) => {
						addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal', ingData);
					});
				}
			}

			let isRecipeLocked = (dataset && dataset.recipeLocked === 'true');
			if (isValidationReopen && !dataset && form.querySelector('#id').value) isRecipeLocked = true;

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
				if (form) form.reset();
				if (form) form.querySelector('#id').value = '';
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) errorAlert.remove();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (editImageUploader) editImageUploader.resetUploader();
			} else {
				mainElement.removeAttribute('data-show-edit-product-modal');
			}
		});
	}
	
	function formatDate(dateString) {
		if (!dateString) return 'N/A';
		try {
			const date = new Date(dateString);
			if (isNaN(date)) return 'N/A';
			return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
		} catch (e) {
			return 'N/A';
		}
	}
	
	function calculateExpirationDate(createdDateString, expirationDays) {
		if (!createdDateString || !expirationDays || parseInt(expirationDays) <= 0) return 'No Expiration';
		try {
			const createdDate = new Date(createdDateString);
			if (isNaN(createdDate)) return 'N/A';
			
			const expDays = parseInt(expirationDays);
			// Use setDate to add days, handling month/year rollovers
			const expirationDate = new Date(createdDate);
			expirationDate.setDate(createdDate.getDate() + expDays);
			
			return formatDate(expirationDate.toISOString().split('T')[0]);
			
		} catch (e) {
			return 'N/A';
		}
	}

	const viewProductModal = document.getElementById('viewProductModal');
	if (viewProductModal) {
		viewProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-product-btn')) return;

			const dataset = button.dataset;
			viewProductModal.relatedTarget = button;

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
			
			// --- MODIFIED DATE FIELDS LOGIC ---
			const lastUpdated = dataset.stockLastUpdated || 'N/A';
			const lastUpdatedEl = viewProductModal.querySelector('#viewProductStockLastUpdated');
			if (lastUpdatedEl) {
				lastUpdatedEl.textContent = lastUpdated !== 'N/A' ? lastUpdated : 'N/A';
			}

			const createdDate = dataset.createdDate ? formatDate(dataset.createdDate) : 'N/A';
			const expirationDays = dataset.expirationDays || '0';
			
			// Prefer the pre-calculated expiration date from server if available
			let expirationDateText = 'No Expiration';
			if (dataset.expirationDate) {
				expirationDateText = formatDate(dataset.expirationDate);
			} else {
				// Fallback to calculation
				expirationDateText = calculateExpirationDate(dataset.createdDate, dataset.expirationDays);
			}
			
			viewProductModal.querySelector('#viewProductCreatedDate').textContent = createdDate;
			viewProductModal.querySelector('#viewProductExpirationDate').textContent = expirationDateText;
			
			const expDaysText = parseInt(expirationDays) > 0 ? `(${expirationDays} days)` : '';
			viewProductModal.querySelector('#viewProductExpirationDays').textContent = expDaysText;
			
			const expDateEl = viewProductModal.querySelector('#viewProductExpirationDate');
			if (expDateEl) {
				if (expirationDateText !== 'N/A' && expirationDateText !== 'No Expiration') {
					// Check if actually expired against today
					const now = new Date();
					now.setHours(0,0,0,0);
					
					let expDateObj = null;
					if(dataset.expirationDate) {
						expDateObj = new Date(dataset.expirationDate);
					} else if (dataset.createdDate && parseInt(expirationDays) > 0) {
						expDateObj = new Date(dataset.createdDate);
						expDateObj.setDate(expDateObj.getDate() + parseInt(expirationDays));
					}
					
					if (expDateObj) {
						expDateObj.setHours(0,0,0,0);
						if (expDateObj < now) {
							expDateEl.className = 'fw-bold text-danger';
						} else {
							expDateEl.className = 'fw-bold text-danger'; // User prefers red for expiration date
						}
					}
				} else {
					expDateEl.className = 'fw-bold text-success';
				}
			}
			// --- END MODIFIED DATE FIELDS LOGIC ---

			const ingredientsListDiv = viewProductModal.querySelector('#viewProductIngredientsList');
			const ingredientsHeading = viewProductModal.querySelector('#viewProductIngredientsHeading');
			const existingBadge = ingredientsHeading ? ingredientsHeading.querySelector('.badge') : null;
			if (existingBadge) existingBadge.remove();

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
					ingredientsDataView = [];
				}
			}

			const isRecipeLocked = (dataset.recipeLocked === 'true');
			if (isRecipeLocked && ingredientsHeading) {
				const lockBadge = document.createElement('span');
				lockBadge.className = 'badge bg-secondary ms-2';
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
				if (deleteInput) deleteInput.value = dataset.id;
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

			if (!isValidationReopen) {
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader();
				
				const dateInput = form.querySelector('#addCreatedDate');
				if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];
				
				initThresholdSliders(addProductModal);
			} else {
				initThresholdSliders(addProductModal);
				if (addImageUploader) addImageUploader.resetUploader();
			}
		});

		addProductModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddProductModal !== 'true') {
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) errorAlert.remove();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader();
			} else {
				mainElement.removeAttribute('data-show-add-product-modal');
			}
		});
	}

	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addCategoryForm');
		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
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
		if (!template || !containerDiv) return;

		const fragment = template.content ? template.content.cloneNode(true) : template.cloneNode(true).innerHTML;
		const tempDiv = document.createElement('div');
		if (typeof fragment === 'string') tempDiv.innerHTML = fragment;
		else tempDiv.appendChild(fragment);
		const newRowElement = tempDiv.querySelector('.ingredient-row');

		if (!newRowElement) return;

		const currentRowCount = containerDiv.querySelectorAll('.ingredient-row').length;
		const index = currentRowCount;

		newRowElement.querySelectorAll('[name]').forEach(input => {
			if (input.name) input.name = input.name.replace('[INDEX]', `[${index}]`);
		});

		if (data) {
			const select = newRowElement.querySelector('.ingredient-item');
			const quantityInput = newRowElement.querySelector('.ingredient-quantity');
			if (select) select.value = data.itemId || '';
			if (quantityInput) quantityInput.value = data.quantity || '';
		}
		containerDiv.appendChild(newRowElement);
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
				if (input.name) input.name = input.name.replace(/\[\d+\]/g, `[${index}]`);
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

		const row = maxBtn.closest('tr');
		const quantityInput = row ? row.querySelector('.quantity-change-input') : null;
		const productId = row ? row.dataset.productId : null;

		if (!row || !quantityInput || !productId) return;

		maxBtn.disabled = true;
		maxBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

		fetch(`/admin/products/calculate-max/${productId}`)
			.then(response => {
				if (!response.ok) throw new Error(response.statusText);
				return response.json();
			})
			.then(data => {
				if (data && data.maxQuantity !== undefined) {
					quantityInput.value = data.maxQuantity;
				}
			})
			.catch(error => {
				console.error("Error fetching max:", error);
				alert("Error calculating max.");
			})
			.finally(() => {
				maxBtn.disabled = false;
				maxBtn.textContent = 'Max';
			});
	});
	
	const manageStockModal = document.getElementById('manageStockModal');
	if (manageStockModal) {
		
		manageStockModal.addEventListener('change', function(e) {
			if (e.target && e.target.classList.contains('reason-category-select')) {
				const reason = e.target.value;
				const row = e.target.closest('tr'); 
				
				const addBtn = row.querySelector('.add-stock-btn');
				const deductBtn = row.querySelector('.deduct-stock-btn');
				const maxBtn = row.querySelector('.max-quantity-btn');
				
				// Default state (e.g. for Manual Adjust)
				addBtn.disabled = false;
				addBtn.classList.remove('disabled');
				addBtn.title = "Add stock (Production/Adjustment/Restock)";
				
				deductBtn.disabled = false;
				deductBtn.classList.remove('disabled');
				deductBtn.title = "Deduct stock (Waste/Adjustment)";
				
				if (maxBtn) {
					maxBtn.disabled = false;
					maxBtn.title = "Calculate max producible";
				}
				
				// Handle Logic based on updated Reason List
				if (reason === 'Production') {
					deductBtn.disabled = true;
					deductBtn.classList.add('disabled');
					deductBtn.title = "Production implies increasing stock.";
				} else if (reason === 'Restock') {
					// Restock (for products) implies adding, usually without ingredient consumption
					deductBtn.disabled = true;
					deductBtn.classList.add('disabled');
					deductBtn.title = "Restocking implies adding finished goods.";
				} else if (['Expired', 'Damaged', 'Waste'].includes(reason)) {
					// Waste reasons -> Deduct only
					addBtn.disabled = true;
					addBtn.classList.add('disabled');
					addBtn.title = "Waste reasons imply removing stock.";
					if (maxBtn) {
						maxBtn.disabled = true;
					}
				} 
				// 'Manual' allows both
			}
		});

		manageStockModal.addEventListener('hidden.bs.modal', function() {
			manageStockModal.querySelectorAll('.stock-adjust-form input[type="number"]').forEach(input => {
				input.value = '';
			});
			manageStockModal.querySelectorAll('.reason-category-select').forEach(select => {
				select.value = 'Manual'; // Reset to Manual as standard default
			});
			mainElement.removeAttribute('data-show-manage-stock-modal');
		});
	}
});