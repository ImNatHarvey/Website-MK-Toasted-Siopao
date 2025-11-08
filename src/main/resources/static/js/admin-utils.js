/**
 * Re-usable function to set up an image uploader.
 * @param {string} containerId 
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
	const removeImageHiddenInput = uploader.querySelector('.image-remove-flag');

	if (!input || !dropZone || !previewContainer || !previewImg || !removeBtn) {
		console.error(`Uploader #${containerId} is missing required elements.`);
		return;
	}

	if (!removeImageHiddenInput) {
		console.error(`Uploader #${containerId} is missing '.image-remove-flag' hidden input.`);
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