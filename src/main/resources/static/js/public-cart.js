document.addEventListener('DOMContentLoaded', function() {
	// ========================================================================
	// == START: PUBLIC CART LOGIC (REFACTORED for Full Cart Management) ==
	// ========================================================================
	const cartSummary = document.querySelector('.order-summary');
	if (cartSummary) {
		console.log("Cart summary found. Initializing public cart logic.");
		const CART_KEY = 'mkSiopaoCart';
		const orderItemsList = cartSummary.querySelector('.order-items-list');
		const emptyOrderDiv = cartSummary.querySelector('.empty-order');
		const totalPriceEl = cartSummary.querySelector('.total-price');
		
		// Get the correct checkout button (it's an <a> on menu.html, <button> on order.html)
		const checkoutButton = cartSummary.querySelector('.btn-checkout'); 

		// 1. Helper: Get Cart
		const getCart = () => {
			try {
				const cart = JSON.parse(sessionStorage.getItem(CART_KEY) || '{}');
				return cart;
			} catch (e) {
				console.error("Failed to parse cart from sessionStorage", e);
				return {};
			}
		};

		// 2. Helper: Save Cart
		const saveCart = (cart) => {
			sessionStorage.setItem(CART_KEY, JSON.stringify(cart));
		};

		// 3. Helper: Format Currency
		const formatCurrency = (value) => {
			return new Intl.NumberFormat('en-PH', {
				style: 'currency',
				currency: 'PHP'
			}).format(value);
		};

		// 4. Main Function: Add to Cart (from Product Card)
		const addToCart = (productId, name, price, image, quantityToAdd) => {
			if (quantityToAdd <= 0) return;

			const cart = getCart();
			const existingItem = cart[productId];

			if (existingItem) {
				existingItem.quantity += quantityToAdd;
			} else {
				cart[productId] = { name, price: parseFloat(price), image, quantity: quantityToAdd };
			}

			saveCart(cart);
			renderCart();
		};

		// 5. NEW Function: Update Cart Quantity (from Cart)
		const updateCartQuantity = (productId, newQuantity) => {
			const cart = getCart();
			if (!cart[productId]) return;

			if (newQuantity <= 0) {
				delete cart[productId];
			} else {
				cart[productId].quantity = newQuantity;
			}

			saveCart(cart);
			renderCart();
		};

		// 6. NEW Function: Increment Item
		const incrementItem = (productId) => {
			const cart = getCart();
			if (cart[productId]) {
				// Find product card on page to check stock, if it exists
				const productCard = document.querySelector(`.product-card-public[data-product-id="${productId}"]`);
				const stock = productCard ? parseInt(productCard.dataset.productStock, 10) : Infinity;
				
				if (cart[productId].quantity < stock) {
					updateCartQuantity(productId, cart[productId].quantity + 1);
				} else {
					// Optionally show a toast that max stock is reached in cart
					console.warn(`Cannot increment item ${productId}, max stock ${stock} reached.`);
				}
			}
		};

		// 7. NEW Function: Decrement Item
		const decrementItem = (productId) => {
			const cart = getCart();
			if (cart[productId]) {
				updateCartQuantity(productId, cart[productId].quantity - 1);
			}
		};

		// 8. NEW Function: Remove Item
		const removeItem = (productId) => {
			updateCartQuantity(productId, 0); // Setting qty to 0 removes it
		};

		// 9. NEW Function: Clear Cart (MODIFIED)
		const clearCart = () => {
			saveCart({});
			renderCart();
		};
		
		// --- ADDED: Helper to get total price ---
		const getCartTotal = (cart) => {
			let total = 0;
			Object.keys(cart).forEach(id => {
				const item = cart[id];
				total += item.price * item.quantity;
			});
			return total;
		};

		// 10. Main Function: Render Cart (HEAVILY MODIFIED)
		const renderCart = () => {
			const cart = getCart();
			const productIds = Object.keys(cart);

			if (!orderItemsList || !emptyOrderDiv || !totalPriceEl || !checkoutButton) {
				console.warn("Cart elements (list, emptyDiv, price, button) not all found. Cart render aborted.");
				return;
			}

			orderItemsList.innerHTML = '';
			const totalPrice = getCartTotal(cart); // Use helper

			const isButtonTag = checkoutButton.tagName === 'BUTTON';

			if (productIds.length === 0) {
				emptyOrderDiv.classList.remove('d-none');
				orderItemsList.classList.add('d-none');

				if (isButtonTag) {
					checkoutButton.disabled = true;
				} else {
					checkoutButton.classList.add('disabled');
					checkoutButton.style.pointerEvents = 'none';
					checkoutButton.setAttribute('aria-disabled', 'true');
				}
			} else {
				emptyOrderDiv.classList.add('d-none');
				orderItemsList.classList.remove('d-none');

				if (isButtonTag) {
					checkoutButton.disabled = false;
				} else {
					checkoutButton.classList.remove('disabled');
					checkoutButton.style.pointerEvents = 'auto';
					checkoutButton.setAttribute('aria-disabled', 'false');
				}

				productIds.forEach(productId => {
					const item = cart[productId];
					const itemTotal = item.price * item.quantity;

					const itemEl = document.createElement('div');
					itemEl.className = 'order-item';
					itemEl.innerHTML = `
                        <div class="order-item-img-container">
                            <img src="${item.image}" alt="${item.name}" class="order-item-img">
                        </div>
                        <div class="order-item-title">${item.name}</div>
                        <div class="quantity-stepper-inline" style="max-width: 90px;">
                            <button class="btn btn-sm btn-action-delete qty-btn minus cart-decrement-btn" data-product-id="${productId}">
                                <i class="fa-solid fa-minus"></i>
                            </button>
                            <input type="text" class="form-control form-control-sm qty-input" value="${item.quantity}" readonly>
                            <button class="btn btn-sm btn-action-add qty-btn plus cart-increment-btn" data-product-id="${productId}">
                                <i class="fa-solid fa-plus"></i>
                            </button>
                        </div>
                        <div class="order-item-total">
                            ${formatCurrency(itemTotal)}
                        </div>
                        <button class="btn btn-sm btn-link text-danger cart-remove-btn" data-product-id="${productId}" title="Remove item">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    `;
					orderItemsList.appendChild(itemEl);
				});
			}

			totalPriceEl.textContent = formatCurrency(totalPrice);
		};

		// 11. Event Listener: Handle Product Card Stepper
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

		// 12. Event Listener: Handle "Add to Order" Button Click
		document.addEventListener('click', function(event) {
			const button = event.target.closest('.btn-add-to-order');
			if (!button) return;

			const card = button.closest('.product-card-public');
			if (!card) return;

			const { productId, productName, productPrice, productImage, productStock } = card.dataset;
			const qtyInput = card.querySelector('.qty-input');
			const minusBtn = card.querySelector('.qty-btn.minus');
			const plusBtn = card.querySelector('.qty-btn.plus'); 
			const quantityToAdd = parseInt(qtyInput.value, 10);

			if (quantityToAdd > 0) {
				addToCart(productId, productName, productPrice, productImage, quantityToAdd);
				qtyInput.value = 0;
				minusBtn.disabled = true;
				if (plusBtn) {
					// Re-check stock in case it's 0
					plusBtn.disabled = (parseInt(productStock, 10) <= 0);
				}
			}
		});

		// 13. NEW Event Listener: Handle Clicks INSIDE the Cart Summary
		cartSummary.addEventListener('click', function(event) {
			const button = event.target.closest('button');
			if (!button) return;

			const productId = button.dataset.productId;
			if (!productId) {
				if (button.classList.contains('btn-clear-cart')) {
					clearCart();
				}
				return;
			}

			if (button.classList.contains('cart-increment-btn')) {
				incrementItem(productId);
			} else if (button.classList.contains('cart-decrement-btn')) {
				decrementItem(productId);
			} else if (button.classList.contains('cart-remove-btn')) {
				removeItem(productId);
			}
		});

		// 14. Initialization
		renderCart();
		
		// 15. Check for 'clearCart' flag from redirect (e.g., after successful order)
		const messageContainer = document.getElementById('toast-messages');
		if (messageContainer && messageContainer.dataset.clearCart === 'true') {
			console.log("Order success flag detected. Clearing cart.");
			clearCart();
			delete messageContainer.dataset.clearCart;
		}

	} else {
		console.log("Cart summary not found. Skipping public cart logic.");
	}
	// ========================================================================
	// == END: PUBLIC CART LOGIC ==
	// ========================================================================
});