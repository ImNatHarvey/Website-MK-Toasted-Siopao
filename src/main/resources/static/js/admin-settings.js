/*
File: imnatharvey/website-mk-toasted-siopao/Website-MK-Toasted-Siopao-8863827f7dde3d57c4a255585160c67e285a6f88/src/main/resources/static/js/admin-settings.js
*/
/**
 * JavaScript specific to the Admin Settings page (admin/settings.html)
 * Handles initializing all the image uploaders.
 *
 * Relies on global functions from admin-utils.js:
 * - setupImageUploader(containerId)
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-settings.js loaded");

	// --- Initialize all 9 uploaders ---
	const uploaderIds = [
		'carousel1Uploader', 'carousel2Uploader', 'carousel3Uploader',
		'feature1Uploader', 'feature2Uploader', 'feature3Uploader', 'feature4Uploader',
		'whyUsUploader', 'aboutUploader'
	];

	uploaderIds.forEach(id => {
		// Call the global setupImageUploader function
		setupImageUploader(id);

		// Now, find the associated hidden input and load its current value
		const uploaderElement = document.getElementById(id);
		if (uploaderElement) {
			// Find the hidden input *inside* the uploader container
			// This relies on the function's internal 'uploader.showPreview'
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