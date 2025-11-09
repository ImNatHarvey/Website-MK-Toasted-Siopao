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

/**
 * NEW FUNCTION: TOAST NOTIFICATION HANDLER
 * This function finds the hidden #toast-messages div,
 * reads all data attributes, and creates a toast for each one.
 */
function showToastNotifications() {
	const messageContainer = document.getElementById('toast-messages');
	const toastContainer = document.querySelector('.toast-container');

	if (!messageContainer || !toastContainer) {
		console.log("Toast containers not found. Skipping toast notifications.");
		return;
	}

	const messages = messageContainer.dataset;
	let toastCounter = 0;

	for (const key in messages) {
		if (Object.prototype.hasOwnProperty.call(messages, key) && messages[key]) {
			const message = messages[key];
			const isError = key.toLowerCase().includes('error');

			const toastId = `toast-${key}-${toastCounter++}`;

			// --- MODIFIED: Create Toast HTML with Header ---
			const toastEl = document.createElement('div');
			toastEl.id = toastId;
			// Remove background color from main toast element
			toastEl.className = `toast`;
			toastEl.setAttribute('role', 'alert');
			toastEl.setAttribute('aria-live', 'assertive');
			toastEl.setAttribute('aria-atomic', 'true');

			const headerClass = isError ? 'text-bg-danger' : 'text-bg-success';
			const iconClass = isError ? 'fa-triangle-exclamation' : 'fa-check-circle';
			const title = isError ? 'Error' : 'Success';
			const startTime = Date.now(); // Record start time

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
			// --- END MODIFICATION ---

			// Append to container
			toastContainer.appendChild(toastEl);

			// Initialize and show
			const toast = new bootstrap.Toast(toastEl, {
				delay: isError ? 8000 : 5000 // Longer for errors
			});

			// --- ADDED: Timer Logic ---
			const timeElement = toastEl.querySelector('.toast-time');
			let timerInterval = null;
			if (timeElement) {
				const updateTimer = () => {
					const now = Date.now();
					const elapsed = Math.round((now - startTime) / 1000); // seconds

					if (elapsed < 60) {
						timeElement.textContent = elapsed < 5 ? 'now' : `${elapsed} secs ago`;
					} else {
						const minutes = Math.floor(elapsed / 60);
						timeElement.textContent = `${minutes} min ago`;
					}
				};
				// Set an interval to update the time
				timerInterval = setInterval(updateTimer, 5000); // Update every 5 seconds
			}
			// --- END ADDED ---

			// --- MODIFIED: Clear interval on hide ---
			toastEl.addEventListener('hidden.bs.toast', () => {
				if (timerInterval) {
					clearInterval(timerInterval);
				}
				toastEl.remove();
			});
			// --- END MODIFICATION ---

			toast.show();
			console.log(`Showing ${isError ? 'error' : 'success'} toast: ${message}`);
		}
	}
}

// Run the toast notification handler on page load
document.addEventListener('DOMContentLoaded', showToastNotifications);