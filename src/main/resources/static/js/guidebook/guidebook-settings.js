(function() {
	window.MK_GUIDEBOOK_REGISTRY = window.MK_GUIDEBOOK_REGISTRY || [];

	const settingsGuide = {
		id: 'settings',
		sortOrder: 8, // 8th Page
		path: '/admin/settings',
		title: 'Site Management',
		content: `
            <div class="guide-scroll-area">
                <h5 class="text-primary mb-3">Customizing Your Website</h5>
                <p>This page allows you to update the text, images, and settings of the public-facing website without needing to write any code. Changes made here are reflected immediately after saving.</p>
                
                <hr>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-copyright me-2 text-info"></i>1. Branding & Identity</h6>
                    <ul class="small mb-0">
                        <li><strong>Website Name:</strong> Appears in the browser tab and the top navigation bar.</li>
                        <li><strong>Logo:</strong> The main brand image in the top-left navbar. <span class="text-muted">(Recommended: Square PNG with transparent background)</span>.</li>
                        <li><strong>Favicon:</strong> The small icon shown in the browser tab next to the page title. <span class="text-muted">(Recommended: .ico file)</span>.</li>
                        <li><strong>Footer Text:</strong> The copyright or legal notice at the very bottom of every page.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-credit-card me-2 text-warning"></i>2. Payment Configuration</h6>
                    <p class="small text-muted mb-2">These details appear in the <strong>Payment Modal</strong> when a customer chooses GCash at checkout.</p>
                    <ul class="small mb-0">
                        <li><strong>GCash QR Code:</strong> Upload a clear screenshot or image of your store's QR code.</li>
                        <li><strong>Account Name/Number:</strong> Displayed as text options for customers who cannot scan the QR code.</li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-house me-2 text-success"></i>3. Homepage Content</h6>
                    <p class="small text-muted mb-2">Control the marketing sections of the landing page:</p>
                    <ul class="list-group list-group-flush small">
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>Carousel (Slider):</strong> Upload up to 3 wide banner images (1600x700px recommended) that slide automatically at the top of the home page.
                        </li>
                        <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>Featured Products:</strong> Customize the 4 highlight cards (e.g., "Best Sellers", "Combos"). You can set the Title, Description, and Image for each.
                        </li>
                         <li class="list-group-item bg-transparent px-0 py-1">
                            <strong>Promo & "Why Us":</strong> Update the text and images for the special offer banner and the value proposition section.
                        </li>
                    </ul>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold"><i class="fa-solid fa-address-card me-2 text-secondary"></i>4. About & Contact</h6>
                    <ul class="small mb-0">
                        <li><strong>About Page:</strong> Set the title, upload a team/store image, and write two paragraphs of your story.</li>
                        <li><strong>Contact Links:</strong> Set the display text and actual URLs for your Facebook page and Phone number (used in the footer/contact section).</li>
                    </ul>
                </div>
                
                <div class="alert alert-info small">
                    <i class="fa-solid fa-floppy-disk me-1"></i> <strong>Tip:</strong> 
                    Scroll to the very bottom and click <strong>Save All Changes</strong> to apply your updates. Large images might take a few seconds to upload.
                </div>
            </div>
        `
	};

	window.MK_GUIDEBOOK_REGISTRY.push(settingsGuide);
})();