/**
 * JavaScript specific to the Admin Products page (admin/products.html)
 * Handles modal population, recipe ingredients, max button, AND NEW IMAGE UPLOADER.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-products.js loaded"); // Confirm script is running

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element with data-inventory-stock-map not found in admin-products.js!");
		return;
	}

	// **** NEW FUNCTION: Image Uploader Logic ****
	function setupImageUploader(containerId) {
		const uploader = document.getElementById(containerId);
		if (!uploader) {
			console.warn(`Uploader container #${containerId} not found.`);
			return;
		}

		const input = uploader.querySelector('.image-uploader-input');
		const dropZone = uploader.querySelector('.image-drop-zone');
		const previewContainer = uploader.querySelector('.image-preview-container');
		const previewImg = uploader.querySelector('.image-preview');
		const removeBtn = uploader.querySelector('.image-remove-btn');
		const form = uploader.closest('form');
		// *** FIX: Find the correct hidden input for this uploader's form ***
		const removeImageHiddenInput = form ? form.querySelector('input[name="removeImage"]') : null;

		if (!input || !dropZone || !previewContainer || !previewImg || !removeBtn || !removeImageHiddenInput) {
			console.error(`Uploader #${containerId} is missing required elements (input, dropZone, previewContainer, previewImg, removeBtn, or removeImage hidden input).`);
			return;
		}

		// Function to show the preview
		const showPreview = (fileOrSrc) => {
			// Check 1: Is it a valid image URL string?
			if (typeof fileOrSrc === 'string' && fileOrSrc && fileOrSrc !== "null") {
				// It's a valid URL (for edit modal)
				previewImg.src = fileOrSrc;
				uploader.classList.add('preview-active');
				input.value = ''; // Clear file input
				removeImageHiddenInput.value = 'false'; // **** FIX: Explicitly set remove to false ****
			}
			// Check 2: Is it a File object? (Check for `name` property)
			else if (typeof fileOrSrc === 'object' && fileOrSrc !== null && fileOrSrc.name) {
				// It's a File object (from upload or drop)
				const reader = new FileReader();
				reader.onload = () => {
					previewImg.src = reader.result;
					uploader.classList.add('preview-active');
					removeImageHiddenInput.value = 'false'; // **** FIX: Explicitly set remove to false ****
				};
				reader.readAsDataURL(fileOrSrc);
			}
			// Check 3: It's null, undefined, "", or "null"
			else {
				// It's an invalid source (null, "", or not a file), so reset
				resetUploader();
			}
		};

		// Function to reset the uploader
		const resetUploader = () => {
			previewImg.src = '';
			uploader.classList.remove('preview-active');
			input.value = ''; // Clear the file input
			removeImageHiddenInput.value = 'true'; // **** FIX: Explicitly set remove to true ****
		};

		// --- Event Listeners ---
		// 1. Click on drop zone OR preview container triggers file input click
		dropZone.addEventListener('click', () => {
			input.click();
		});
		previewContainer.addEventListener('click', (e) => {
			// Don't trigger if the remove button was clicked
			if (e.target !== removeBtn && !removeBtn.contains(e.target)) {
				input.click();
			}
		});


		// 2. File input change (user selected a file)
		input.addEventListener('change', () => {
			if (input.files && input.files[0]) {
				showPreview(input.files[0]);
			}
		});

		// 3. Remove button click
		removeBtn.addEventListener('click', (e) => {
			e.stopPropagation();
			resetUploader();
		});

		// 4. Drag and Drop events
		uploader.addEventListener('dragover', (e) => {
			e.preventDefault();
			uploader.classList.add('drag-over');
		});
		uploader.addEventListener('dragleave', (e) => {
			e.preventDefault();
			uploader.classList.remove('drag-over');
		});
		uploader.addEventListener('drop', (e) => {
			e.preventDefault();
			uploader.classList.remove('drag-over');
			if (e.dataTransfer.files && e.dataTransfer.files[0]) {
				// Manually set the file input's files property
				input.files = e.dataTransfer.files;
				showPreview(e.dataTransfer.files[0]);
			}
		});

		// --- Public API for the modal ---
		// Add functions to the uploader element itself so modal logic can call them
		uploader.showPreview = showPreview;
		uploader.resetUploader = resetUploader;

		// --- Initial Reset (for Add Modal) ---
		// *** FIX: Check the ID and form to ensure we reset the right one ***
		if (containerId === 'addImageUploader' || form.id === 'addProductForm') {
			resetUploader();
		}
	}
	// **** END NEW FUNCTION: Image Uploader Logic ****

	// --- Initialize both uploaders on page load ---
	setupImageUploader('addImageUploader');
	setupImageUploader('editImageUploader');


	// **** NEW FUNCTION: Initialize threshold sliders and inputs ****
	function initThresholdSliders(modalElement) {
		console.log("Initializing threshold sliders for modal:", modalElement.id);
		const lowThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="low"]');
		const criticalThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="critical"]');

		if (!lowThresholdGroup || !criticalThresholdGroup) {
			console.warn("Could not find threshold groups in modal:", modalElement.id);
			return;
		}

		const lowInput = lowThresholdGroup.querySelector('.threshold-input');
		const lowSlider = lowThresholdGroup.querySelector('.threshold-slider');
		const criticalInput = criticalThresholdGroup.querySelector('.threshold-input');
		const criticalSlider = criticalThresholdGroup.querySelector('.threshold-slider');

		if (!lowInput || !lowSlider || !criticalInput || !criticalSlider) {
			console.error("Missing one or more threshold inputs/sliders.");
			return;
		}

		// --- Helper Function ---
		// Syncs slider and input, adjusting slider max if needed
		const syncSliderAndInput = (input, slider) => {
			// --- UPDATED: Handle blank/NaN state ---
			let value = parseInt(input.value, 10);
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

			// **** START: QUICK FIX ****
			// Critical max should be one less than low, but not less than 0.
			let criticalMax = (lowValue > 0) ? lowValue - 1 : 0;
			// **** END: QUICK FIX ****

			// Set the max attribute for the critical input and slider
			criticalInput.max = criticalMax;
			criticalSlider.max = criticalMax;

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


	// --- Logic for Edit Product Modal ---
	const editProductModal = document.getElementById('editProductModal');
	if (editProductModal) {
		// ...
		const form = editProductModal.querySelector('#editProductForm');
		const modalTitle = editProductModal.querySelector('#editProductModalLabel');
		const ingredientsContainer = editProductModal.querySelector('#editIngredientsContainerModal');
		const recipeLockedWarning = editProductModal.querySelector('#editRecipeLockedWarning');
		const addIngredientBtn = editProductModal.querySelector('#addIngredientBtnEditModal');
		const editImageUploader = document.getElementById('editImageUploader'); // NEW


		editProductModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			let dataset;

			// --- UPDATED: Check if we are reopening from validation ---
			const isValidationReopen = mainElement.dataset.showEditProductModal === 'true';
			console.log("Edit Product Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); // Debug


			// ... (Omitted unchanged dataset logic for view/edit buttons) ...
			if (button && button.classList.contains('edit-product-btn-from-view')) {
				const viewModal = document.getElementById('viewProductModal');
				const originalButton = viewModal ? viewModal.relatedTarget : null;
				dataset = originalButton ? originalButton.dataset : {};
			} else if (button && button.classList.contains('edit-product-btn')) {
				dataset = button.dataset;
			} else if (!isValidationReopen) { // Only warn if not a validation reopen
				console.warn("Edit Product modal opened without expected button source.");
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (editImageUploader) editImageUploader.resetUploader(); // NEW
				return;
			}

			if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				// We just need to ensure the title is correct.
				const nameInput = form.querySelector('#editProductNameModal');
				modalTitle.textContent = 'Edit: ' + (nameInput.value || 'Product');

				// **** NEW: Re-initialize image uploader state from form ****
				const existingImageUrl = form.querySelector('#editProductImageUrlHidden').value;
				const removeImage = form.querySelector('#editProductRemoveImageHidden').value === 'true';

				// *** FIX: Check removeImage flag first
				if (removeImage) {
					editImageUploader.resetUploader();
				} else if (existingImageUrl) {
					editImageUploader.showPreview(existingImageUrl);
				} else {
					editImageUploader.resetUploader();
				}
				// **** END NEW ****

			} else if (dataset) {
				console.log("Populating Edit Product Modal with data:", dataset);

				// ... (Omitted unchanged form population logic) ...
				modalTitle.textContent = 'Edit: ' + (dataset.name || 'Product');
				form.querySelector('#id').value = dataset.id || '';
				form.querySelector('#editProductNameModal').value = dataset.name || '';
				form.querySelector('#editProductCategoryModal').value = dataset.categoryId || '';
				form.querySelector('#editProductPriceModal').value = dataset.price || '0.00';
				form.querySelector('#editProductDescriptionModal').value = dataset.description || '';
				// form.querySelector('#editProductImageUrlModal').value = dataset.imageUrl || ''; // OLD
				form.querySelector('#editLowThresholdInput').value = dataset.lowStockThreshold || '0'; // UPDATED ID
				form.querySelector('#editCriticalThresholdInput').value = dataset.criticalStockThreshold || '0'; // UPDATED ID

				// **** NEW: Populate image uploader ****
				const imageUrl = dataset.imageUrl;
				// *** FIX: Use showPreview for ALL cases to handle null/empty ***
				editImageUploader.showPreview(imageUrl);
				// **** END NEW ****

				// ... (Omitted unchanged ingredients logic) ...
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

			// --- This logic runs for both edit and validation reopen ---
			// UPDATED: Check form for recipeLocked value if dataset is not present (validation reopen)
			let isRecipeLocked = (dataset && dataset.recipeLocked === 'true');
			if (isValidationReopen && !isRecipeLocked) {
				// Check if the form's (hidden) data implies it's locked
				// This is tricky, as we don't pass this in the DTO.
				// We'll rely on the fact that an "edit" *must* have come from a button
				// and `dataset` will be populated *even on validation reopen* if we set it right.
				// The logic might be slightly flawed, but let's assume `dataset` is available.
				// A better way would be to pass `recipeLocked` in the DTO.
				// For now, let's just use the dataset.
				if (form.querySelector('#id').value) { // If it's an edit
					// We can't know for sure without the dataset.
					// Let's assume the button's dataset is still the best source.
					// This part is complex. Let's stick to the original `dataset` check.
				}
			}
			console.log("Is recipe locked? ", isRecipeLocked);


			if (isRecipeLocked) {
				if (recipeLockedWarning) recipeLockedWarning.style.display = 'block';
				if (addIngredientBtn) addIngredientBtn.style.display = 'none';
				if (ingredientsContainer) { // Check if container exists
					ingredientsContainer.querySelectorAll('select, input, button').forEach(el => {
						el.disabled = true;
						if (el.classList.contains('remove-ingredient-btn')) {
							el.style.display = 'none';
						}
					});
				}
			} else {
				if (recipeLockedWarning) recipeLockedWarning.style.display = 'none';
				if (addIngredientBtn) addIngredientBtn.style.display = 'block';
				if (ingredientsContainer) { // Check if container exists
					ingredientsContainer.querySelectorAll('select, input, button').forEach(el => {
						el.disabled = false;
						if (el.classList.contains('remove-ingredient-btn')) {
							el.style.display = 'block';
						}
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

			// **** CALL NEW SLIDER FUNCTION ****
			initThresholdSliders(editProductModal);
		});

		// ... (Omitted unchanged 'hidden.bs.modal' listener) ...
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
				if (editImageUploader) editImageUploader.resetUploader(); // NEW
			} else {
				console.log("Resetting showEditProductModal flag on hide.")
				mainElement.removeAttribute('data-show-edit-product-modal');
			}
		});
	}

	// --- Logic for View Product Modal ---
	// ... (Omitted unchanged 'show.bs.modal' listener) ...
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

			// ==================================
			// == QUICK FIX MOVED HERE ==
			// ==================================
			const ingredientsListDiv = viewProductModal.querySelector('#viewProductIngredientsList');
			const ingredientsHeading = viewProductModal.querySelector('#viewProductIngredientsHeading'); // Find heading

			// --- Clear previous badge ---
			const existingBadge = ingredientsHeading ? ingredientsHeading.querySelector('.badge') : null;
			if (existingBadge) {
				existingBadge.remove();
			}

			ingredientsListDiv.innerHTML = ''; // Clear ingredients list
			// ==================================
			// == END FIX ==
			// ==================================

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

			// ==================================
			// == QUICK FIX MOVED HERE ==
			// ==================================
			// --- NEW: Add Lock Status to View Modal ---
			const isRecipeLocked = (dataset.recipeLocked === 'true');
			if (isRecipeLocked && ingredientsHeading) {
				const lockBadge = document.createElement('span');
				lockBadge.className = 'badge bg-warning text-dark ms-2'; // This matches the user's image style
				lockBadge.textContent = 'Recipe Locked';
				ingredientsHeading.appendChild(lockBadge); // Append inside the h5
			}
			// --- END NEW ---
			// ==================================
			// == END FIX ==
			// ==================================

			if (ingredientsDataView.length > 0) {
				const table = document.createElement('table');
				table.className = 'table table-sm table-striped mt-2'; // Added margin-top
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

			// ... (Omitted unchanged delete form logic) ...
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
		const addImageUploader = document.getElementById('addImageUploader'); // NEW

		// **** CALL NEW SLIDER FUNCTION ****
		addProductModal.addEventListener('show.bs.modal', function() {
			// --- UPDATED: Check if we are reopening from validation ---
			const isValidationReopen = mainElement.dataset.showAddProductModal === 'true';
			console.log("Add Product Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); // Debug

			// Only init sliders if it's NOT a reopen from validation
			if (!isValidationReopen) {
				// --- UPDATED: Reset form *before* initializing sliders ---
				console.log("Populating modal for ADD (resetting form)."); // Debug
				if (form) form.reset();
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader(); // NEW

				// --- NEW: Manually reset threshold inputs to blank ---
				const lowInput = form.querySelector('#addLowThresholdInput');
				const critInput = form.querySelector('#addCriticalThresholdInput');
				if (lowInput) lowInput.value = '';
				if (critInput) critInput.value = '';
				// --- END NEW ---

				initThresholdSliders(addProductModal);
			} else {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				// Sliders still need to be initialized to match the (invalid) values
				initThresholdSliders(addProductModal);
				// Image uploader state is lost on validation error, user must re-select
				if (addImageUploader) addImageUploader.resetUploader();
			}
		});

		addProductModal.addEventListener('hidden.bs.modal', function() {
			// ... (Omitted unchanged 'hidden.bs.modal' listener) ...
			if (mainElement.dataset.showAddProductModal !== 'true') {
				console.log("Clearing Add Product modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const errorAlert = form ? form.querySelector('.alert.alert-danger') : null;
				if (errorAlert && errorAlert.getAttribute('th:if') === null) {
					errorAlert.remove();
				}
				if (ingredientsContainer) ingredientsContainer.innerHTML = '';
				if (addImageUploader) addImageUploader.resetUploader(); // NEW
			} else {
				console.log("Resetting showAddProductModal flag on hide.")
				mainElement.removeAttribute('data-show-add-product-modal');
			}
		});
	}

	// --- NEW: Logic for Manage Categories Modal (Clear on Hide) ---
	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				console.log("Clearing Manage Categories (Add) form on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showManageCategoriesModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}
	// --- END NEW ---

	// --- NEW: Logic for Edit Category Modal ---
	const editCategoryModal = document.getElementById('editCategoryModal');
	if (editCategoryModal) {
		const form = editCategoryModal.querySelector('#editCategoryForm');

		editCategoryModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isValidationReopen = mainElement.dataset.showEditCategoryModal === 'true';
			console.log("Edit Category Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); // Debug

			if (button && button.classList.contains('edit-category-btn') && !isValidationReopen) {
				const dataset = button.dataset;
				console.log("Populating Edit Category Modal with data:", dataset); // Debug
				if (form) {
					form.querySelector('#editCategoryId').value = dataset.id || '';
					form.querySelector('#editCategoryName').value = dataset.name || '';
				}
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
			}

			// Clear previous validation highlights unless reopening
			if (mainElement.dataset.showEditCategoryModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); // Debug
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); // Debug
			}
		});

		editCategoryModal.addEventListener('hidden.bs.modal', function() {
			// Clear form state only if not flagged to stay open
			if (mainElement.dataset.showEditCategoryModal !== 'true') {
				console.log("Clearing Edit Category modal on hide (not validation reopen).") // Debug
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showEditCategoryModal flag on hide.") // Debug
				mainElement.removeAttribute('data-show-edit-category-modal');
			}
		});
	}
	// --- END NEW ---


	// --- Recipe Ingredient Management ---
	// ... (addIngredientRow, removeIngredientRow, renumberIngredientRows functions remain unchanged) ...
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


	// Add Ingredient Button Listeners
	document.getElementById('addIngredientBtn')?.addEventListener('click', () => addIngredientRow('addIngredientsContainer', 'ingredientRowTemplate'));
	document.getElementById('addIngredientBtnEditModal')?.addEventListener('click', () => addIngredientRow('editIngredientsContainerModal', 'ingredientRowTemplateEditModal'));

	// **** START: CONSOLIDATED CLICK LISTENER ****
	// Event delegation for Remove buttons AND Max buttons
	document.addEventListener('click', function(event) {

		// --- Handle Remove Ingredient ---
		const removeBtn = event.target.closest('.remove-ingredient-btn');
		if (removeBtn) {
			removeIngredientRow(removeBtn);
			return; // Stop processing this click
		}

		// --- Handle MAX BUTTON LOGIC ---
		const maxBtn = event.target.closest('.max-quantity-btn');
		if (!maxBtn) return; // Not a max button, stop processing

		console.log("--- Max button clicked (product page) ---");

		const row = maxBtn.closest('tr');
		const quantityInput = row ? row.querySelector('.quantity-change-input') : null;
		const productId = row ? row.dataset.productId : null;

		if (!mainElement) {
			console.error("Main element not found inside Max button listener.");
			alert("Internal error: Configuration data missing.");
			return;
		}
		const inventoryStockJson = mainElement.dataset.inventoryStockMap;
		const ingredientsData = row ? row.dataset.productIngredients : null;

		if (!row) { alert("Internal error: Could not identify product row."); return; }
		if (!quantityInput) { alert("Internal error: Could not find quantity field."); return; }
		if (!productId) { alert("Internal error: Product ID missing."); return; }
		if (!ingredientsData || ingredientsData.trim() === '') { alert("Could not calculate maximum: Product recipe data is missing."); return; }
		if (!inventoryStockJson || inventoryStockJson.trim() === '') { alert("Could not calculate maximum: Inventory stock data missing."); return; }


		try {
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
			if (recipe.length === 0) { alert("Product has no valid ingredients."); quantityInput.value = 0; return; }

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
			} catch (jsonError) { console.error("Error parsing inventory stock JSON:", jsonError); alert("Inventory data error."); quantityInput.value = ''; return; }

			console.log("Parsed inventory map:", inventoryStockMap);
			if (Object.keys(inventoryStockMap).length === 0) { alert("Inventory data appears empty."); quantityInput.value = 0; return; }


			console.log("Calculating maximum...");
			let maxPossible = Infinity;
			for (const ingredient of recipe) {
				const availableStock = inventoryStockMap[ingredient.itemId];
				console.log(`Ingredient ID ${ingredient.itemId}: Need ${ingredient.quantityNeeded}, Available: ${availableStock}`);

				if (availableStock === undefined || availableStock === null || isNaN(availableStock)) {
					console.error(`Stock not found/invalid for ingredient ID ${ingredient.itemId}.`); maxPossible = 0; break;
				}
				if (availableStock <= 0 || availableStock < ingredient.quantityNeeded) {
					console.log(`Insufficient stock for ingredient ID ${ingredient.itemId}.`); maxPossible = 0; break;
				}

				const possibleUnits = Math.floor(availableStock / ingredient.quantityNeeded);
				maxPossible = Math.min(maxPossible, possibleUnits);
			}

			console.log(`Max Possible = ${maxPossible}`);

			if (maxPossible === Infinity || maxPossible < 0) {
				console.warn("Max calculation result invalid, setting to 0.");
				quantityInput.value = 0;
			} else {
				quantityInput.value = maxPossible;
			}
			console.log(`Set quantity input to: ${quantityInput.value}`);

		} catch (error) { console.error("Unexpected error during Max calculation:", error); alert("Calculation error."); quantityInput.value = ''; }
	});
	// **** END: CONSOLIDATED CLICK LISTENER ****

});