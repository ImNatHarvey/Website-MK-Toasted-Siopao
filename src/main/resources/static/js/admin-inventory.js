document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-inventory.js loaded");

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element not found in admin-inventory.js!");
		return;
	}

	// --- addItemModal (Complex) ---
	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');
		const itemStatusSelect = addItemModal.querySelector('#itemStatus'); 
		// Removed itemStatusWarning element reference

		const itemStockInfoContainer = addItemModal.querySelector('#itemStockInfoContainer');

		const itemStockHiddenInput = addItemModal.querySelector('#itemCurrentStockHidden');

		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		// Removed helper to toggle status warning text
		
		// Removed itemStatusSelect.addEventListener('change', ...)
		


		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');
			
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';
            const dataset = isEdit ? button.dataset : {};

			// --- Step 1: Handle Validation Reopen vs. Fresh Open (Resetting all fields if new session) ---
			if (!isValidationReopen) {
                // This is a fresh open (Add or Edit button click)
				if (itemForm) {
					itemForm.reset();
					// Clear validation classes, etc.
                    itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
                    itemForm.querySelectorAll('.invalid-feedback').forEach(el => {
                        if (el.getAttribute('th:if') === null) {
                            el.textContent = '';
                            el.classList.remove('d-block');
                        }
                    });
				}
				// Explicitly clear non-standard fields for a guaranteed clean slate
                itemIdInput.value = '';
                itemStockInfoContainer.style.display = 'none';
				
				// --- FIX: Explicitly clear main input/select fields for ADD mode clean slate ---
				itemNameInput.value = '';
				itemCategorySelect.value = '';
				itemUnitSelect.value = '';
				itemCostInput.value = '';
				itemLowThresholdInput.value = '';
				itemCriticalThresholdInput.value = '';
				if (itemStatusSelect) itemStatusSelect.value = 'ACTIVE';
				// --- END FIX ---
			}
            
            // --- Step 2: Determine Add vs. Edit/Reopen State and Set UI ---
			let isExistingItem = isEdit || (isValidationReopen && itemIdInput.value);

			if (itemStatusSelect) itemStatusSelect.disabled = false;
            
			if (isExistingItem) {
                // Case A: Fresh Edit or Validation Reopen of an Edit
                
                // For a FRESH EDIT, populate fields now (overwriting the reset defaults/explicit clears)
                if (isEdit && !isValidationReopen) {
					console.log("Fresh Edit: Populating fields from button dataset.");
                    itemIdInput.value = dataset.id || '';
                    itemNameInput.value = dataset.name || '';
                    itemCategorySelect.value = dataset.categoryId || '';
                    itemUnitSelect.value = dataset.unitId || '';
                    itemStatusSelect.value = dataset.itemStatus || 'ACTIVE';
                    if (itemStockHiddenInput) itemStockHiddenInput.value = dataset.currentStock || '0.00';
                    itemLowThresholdInput.value = parseFloat(dataset.lowThreshold || '0').toFixed(0);
                    itemCriticalThresholdInput.value = parseFloat(dataset.criticalThreshold || '0').toFixed(0);
                    itemCostInput.value = dataset.cost || '';
                } 
				
				// Set title based on current value (for reopen) or fresh value (for click)
				modalTitle.textContent = 'Edit Inventory Item: ' + (itemNameInput.value || 'N/A');
                itemStockInfoContainer.style.display = 'block';

			} else {
                // Case B: Fresh Add (guaranteed clean slate) or Validation Reopen (Add)
				
				// Set default title 
                modalTitle.textContent = 'Add New Inventory Item';
                itemStockInfoContainer.style.display = 'none';
				
				if (!isValidationReopen) {
					if (itemStatusSelect) itemStatusSelect.value = 'ACTIVE';
				}
			}
            
            // --- Step 3: Final UI Adjustments ---
			// Removed status warning toggle call
			initThresholdSliders(addItemModal);
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';

			if (isValidationReopen) {
				mainElement.removeAttribute('data-show-add-item-modal');
			}
			if (itemStatusSelect) itemStatusSelect.disabled = false; 
			// Removed status warning reset call
		});
	}

	// --- manageCategoriesModal (Complex) - Left as-is ---
	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addInvCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}

	// --- editInvCategoryModal (Simple) - REFACTORED (Still simple form) ---
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
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = form ? form.querySelector('.invalid-feedback.d-block') : null;
				if (globalError) globalError.textContent = '';
			} else {
				mainElement.removeAttribute('data-show-manage-units-modal');
			}
		});
	}

	// --- editUnitModal (Simple) - REFACTORED (Still simple form) ---
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
		
		// --- ADDED: Event listener moved from inventory.html script block ---
		manageStockModal.addEventListener('change', function(e) {
			if (e.target && e.target.name === 'reasonCategory') {
				const reason = e.target.value;
				const row = e.target.closest('tr'); 
				const addBtn = row.querySelector('button[value="add"]');
				const deductBtn = row.querySelector('button[value="deduct"]');
				
				// Reset first
				addBtn.disabled = false;
				addBtn.classList.remove('disabled');
				addBtn.title = "";
				deductBtn.disabled = false;
				deductBtn.classList.remove('disabled');
				deductBtn.title = "";
				
				if (reason === 'Restock' || reason === 'Production') {
					// Restock / Production = Add only. Disable Deduct.
					deductBtn.disabled = true;
					deductBtn.classList.add('disabled');
					deductBtn.title = reason === 'Restock' ? 
						"Restock implies adding stock." : 
						"Production implies adding stock (finished good production/recipe raw materials received).";
				} else if (['Expired', 'Damaged', 'Waste'].includes(reason)) {
					// Waste reasons = Deduct only. Disable Add.
					addBtn.disabled = true;
					addBtn.classList.add('disabled');
					addBtn.title = "This reason implies removing stock.";
				}
				// 'Manual' allows both (reset covers this case)
			}
		});
		// --- END ADDED ---
		
		manageStockModal.addEventListener('hidden.bs.modal', function() {
			manageStockModal.querySelectorAll('.stock-adjust-form input[type="number"]').forEach(input => {
				input.value = '';
			});
			mainElement.removeAttribute('data-show-manage-stock-modal');
		});
	}
	
	// Helper function for date calculations/formatting
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
	
	function calculateExpirationDate(receivedDateString, expirationDays) {
		if (!receivedDateString || !expirationDays || parseInt(expirationDays) <= 0) return 'No Expiration';
		try {
			const receivedDate = new Date(receivedDateString);
			if (isNaN(receivedDate)) return 'N/A';
			
			const expDays = parseInt(expirationDays);
			// Use setDate to add days, handling month/year rollovers
			const expirationDate = new Date(receivedDate);
			expirationDate.setDate(receivedDate.getDate() + expDays);
			
			return formatDate(expirationDate.toISOString().split('T')[0]);
			
		} catch (e) {
			return 'N/A';
		}
	}


	// --- viewItemModal (Custom) - MODIFIED to include Item Status and Dates ---
	const viewItemModal = document.getElementById('viewItemModal');
	if (viewItemModal) {
		viewItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-item-btn')) {
				console.warn("View Item Modal opened without a valid row source.");
				return;
			}

			const dataset = button.dataset;

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
			
			// --- MODIFIED DATE FIELDS LOGIC ---
			const lastUpdated = dataset.lastUpdated || 'N/A';
			if (lastUpdated !== 'N/A' && viewItemModal.querySelector('#viewItemLastUpdated')) {
				viewItemModal.querySelector('#viewItemLastUpdated').textContent = lastUpdated;
			} else {
				viewItemModal.querySelector('#viewItemLastUpdated').textContent = 'N/A';
			}
			
			// ADDED: Item Active Status
			setText('#viewItemActiveStatus', dataset.itemStatus || 'N/A');

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
			
			const receivedDate = dataset.receivedDate ? formatDate(dataset.receivedDate) : 'N/A';
			const expirationDays = dataset.expirationDays || '0';
			
			// Prefer stored expirationDate, fallback to calculation
			let expirationDateText = 'No Expiration';
			if (dataset.expirationDate) {
				expirationDateText = formatDate(dataset.expirationDate);
			} else {
				expirationDateText = calculateExpirationDate(dataset.receivedDate, dataset.expirationDays);
			}
			
			setText('#viewItemReceivedDate', receivedDate);
			setText('#viewItemExpirationDays', expirationDays + ' days');
			setText('#viewItemExpirationDate', expirationDateText);
			
			const expDateEl = viewItemModal.querySelector('#viewItemExpirationDate');
			if (expDateEl) {
				if (expirationDateText !== 'N/A' && expirationDateText !== 'No Expiration') {
					// Check if actually expired against today
					const now = new Date();
					now.setHours(0,0,0,0);
					
					// Need to parse the formatted string back or use the raw data for comparison
					let expDateObj = null;
					if(dataset.expirationDate) {
						expDateObj = new Date(dataset.expirationDate);
					} else if (dataset.receivedDate && parseInt(expirationDays) > 0) {
						expDateObj = new Date(dataset.receivedDate);
						expDateObj.setDate(expDateObj.getDate() + parseInt(expirationDays));
					}
					
					if (expDateObj) {
						expDateObj.setHours(0,0,0,0);
						if (expDateObj < now) {
							// Expired
							expDateEl.className = 'fw-bold text-danger';
						} else {
							// Not expired, show red as user requested style, or perhaps simple text
							// The user image showed red text for expiration date regardless.
							// I will keep it red as per their screenshot unless valid logic dictates otherwise.
							expDateEl.className = 'fw-bold text-danger';
						}
					}
				} else {
					expDateEl.className = 'fw-bold text-success';
				}
			}
			// --- END MODIFIED DATE FIELDS LOGIC ---
		});
	}
});