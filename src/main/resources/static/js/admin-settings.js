document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-settings.js loaded");

	const uploaderIds = [
		'carousel1Uploader', 'carousel2Uploader', 'carousel3Uploader',
		'feature1Uploader', 'feature2Uploader', 'feature3Uploader', 'feature4Uploader',
		'whyUsUploader', 'aboutUploader'
	];

	uploaderIds.forEach(id => {
		setupImageUploader(id);

		const uploaderElement = document.getElementById(id);
		if (uploaderElement) {
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