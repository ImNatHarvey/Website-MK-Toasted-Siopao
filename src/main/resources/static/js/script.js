/**
 * Main script file for admin portal features.
 */
document.addEventListener('DOMContentLoaded', function() {

    // Find the main element that holds our data attributes from Thymeleaf
    const mainElement = document.querySelector('main');
    
    if (!mainElement) {
        // If there's no main element, we can't check for modal triggers.
        return;
    }

    // --- Logic to re-open modal on validation error ---

    // Check for category modal trigger
    const showCategoryModal = mainElement.dataset.showCategoryModal;
    if (showCategoryModal === 'true') {
        const categoryModalElement = document.getElementById('manageCategoriesModal');
        if (categoryModalElement) {
            // Found the element, so create a new Bootstrap Modal instance and show it.
            const categoryModal = new bootstrap.Modal(categoryModalElement);
            categoryModal.show();
        }
    }

    // Check for "Add Product" modal trigger
    const showAddProductModal = mainElement.dataset.showAddProductModal;
    if (showAddProductModal === 'true') {
        const productModalElement = document.getElementById('addProductModal');
        if (productModalElement) {
            // Found the element, so create a new Bootstrap Modal instance and show it.
            const productModal = new bootstrap.Modal(productModalElement);
            productModal.show();
        }
    }

    // You can add other global admin scripts here later

});