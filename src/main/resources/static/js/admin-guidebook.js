// Global Registry for Guidebook Pages
window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

document.addEventListener('DOMContentLoaded', function() {
	console.log("admin-guidebook.js (Engine) loaded.");

	const guidebookModal = document.getElementById('guidebookModal');
	const guidebookTitle = document.getElementById('guidebookTitle');
	const guidebookContent = document.getElementById('guidebookContent');
	const guidebookFooter = document.getElementById('guidebookFooter');

	// 1. Sort pages by their defined 'sortOrder'
	const sortedPages = window.MK_GUIDEBOOK_REGISTRY.sort((a, b) => a.sortOrder - b.sortOrder);

	// 2. Function to build the Carousel HTML (No internal Controls)
	function buildGuidebookCarousel() {
		if (!guidebookContent) return;
		if (sortedPages.length === 0) {
			guidebookContent.innerHTML = '<div class="text-center p-4">No guides available.</div>';
			return;
		}

		// Indicators (Dots)
		let indicatorsHtml = '<div class="carousel-indicators custom-indicators">';
		sortedPages.forEach((page, index) => {
			indicatorsHtml += `
                <button type="button" data-bs-target="#adminGuideCarousel" data-bs-slide-to="${index}" 
                    class="${index === 0 ? 'active' : ''}" aria-current="${index === 0 ? 'true' : 'false'}" 
                    aria-label="${page.title}"></button>
            `;
		});
		indicatorsHtml += '</div>';

		// Slides
		let innerHtml = '<div class="carousel-inner">';
		sortedPages.forEach((page, index) => {
			innerHtml += `
                <div class="carousel-item ${index === 0 ? 'active' : ''}" data-page-path="${page.path}">
                    <div class="guide-slide-content p-4">
                        ${page.content}
                    </div>
                </div>
            `;
		});
		innerHtml += '</div>';

		// Assemble
		guidebookContent.innerHTML = `
            <div id="adminGuideCarousel" class="carousel slide" data-bs-interval="false">
                ${innerHtml}
                ${indicatorsHtml}
            </div>
        `;
	}

	// 3. Update Footer Buttons (Centered & Equal Size, Looping enabled)
	function updateFooterButtons(activeIndex, totalItems) {
		if (!guidebookFooter) return;

		// Rebuild footer: Counter left (abs), Buttons center, Close right (abs)
		// Removed 'disabled' attributes since looping is requested
		guidebookFooter.innerHTML = `
            <div class="position-absolute start-0 ps-4 text-muted small fst-italic d-none d-md-block">
                Page ${activeIndex + 1} of ${totalItems}
            </div>
            
            <div class="d-flex gap-3 justify-content-center w-100">
                <button type="button" class="btn btn-outline-secondary guide-nav-btn" id="guidePrevBtn">
                    <i class="fa-solid fa-chevron-left me-1"></i> Previous
                </button>
                <button type="button" class="btn btn-primary guide-nav-btn" id="guideNextBtn">
                    Next <i class="fa-solid fa-chevron-right ms-1"></i>
                </button>
            </div>

            <div class="position-absolute end-0 pe-3">
                <button type="button" class="btn btn-custom-secondary" data-bs-dismiss="modal">Close</button>
            </div>
        `;
	}

	// 4. Initialize
	if (guidebookModal) {
		buildGuidebookCarousel();

		const carouselElement = document.getElementById('adminGuideCarousel');
		if (carouselElement) {
			const bsCarousel = new bootstrap.Carousel(carouselElement, {
				interval: false,
				wrap: true // Enable looping
			});

			// -- EVENT: OPEN MODAL --
			guidebookModal.addEventListener('show.bs.modal', function() {
				const currentPath = window.location.pathname;
				let targetIndex = 0;

				// Find page matching URL
				sortedPages.forEach((page, index) => {
					if (currentPath.startsWith(page.path)) {
						targetIndex = index;
					}
				});

				// Set title
				if (guidebookTitle) guidebookTitle.textContent = sortedPages[targetIndex].title;

				// Move carousel
				bsCarousel.to(targetIndex);

				// Init buttons
				updateFooterButtons(targetIndex, sortedPages.length);
			});

			// -- EVENT: SLIDE CHANGED --
			carouselElement.addEventListener('slid.bs.carousel', function(event) {
				const activeItem = event.relatedTarget;
				const allItems = carouselElement.querySelectorAll('.carousel-item');
				const newIndex = Array.from(allItems).indexOf(activeItem);

				if (newIndex !== -1) {
					if (guidebookTitle) guidebookTitle.textContent = sortedPages[newIndex].title;
					updateFooterButtons(newIndex, sortedPages.length);
				}
			});

			// -- EVENT: FOOTER CLICK DELEGATION --
			if (guidebookFooter) {
				guidebookFooter.addEventListener('click', function(e) {
					const target = e.target.closest('button');
					if (!target) return;

					if (target.id === 'guidePrevBtn') {
						bsCarousel.prev();
					} else if (target.id === 'guideNextBtn') {
						bsCarousel.next();
					}
				});
			}
		}
	}
});