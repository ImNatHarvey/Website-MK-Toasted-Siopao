/**
 * admin-utils.js
 *
 * Contains shared utility functions for the admin panel, such as:
 * - setupImageUploader: Initializes the image preview/remove component.
 * - initThresholdSliders: Initializes the number/slider inputs for thresholds.
 */

/**
 * Re-usable function to set up an image uploader.
 * @param {string} containerId The ID of the uploader's main container.
 */
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
	// Find the hidden 'remove' flag
	const removeImageHiddenInput = uploader.querySelector('.image-remove-flag');

	if (!input || !dropZone || !previewContainer || !previewImg || !removeBtn) {
		console.error(`Uploader #${containerId} is missing required elements.`);
		return;
	}

	if (!removeImageHiddenInput) {
		console.error(`Uploader #${containerId} is missing '.image-remove-flag' hidden input.`);
		// We can let it continue, but 'remove' won't work on the server
	}


	// Function to show the preview
	const showPreview = (fileOrSrc) => {
		// Check 1: Is it a valid image URL string?
		if (typeof fileOrSrc === 'string' && fileOrSrc && fileOrSrc !== "null") {
			previewImg.src = fileOrSrc;
			uploader.classList.add('preview-active');
			input.value = ''; // Clear file input
			// Set remove flag to false
			if (removeImageHiddenInput) {
				removeImageHiddenInput.value = 'false';
			}
		}
		// Check 2: Is it a File object?
		else if (typeof fileOrSrc === 'object' && fileOrSrc !== null && fileOrSrc.name) {
			const reader = new FileReader();
			reader.onload = () => {
				previewImg.src = reader.result;
				uploader.classList.add('preview-active');
			};
			reader.readAsDataURL(fileOrSrc);
			// Set remove flag to false
			if (removeImageHiddenInput) {
				removeImageHiddenInput.value = 'false';
			}
		}
		// Check 3: It's null, undefined, "", or "null"
		else {
			resetUploader();
		}
	};

	// Function to reset the uploader (clear preview)
	const resetUploader = () => {
		previewImg.src = '';
		uploader.classList.remove('preview-active');
		input.value = ''; // Clear the file input
		// Set remove flag to true
		if (removeImageHiddenInput) {
			removeImageHiddenInput.value = 'true';
		}
	};

	// --- Event Listeners ---
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
		// NOTE: This now sets the hidden 'removeImage' flag to 'true',
		// telling the server to revert to the default image.
	});

	// Drag and Drop
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

	// --- Public API ---
	uploader.showPreview = showPreview;
	uploader.resetUploader = resetUploader;
}

/**
 * Initializes the linked input[type=number] and input[type=range] sliders
 * for setting thresholds.
 * Assumes inputs are inside a `.threshold-group` with classes
 * `.threshold-input` and `.threshold-slider`.
 * @param {HTMLElement} modalElement The modal element containing the sliders.
 */
function initThresholdSliders(modalElement) {
	console.log("Initializing threshold sliders for modal:", modalElement.id);
	const lowThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="low"]');
	const criticalThresholdGroup = modalElement.querySelector('.threshold-group[data-threshold-type="critical"]');

	if (!lowThresholdGroup || !criticalThresholdGroup) {
		console.warn("Could not find threshold groups in modal:", modalElement.id);
		return;
	}

	// Find by class, not ID
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
		let value = parseInt(input.value, 10); // Use parseInt
		let isBlank = isNaN(value);
		if (isBlank) {
			slider.value = 0; // Set slider to 0 if input is blank
			return; // Don't proceed
		}

		let sliderMax = parseInt(slider.max, 10);
		if (value > sliderMax) {
			slider.max = value;
		}
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

		if (isNaN(lowValue)) {
			lowValue = 0; // Treat as 0 for max calculation
		}
		if (isNaN(criticalValue)) {
			criticalValue = 0; // Treat as 0 for capping
		}

		// Critical max should be one less than low, but not less than 0.
		let criticalMax = (lowValue > 0) ? lowValue - 1 : 0;

		// Set the max attribute for the critical input and slider
		criticalInput.max = criticalMax; // Set max on number input
		criticalSlider.max = criticalMax; // Set max on range slider

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
	});

	// Input updates Slider
	lowInput.addEventListener('input', () => {
		syncSliderAndInput(lowInput, lowSlider);
		enforceThresholdLogic(); // Check logic
	});
	criticalInput.addEventListener('input', () => {
		syncSliderAndInput(criticalInput, criticalSlider);
		enforceThresholdLogic(); // Check logic
	});
}