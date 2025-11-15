document.addEventListener('DOMContentLoaded', function() {
    console.log("customer-cart.js loaded.");

	// --- CSRF TOKEN ---
	const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
	const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
	
	const csrfHeader = csrfHeaderEl ? csrfHeaderEl.content : null;
	const csrfToken = csrfTokenEl ? csrfTokenEl.content : null;
	// --- END CSRF ---

    const cartSummary = document.querySelector('.order-summary');
    if (!cartSummary) {
        console.log("Cart summary not found. Skipping customer cart logic.");
        return;
    }

    // API helper
    const api = {
        call: async (endpoint, payload) => {
            try {
				// --- CSRF FIX: Build headers ---
				const headers = {
                    'Content-Type': 'application/json',
                };
                if (csrfHeader && csrfToken) {
					headers[csrfHeader] = csrfToken;
				}
				// --- END CSRF FIX ---
				
                const response = await fetch(`/api/cart/${endpoint}`, {
                    method: 'POST',
                    headers: headers, // --- CSRF FIX: Use dynamic headers ---
                    body: JSON.stringify(payload),
                });
                
                if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.error || `Server error: ${response.status}`);
                }
                return await response.json();
            } catch (error) {
                console.error(`API call to /api/cart/${endpoint} failed:`, error);
                if (typeof queueToast === 'function') {
                    queueToast(error.message, true);
                    showToastNotifications();
                }
                // --- MODIFICATION: Re-throw the error to be caught by the caller ---
                throw error;
            }
        },
        add: (productId, quantity) => api.call('add', { productId, quantity }),
        update: (productId, newQuantity) => api.call('update', { productId, newQuantity }),
        remove: (productId) => api.call('remove', { productId }),
        clear: () => api.call('clear', {}),
    };

    // UI Elements
    const orderItemsList = cartSummary.querySelector('.order-items-list');
    const emptyOrderDiv = cartSummary.querySelector('.empty-order');
    const totalPriceEl = cartSummary.querySelector('.total-price');
    const checkoutButton = cartSummary.querySelector('.btn-checkout'); // This is an <a> tag in customer/menu

    // Helper: Format Currency
    const formatCurrency = (value) => {
        return new Intl.NumberFormat('en-PH', {
            style: 'currency',
            currency: 'PHP'
        }).format(value);
    };

    /**
     * Renders the entire cart sidebar based on the API response data.
     * @param {object} cartData - The cart data object from the API (must contain items, totalPrice, totalItems)
     */
    const renderCart = (cartData) => {
        if (!cartData) {
            console.warn("RenderCart called with null data. Aborting.");
            return; 
        }
        
        const { items, totalPrice, totalItems } = cartData;

        if (!orderItemsList || !emptyOrderDiv || !totalPriceEl || !checkoutButton) {
            console.warn("Cart elements (list, emptyDiv, price, button) not all found. Cart render aborted.");
            return;
        }

        orderItemsList.innerHTML = ''; // Clear existing items

        const isButtonTag = checkoutButton.tagName === 'BUTTON';

        if (!items || items.length === 0) {
            emptyOrderDiv.classList.remove('d-none');
            orderItemsList.classList.add('d-none');

            if (isButtonTag) {
                checkoutButton.disabled = true;
            } else {
                checkoutButton.classList.add('disabled');
            }
        } else {
            emptyOrderDiv.classList.add('d-none');
            orderItemsList.classList.remove('d-none');

            if (isButtonTag) {
                checkoutButton.disabled = false;
            } else {
                checkoutButton.classList.remove('disabled');
            }

            items.forEach(item => {
                const itemEl = document.createElement('div');
                itemEl.className = 'order-item';
                itemEl.innerHTML = `
                    <div class="order-item-img-container">
                        <img src="${item.image}" alt="${item.name}" class="order-item-img">
                    </div>
                    <div class="order-item-title">${item.name}</div>
                    <div class="quantity-stepper-inline" style="max-width: 90px;">
                        <button class="btn btn-sm btn-action-delete qty-btn minus cart-decrement-btn" data-product-id="${item.productId}">
                            <i class="fa-solid fa-minus"></i>
                        </button>
                        <input type="text" class="form-control form-control-sm qty-input" value="${item.quantity}" readonly>
                        <button class="btn btn-sm btn-action-add qty-btn plus cart-increment-btn" data-product-id="${item.productId}" data-stock="${item.stock}">
                            <i class="fa-solid fa-plus"></i>
                        </button>
                    </div>
                    <div class="order-item-total">
                        ${formatCurrency(item.subtotal)}
                    </div>
                    <button class="btn btn-sm btn-link text-danger cart-remove-btn" data-product-id="${item.productId}" title="Remove item">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                `;
                orderItemsList.appendChild(itemEl);
            });
        }

        totalPriceEl.textContent = formatCurrency(totalPrice);
    };

    // Event Listener: Handle Product Card Stepper (from public-cart.js, unchanged)
    document.addEventListener('click', function(event) {
        const button = event.target.closest('.qty-btn');
        if (!button) return;

        const card = button.closest('.product-card-public');
        if (!card) return; // Not a product card stepper, probably cart stepper

        const stepper = button.closest('.quantity-stepper-inline');
        if (!stepper) return;

        const qtyInput = stepper.querySelector('.qty-input');
        const minusBtn = stepper.querySelector('.qty-btn.minus');
        const plusBtn = stepper.querySelector('.qty-btn.plus');
        
        const stock = parseInt(card.dataset.productStock, 10);
        if (isNaN(stock)) {
            console.warn("Product stock is not a number:", card.dataset.productStock);
            return;
        }

        let currentQty = parseInt(qtyInput.value, 10);

        if (button.classList.contains('plus')) {
            if (currentQty < stock) {
                currentQty++;
            }
        } else if (button.classList.contains('minus')) {
            currentQty--;
        }

        if (currentQty < 0) currentQty = 0;
        
        qtyInput.value = currentQty;
        minusBtn.disabled = (currentQty === 0);
        plusBtn.disabled = (currentQty >= stock);
    });

    // Event Listener: Handle "Add to Order" Button Click
    // --- MODIFIED: Wrapped in try...catch ---
    document.addEventListener('click', async function(event) {
        const button = event.target.closest('.btn-add-to-order');
        if (!button) return;

        const card = button.closest('.product-card-public');
        if (!card) return;

        const { productId, productStock } = card.dataset;
        const qtyInput = card.querySelector('.qty-input');
        const minusBtn = card.querySelector('.qty-btn.minus');
        const plusBtn = card.querySelector('.qty-btn.plus'); 
        const quantityToAdd = parseInt(qtyInput.value, 10);

        if (quantityToAdd > 0) {
            button.disabled = true;
            button.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Adding...';
            
            try {
                const updatedCartData = await api.add(productId, quantityToAdd);
                renderCart(updatedCartData); // Only render on success
                
                // Reset card stepper
                qtyInput.value = 0;
                if(minusBtn) minusBtn.disabled = true;
                if (plusBtn) {
                    plusBtn.disabled = (parseInt(productStock, 10) <= 0);
                }
            } catch (error) {
                // Error toast is already shown by api.call
                console.warn("Add to cart failed, UI not updated.");
            } finally {
                button.disabled = false;
                button.innerHTML = '<i class="fa-solid fa-cart-plus me-1"></i> Add to Order';
            }
        }
    });

    // Event Listener: Handle Clicks INSIDE the Cart Summary
    // --- MODIFIED: Wrapped in try...catch ---
    cartSummary.addEventListener('click', async function(event) {
        const button = event.target.closest('button');
        if (!button) return;

        try {
            // --- Handle Clear Cart ---
            if (button.classList.contains('btn-clear-cart')) {
                const updatedCartData = await api.clear();
                renderCart(updatedCartData);
                return;
            }

            const productId = button.dataset.productId;
            if (!productId) return;

            const itemRow = button.closest('.order-item');
            const qtyInput = itemRow ? itemRow.querySelector('.qty-input') : null;
            let currentQty = qtyInput ? parseInt(qtyInput.value, 10) : 0;
            let newQuantity = currentQty;

            if (button.classList.contains('cart-increment-btn')) {
                const stock = parseInt(button.dataset.stock, 10);
                if (currentQty < stock) {
                    newQuantity = currentQty + 1;
                    const updatedCartData = await api.update(productId, newQuantity);
                    renderCart(updatedCartData);
                } else {
                    if (typeof queueToast === 'function') {
                        queueToast("Max stock reached for this item.", true);
                        showToastNotifications();
                    }
                }
            } else if (button.classList.contains('cart-decrement-btn')) {
                newQuantity = currentQty - 1; // update API handles 0 or less
                const updatedCartData = await api.update(productId, newQuantity);
                renderCart(updatedCartData);
            } else if (button.classList.contains('cart-remove-btn')) {
                const updatedCartData = await api.remove(productId);
                renderCart(updatedCartData);
            }
        } catch (error) {
            // Error toast is already shown by api.call
            console.warn("Cart operation failed, UI not updated.");
        }
    });

    console.log("Customer cart initialized.");
});