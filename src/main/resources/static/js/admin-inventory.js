document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-inventory.js loaded"); 

	const mainElement = document.getElementById('admin-content-wrapper');

	if (!mainElement) {
		console.error("Main element not found in admin-inventory.js!");
		return;
	}

	const addItemModal = document.getElementById('addItemModal');
	if (addItemModal) {
		const itemForm = addItemModal.querySelector('#itemForm');
		const modalTitle = addItemModal.querySelector('#addItemModalLabel');
		const itemIdInput = addItemModal.querySelector('#itemId');
		const itemNameInput = addItemModal.querySelector('#itemName');
		const itemCategorySelect = addItemModal.querySelector('#itemCategory');
		const itemUnitSelect = addItemModal.querySelector('#itemUnit');

		const itemStockInfoContainer = addItemModal.querySelector('#itemStockInfoContainer'); 
		
		const itemStockHiddenInput = addItemModal.querySelector('#itemCurrentStockHidden'); 

		const itemLowThresholdInput = addItemModal.querySelector('#itemLowThreshold');
		const itemCriticalThresholdInput = addItemModal.querySelector('#itemCriticalThreshold');
		const itemCostInput = addItemModal.querySelector('#itemCost');

		addItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isEdit = button && button.classList.contains('edit-item-btn');

			const isValidationReopen = mainElement.dataset.showAddItemModal === 'true';
			console.log("Add/Edit Item Modal 'show.bs.modal' event. IsEdit:", isEdit, "IsValidationReopen:", isValidationReopen); 

			if (isEdit && button.dataset && !isValidationReopen) {
				console.log("Populating modal for EDIT with data:", button.dataset); 
				modalTitle.textContent = 'Edit Inventory Item';
				itemIdInput.value = button.dataset.id || '';
				itemNameInput.value = button.dataset.name || '';
				itemCategorySelect.value = button.dataset.categoryId || '';
				itemUnitSelect.value = button.dataset.unitId || '';

				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'block'; 
				
				if (itemStockHiddenInput) itemStockHiddenInput.value = button.dataset.currentStock || '0.00';

				itemLowThresholdInput.value = parseFloat(button.dataset.lowThreshold || '0').toFixed(0);
				itemCriticalThresholdInput.value = parseFloat(button.dataset.criticalThreshold || '0').toFixed(0);
				itemCostInput.value = button.dataset.cost || '';

			} else if (!isEdit && !isValidationReopen) {
				console.log("Populating modal for ADD (resetting form)."); 
				modalTitle.textContent = 'Add New Inventory Item';
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; 

				if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'none'; 

				itemLowThresholdInput.value = ''; 
				itemCriticalThresholdInput.value = ''; 

			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
				
				if (itemIdInput.value) {
					modalTitle.textContent = 'Edit Inventory Item';
					
					if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'block'; 
					
				} else {
					modalTitle.textContent = 'Add New Inventory Item';
					if (itemStockInfoContainer) itemStockInfoContainer.style.display = 'none'; 
				}
			}

			if (mainElement.dataset.showAddItemModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); 
				if (itemForm) itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = itemForm ? itemForm.querySelector('.alert.alert-danger') : null; 
				if (globalError && globalError.getAttribute('th:if') === null) {
					globalError.remove();
				}
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights.");
			}
			
			initThresholdSliders(addItemModal);
		});

		addItemModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showAddItemModal !== 'true') {
				console.log("Clearing Add/Edit Item modal on hide (not validation reopen).") 
				if (itemForm) itemForm.reset();
				itemIdInput.value = ''; 
				if (itemForm) itemForm.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = itemForm ? itemForm.querySelector('.alert.alert-danger') : null;
				if (globalError && globalError.getAttribute('th:if') === null) { 
					globalError.remove();
				}
			} else {
				console.log("Resetting showAddItemModal flag on hide.") 
				mainElement.removeAttribute('data-show-add-item-modal');
			}
		});
	}

	const manageCategoriesModal = document.getElementById('manageCategoriesModal');
	if (manageCategoriesModal) {
		const form = manageCategoriesModal.querySelector('#addInvCategoryForm');

		manageCategoriesModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageCategoriesModal !== 'true') {
				console.log("Clearing Manage Categories modal on hide (not validation reopen).") 
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showManageCategoriesModal flag on hide.") 
				mainElement.removeAttribute('data-show-manage-categories-modal');
			}
		});
	}
	
	const editInvCategoryModal = document.getElementById('editInvCategoryModal');
	if (editInvCategoryModal) {
		const form = editInvCategoryModal.querySelector('#editInvCategoryForm');

		editInvCategoryModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isValidationReopen = mainElement.dataset.showEditInvCategoryModal === 'true';
			console.log("Edit Inv Category Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); 

			if (button && button.classList.contains('edit-inv-category-btn') && !isValidationReopen) {
				const dataset = button.dataset;
				console.log("Populating Edit Inv Category Modal with data:", dataset);
				if (form) {
					form.querySelector('#editInvCategoryId').value = dataset.id || '';
					form.querySelector('#editInvCategoryName').value = dataset.name || '';
				}
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
			}

			if (mainElement.dataset.showEditInvCategoryModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen)."); 
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); 
			}
		});

		editInvCategoryModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditInvCategoryModal !== 'true') {
				console.log("Clearing Edit Inv Category modal on hide (not validation reopen).")
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showEditInvCategoryModal flag on hide.")
				mainElement.removeAttribute('data-show-edit-inv-category-modal');
			}
		});
	}
	
	const manageUnitsModal = document.getElementById('manageUnitsModal');
	if (manageUnitsModal) {
		const form = manageUnitsModal.querySelector('#addUnitForm'); 

		manageUnitsModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showManageUnitsModal !== 'true') {
				console.log("Clearing Manage Units modal on hide (not validation reopen).") 
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
				const globalError = form ? form.querySelector('.invalid-feedback.d-block') : null;
				if (globalError) globalError.textContent = ''; 
			} else {
				console.log("Resetting showManageUnitsModal flag on hide.") 
				mainElement.removeAttribute('data-show-manage-units-modal');
			}
		});
	}

	const editUnitModal = document.getElementById('editUnitModal');
	if (editUnitModal) {
		const form = editUnitModal.querySelector('#editUnitForm');

		editUnitModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			const isValidationReopen = mainElement.dataset.showEditUnitModal === 'true';
			console.log("Edit Unit Modal 'show.bs.modal' event. IsValidationReopen:", isValidationReopen); 

			if (button && button.classList.contains('edit-unit-btn') && !isValidationReopen) {
				const dataset = button.dataset;
				console.log("Populating Edit Unit Modal with data:", dataset); 
				if (form) {
					form.querySelector('#editUnitId').value = dataset.id || '';
					form.querySelector('#editUnitName').value = dataset.name || '';
					form.querySelector('#editUnitAbbreviation').value = dataset.abbreviation || '';
				}
			} else if (isValidationReopen) {
				console.log("Modal is reopening from validation, form values are preserved by Thymeleaf.");
			}

			if (mainElement.dataset.showEditUnitModal !== 'true') {
				console.log("Clearing validation highlights on modal show (not validation reopen).");
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Modal is being reopened due to validation, NOT clearing highlights."); 
			}
		});

		editUnitModal.addEventListener('hidden.bs.modal', function() {
			if (mainElement.dataset.showEditUnitModal !== 'true') {
				console.log("Clearing Edit Unit modal on hide (not validation reopen).") 
				if (form) form.reset();
				if (form) form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			} else {
				console.log("Resetting showEditUnitModal flag on hide.") 
				mainElement.removeAttribute('data-show-edit-unit-modal');
			}
		});
	}
	
	const manageStockModal = document.getElementById('manageStockModal');
	if (manageStockModal) {
		manageStockModal.addEventListener('hidden.bs.modal', function() {
			console.log("Clearing Manage Stock (Inventory) modal inputs on hide.") 
			manageStockModal.querySelectorAll('.stock-adjust-form input[type="number"]').forEach(input => {
				input.value = '';
			});
			mainElement.removeAttribute('data-show-manage-stock-modal'); 
		});
	}

	const viewItemModal = document.getElementById('viewItemModal');
	if (viewItemModal) {
		viewItemModal.addEventListener('show.bs.modal', function(event) {
			const button = event.relatedTarget;
			if (!button || !button.classList.contains('view-item-btn')) {
				console.warn("View Item Modal opened without a valid row source.");
				return;
			}

			const dataset = button.dataset;
			console.log("Populating View Item Modal with data:", dataset);
			
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
			setText('#viewItemLastUpdated', 'Last Updated: ' + (dataset.lastUpdated || 'N/A'));

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
		});
	}
});