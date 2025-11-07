/*
File: imnatharvey/website-mk-toasted-siopao/Website-MK-Toasted-Siopao-8863827f7dde3d57c4a255585160c67e285a6f88/src/main/resources/static/js/admin-settings.js
*/
/**
 * JavaScript specific to the Admin Settings page (admin/settings.html)
 * Handles initializing all the image uploaders.
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-settings.js loaded");

	/**
	 * Re-usable function to set up an image uploader.
	 * Based on the one from admin-products.js
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
		// --- NEW: Find the hidden 'remove' flag ---
		const removeImageHiddenInput = uploader.querySelector('.image-remove-flag');
		// --- END NEW ---

		if (!input || !dropZone || !previewContainer || !previewImg || !removeBtn) {
			console.error(`Uploader #${containerId} is missing required elements.`);
			return;
		}

		// --- UPDATED: Check for the new hidden input ---
		if (!removeImageHiddenInput) {
			console.error(`Uploader #${containerId} is missing '.image-remove-flag' hidden input.`);
			// We can let it continue, but 'remove' won't work on the server
		}
		// --- END UPDATE ---


		// Function to show the preview
		const showPreview = (fileOrSrc) => {
			// Check 1: Is it a valid image URL string?
			if (typeof fileOrSrc === 'string' && fileOrSrc && fileOrSrc !== "null") {
				previewImg.src = fileOrSrc;
				uploader.classList.add('preview-active');
				input.value = ''; // Clear file input
				// --- NEW: Set remove flag to false ---
				if (removeImageHiddenInput) {
					removeImageHiddenInput.value = 'false';
				}
				// --- END NEW ---
			}
			// Check 2: Is it a File object?
			else if (typeof fileOrSrc === 'object' && fileOrSrc !== null && fileOrSrc.name) {
				const reader = new FileReader();
				reader.onload = () => {
					previewImg.src = reader.result;
					uploader.classList.add('preview-active');
				};
				reader.readAsDataURL(fileOrSrc);
				// --- NEW: Set remove flag to false ---
				if (removeImageHiddenInput) {
					removeImageHiddenInput.value = 'false';
				}
				// --- END NEW ---
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
			// --- NEW: Set remove flag to true ---
			if (removeImageHiddenInput) {
				removeImageHiddenInput.value = 'true';
			}
			// --- END NEW ---
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
			// --- UPDATED: Comment ---
			// NOTE: This now sets the hidden 'removeImage' flag to 'true',
			// telling the server to revert to the default image.
			// --- END UPDATE ---
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

	// --- Initialize all 9 uploaders ---
	const uploaderIds = [
		'carousel1Uploader', 'carousel2Uploader', 'carousel3Uploader',
		'feature1Uploader', 'feature2Uploader', 'feature3Uploader', 'feature4Uploader',
		'whyUsUploader', 'aboutUploader'
	];

	uploaderIds.forEach(id => {
		setupImageUploader(id);

		// Now, find the associated hidden input and load its current value
		const uploaderElement = document.getElementById(id);
		if (uploaderElement) {
			// Find the hidden input *inside* the uploader container
			const hiddenInput = uploaderElement.querySelector('input[type="hidden"]');
			if (hiddenInput && hiddenInput.value) {
				console.log(`Initializing preview for ${id} with path: ${hiddenInput.value}`);
				uploaderElement.showPreview(hiddenInput.value);
			} else {
				console.log(`No initial value for ${id}.`);
			}
		}
	});

});