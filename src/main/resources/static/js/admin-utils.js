function initializeModalForm(options) {
	const modalElement = document.getElementById(options.modalId);
	const wrapperElement = document.getElementById(options.wrapperId || 'admin-content-wrapper');
	if (!modalElement || !wrapperElement) {
		console.error(`initializeModalForm: Could not find modal #${options.modalId} or wrapper.`);
		return;
	}

	const formElement = document.getElementById(options.formId);
	if (!formElement) {
		console.error(`initializeModalForm: Could not find form #${options.formId}.`);
		return;
	}

	modalElement.addEventListener('show.bs.modal', function(event) {
		const isValidationReopen = wrapperElement.getAttribute(options.validationAttribute) === 'true';
		const button = event.relatedTarget;
		const isEdit = button && options.editTriggerClass && button.classList.contains(options.editTriggerClass);
		const dataset = button ? button.dataset : null;

		if (isEdit && dataset && !isValidationReopen) {
			console.log(`Populating modal ${options.modalId} for EDIT with data:`, dataset);
			populateFormFromDataset(formElement, dataset);

			if (options.modalTitleSelector && options.titlePrefix && options.titleDatasetKey) {
				const titleElement = modalElement.querySelector(options.modalTitleSelector);
				if (titleElement && dataset[options.titleDatasetKey]) {
					titleElement.textContent = options.titlePrefix + dataset[options.titleDatasetKey];
				}
			}
		} else if (!isEdit && !isValidationReopen) {
			console.log(`Populating modal ${options.modalId} for ADD (resetting form).`);
			formElement.reset();
			if (options.modalTitleSelector) {
			}
		} else if (isValidationReopen) {
			console.log(`Modal ${options.modalId} is reopening from validation.`);
		}

		if (options.onShow) {
			options.onShow(formElement, dataset, isEdit, isValidationReopen);
		}

		if (!isValidationReopen) {
			formElement.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			const errorAlert = formElement.querySelector('.alert.alert-danger');
			if (errorAlert && errorAlert.getAttribute('th:if') === null) {
				errorAlert.remove();
			}
		}
	});

	modalElement.addEventListener('hidden.bs.modal', function() {
		const isValidationReopen = wrapperElement.getAttribute(options.validationAttribute) === 'true';

		if (options.onHide) {
			options.onHide(formElement, isValidationReopen);
		}

		if (!isValidationReopen) {
			console.log(`Clearing modal ${options.modalId} on hide (not validation reopen).`);
			formElement.reset();
			formElement.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
			const errorAlert = formElement.querySelector('.alert.alert-danger');
			if (errorAlert && errorAlert.getAttribute('th:if') === null) {
				errorAlert.remove();
			}
		} else {
			console.log(`Resetting ${options.validationAttribute} flag on hide.`);
			wrapperElement.removeAttribute(options.validationAttribute);
		}
	});
}

function populateFormFromDataset(form, dataset) {
	for (const key in dataset) {
		if (Object.prototype.hasOwnProperty.call(dataset, key)) {
			let field = form.querySelector(`[name="${key}"]`);
			if (!field) {
				field = form.querySelector(`#${key}`);
			}

			if (field) {
				const value = dataset[key];
				if (field.type === 'checkbox') {
					field.checked = (value === 'true');
				} else if (field.type === 'radio') {
					const radioToSelect = form.querySelector(`input[name="${key}"][value="${value}"]`);
					if (radioToSelect) {
						radioToSelect.checked = true;
					}
				} else {
					field.value = value;
				}
			}
		}
	}
}

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
	const removeImageHiddenInput = uploader.querySelector('.image-remove-flag');

	if (!input || !dropZone || !previewContainer || !previewImg || !removeBtn) {
		console.error(`Uploader #${containerId} is missing required elements.`);
		return;
	}

	if (!removeImageHiddenInput) {
		// This is a soft warning, as the customer-side uploader doesn't use the 'removeImage' flag
		console.warn(`Uploader #${containerId} is missing '.image-remove-flag' hidden input. Remove logic will not be tracked.`);
	}

	const showPreview = (fileOrSrc) => {
		if (typeof fileOrSrc === 'string' && fileOrSrc && fileOrSrc !== "null") {
			previewImg.src = fileOrSrc;
			uploader.classList.add('preview-active');
			input.value = '';
			if (removeImageHiddenInput) {
				removeImageHiddenInput.value = 'false';
			}
		}
		else if (typeof fileOrSrc === 'object' && fileOrSrc !== null && fileOrSrc.name) {
			const reader = new FileReader();
			reader.onload = () => {
				previewImg.src = reader.result;
				uploader.classList.add('preview-active');
			};
			reader.readAsDataURL(fileOrSrc);
			if (removeImageHiddenInput) {
				removeImageHiddenInput.value = 'false';
			}
		}
		else {
			resetUploader();
		}
	};

	const resetUploader = () => {
		previewImg.src = '';
		uploader.classList.remove('preview-active');
		input.value = '';
		if (removeImageHiddenInput) {
			removeImageHiddenInput.value = 'true';
		}
	};

	dropZone.addEventListener('click', () => input.click());
	previewContainer.addEventListener('click', (e) => {
		if (e.target !== removeBtn && !removeBtn.contains(e.target)) {
			input.click();
		}
	});

	input.addEventListener('change', () => {
		if (input.files && input.files[0]) {
			showPreview(input.files[0]);
		}
	});

	removeBtn.addEventListener('click', (e) => {
		e.stopPropagation();
		resetUploader();
	});

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
			input.files = e.dataTransfer.files;
			showPreview(e.dataTransfer.files[0]);
		}
	});

	uploader.showPreview = showPreview;
	uploader.resetUploader = resetUploader;
}

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

	const syncSliderAndInput = (input, slider) => {
		let value = parseInt(input.value, 10);
		let isBlank = isNaN(value);
		if (isBlank) {
			slider.value = 0;
			return;
		}

		let sliderMax = parseInt(slider.max, 10);
		if (value > sliderMax) {
			slider.max = value;
		}
		if (value < 100 && sliderMax > 100) {
			slider.max = 100;
		}

		slider.value = value;
		input.value = value;
	};

	const enforceThresholdLogic = () => {
		let lowValue = parseInt(lowInput.value, 10);
		let criticalValue = parseInt(criticalInput.value, 10);

		if (isNaN(lowValue)) {
			lowValue = 0;
		}
		if (isNaN(criticalValue)) {
			criticalValue = 0;
		}

		let criticalMax = (lowValue > 0) ? lowValue - 1 : 0;

		criticalInput.max = criticalMax;
		criticalSlider.max = criticalMax;

		if (criticalValue > criticalMax) {
			criticalInput.value = criticalMax;
			criticalSlider.value = criticalMax;
		}
	};

	syncSliderAndInput(lowInput, lowSlider);
	syncSliderAndInput(criticalInput, criticalSlider);
	enforceThresholdLogic();

	lowSlider.addEventListener('input', () => {
		lowInput.value = lowSlider.value;
		enforceThresholdLogic();
	});
	criticalSlider.addEventListener('input', () => {
		criticalInput.value = criticalSlider.value;
	});

	lowInput.addEventListener('input', () => {
		syncSliderAndInput(lowInput, lowSlider);
		enforceThresholdLogic();
	});
	criticalInput.addEventListener('input', () => {
		syncSliderAndInput(criticalInput, criticalSlider);
		enforceThresholdLogic();
	});
}

// --- ADDED: Global function to queue toasts from client-side JS ---
const TOAST_QUEUE_KEY = 'toastQueue';
function queueToast(message, isError = false) {
	if (!message) return;
	
	let queue = [];
	try {
		queue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
	} catch (e) {
		console.error("Failed to parse toast queue from sessionStorage:", e);
		queue = [];
	}

	const toastId = `toast-${Date.now()}-${Math.random()}`;
	
	queue.push({
		id: toastId,
		message: message,
		isError: isError,
		timestamp: Date.now()
	});

	sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(queue));
}
// --- END ADDED FUNCTION ---

/**
 * UPDATED FUNCTION: TOAST NOTIFICATION HANDLER
 * This function now uses sessionStorage to create a persistent queue.
 * Toasts no longer auto-hide and feature a live-updating timer.
 * * REFACTORED: Now correctly renders toasts from queue even if #toast-messages
 * div is not present on the current page (e.g., Dashboard).
 */
function showToastNotifications() {
	// const TOAST_QUEUE_KEY = 'toastQueue'; // Already defined above
	const toastContainer = document.querySelector('.toast-container');

	// 1. Check for visual container. If this is missing, we can't do anything.
	if (!toastContainer) {
		console.error("Toast .toast-container not found in DOM. Cannot display toasts.");
		return;
	}

	const messageContainer = document.getElementById('toast-messages'); // This can be null

	// 2. Load existing queue
	let queue = [];
	try {
		queue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
	} catch (e) {
		console.error("Failed to parse toast queue from sessionStorage:", e);
		queue = [];
	}

	// 3. Add new messages IF the container for them exists
	if (messageContainer) {
		const newMessages = messageContainer.dataset;
		let newToastsAdded = false;

		for (const key in newMessages) {
			if (Object.prototype.hasOwnProperty.call(newMessages, key) && newMessages[key]) {
				
				// --- FIX FOR "true" TOAST ---
				if (key === 'clearCart') {
					continue; // Don't make a toast for this attribute
				}
				// --- END FIX ---

				const message = newMessages[key];
				const isError = key.toLowerCase().includes('error');
				const toastId = `toast-${Date.now()}-${Math.random()}`; // Unique ID

				queue.push({
					id: toastId,
					message: message,
					isError: isError,
					timestamp: Date.now()
				});
				newToastsAdded = true;

				// Clear the attribute so it's not re-added on the same page
				delete messageContainer.dataset[key];
			}
		}

		// 4. If new toasts were added, save the updated queue
		if (newToastsAdded) {
			sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(queue));
		}
	} else {
		console.log("No #toast-messages div found on this page. Not adding new toasts, just rendering queue.");
	}


	// 5. Clear the visual toast container and rebuild it from the queue
	toastContainer.innerHTML = '';

	queue.forEach(toastData => {
		const {
			id,
			message,
			isError,
			timestamp
		} = toastData;

		const toastEl = document.createElement('div');
		toastEl.id = id;
		toastEl.className = `toast`;
		toastEl.setAttribute('role', 'alert');
		toastEl.setAttribute('aria-live', 'assertive');
		toastEl.setAttribute('aria-atomic', 'true');

		const headerClass = isError ? 'text-bg-danger' : 'text-bg-success';
		const iconClass = isError ? 'fa-triangle-exclamation' : 'fa-check-circle';
		const title = isError ? 'Error' : 'Success';
		const startTime = timestamp; // Use the stored timestamp

		toastEl.innerHTML = `
            <div class="toast-header ${headerClass} text-white">
                <i class="fa-solid ${iconClass} me-2"></i>
                <strong class="me-auto">${title}</strong>
                <small class="ms-2 toast-time">now</small>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        `;

		toastContainer.appendChild(toastEl);

		// 6. Initialize the toast with autohide: false
		const toast = new bootstrap.Toast(toastEl, {
			autohide: false // Make toast permanent until closed
		});

		// 7. Create the live-updating timer
		const timeElement = toastEl.querySelector('.toast-time');
		let timerInterval = null;

		const updateTimer = () => {
			const now = Date.now();
			const elapsed = Math.round((now - startTime) / 1000); // seconds

			if (elapsed < 5) {
				timeElement.textContent = 'now';
			} else if (elapsed < 60) {
				timeElement.textContent = `${elapsed}s ago`;
			} else {
				const minutes = Math.floor(elapsed / 60);
				const seconds = elapsed % 60;
				timeElement.textContent = `${minutes}m ${seconds}s ago`;
			}
		};

		// Set an interval to update the time every second
		timerInterval = setInterval(updateTimer, 1000);
		updateTimer(); // Run immediately

		// 8. Add listener to remove from queue when closed
		toastEl.addEventListener('hidden.bs.toast', () => {
			// Stop the timer
			if (timerInterval) {
				clearInterval(timerInterval);
			}

			// Remove from sessionStorage
			try {
				let currentQueue = JSON.parse(sessionStorage.getItem(TOAST_QUEUE_KEY) || '[]');
				const newQueue = currentQueue.filter(t => t.id !== id);
				sessionStorage.setItem(TOAST_QUEUE_KEY, JSON.stringify(newQueue));
			} catch (e) {
				console.error("Failed to update toast queue on close:", e);
			}

			// Remove the element from DOM
			toastEl.remove();
		});

		// 9. Show the toast
		toast.show();
		console.log(`Showing ${isError ? 'error' : 'success'} toast: ${message}`);
	});
}
// --- END UPDATED FUNCTION ---

// Run the toast notification handler on page load
document.addEventListener('DOMContentLoaded', showToastNotifications);