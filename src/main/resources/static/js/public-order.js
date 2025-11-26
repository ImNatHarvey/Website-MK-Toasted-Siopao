document.addEventListener('DOMContentLoaded', function() {
    // ========================================================================
    // == START: PUBLIC (GUEST) ORDER FORM & REDIRECT LOGIC ==
    // ========================================================================
    const orderForm = document.getElementById('order-form');
    const placeOrderBtn = document.getElementById('proceed-to-checkout-btn');

    // Check if we are on the public /order page
    if (orderForm && placeOrderBtn) {
        console.log("Public order form logic initialized.");

        const phoneRegex = /^(09|\+639)\d{9}$/;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const GUEST_DATA_KEY = 'guestCheckoutData';

        // --- Helper to normalize whitespace (internal spaces to single space) ---
        const normalizeWhitespace = (value) => {
			if (!value) return '';
			// Trim leading/trailing and replace multiple internal spaces with one
			return value.trim().replace(/\s+/g, ' ');
		}
		// --- End Helper ---

        // --- Field Validation Function ---
        const validateField = (field, regex = null, feedbackEl = null, emptyMsg = "This field is required.", invalidMsg = "Invalid format.") => {
            let isValid = true;
            // Use normalizeWhitespace on value for validation, but DO NOT modify the input field value here.
            const value = normalizeWhitespace(field.value);
            const isRequired = field.hasAttribute('required');
            
            // Check for leading/trailing spaces (now covered by the normalizeWhitespace in value, check if trimmed value starts/ends with a space is not needed)

            if (!value) {
                if (isRequired) {
                    if (feedbackEl) feedbackEl.textContent = emptyMsg;
                    field.classList.add('is-invalid');
                    isValid = false;
                } else {
                    field.classList.remove('is-invalid');
                }
            } else if (regex && !regex.test(field.value.trim())) { // Use original input value for phone/email regex check
                if (feedbackEl) feedbackEl.textContent = invalidMsg;
                field.classList.add('is-invalid');
                isValid = false;
            } else if ((field.id === 'houseNo' || field.id === 'lotNo' || field.id === 'blockNo') && value.length > 50) {
                if (feedbackEl) feedbackEl.textContent = `Cannot exceed 50 characters.`;
                field.classList.add('is-invalid');
                isValid = false;
            } else {
                field.classList.remove('is-invalid');
            }
            return isValid;
        };

        // --- Main Form Validation Function ---
        const validateGuestForm = () => {
            let isFormValid = true;
            
            // Validate all fields
            isFormValid &= validateField(document.getElementById('first-name'), null, document.getElementById('first-name').nextElementSibling);
            isFormValid &= validateField(document.getElementById('last-name'), null, document.getElementById('last-name').nextElementSibling);
            isFormValid &= validateField(document.getElementById('street'), null, document.getElementById('street').nextElementSibling);
            isFormValid &= validateField(document.getElementById('barangay'), null, document.getElementById('barangay').nextElementSibling);
            isFormValid &= validateField(document.getElementById('municipality'), null, document.getElementById('municipality').nextElementSibling);
            isFormValid &= validateField(document.getElementById('province'), null, document.getElementById('province').nextElementSibling);
            
            isFormValid &= validateField(document.getElementById('phone'), phoneRegex, document.getElementById('phone-feedback'), "Phone number is required.", "Invalid format (e.g., 09xxxxxxxxx).");
            isFormValid &= validateField(document.getElementById('email'), emailRegex, document.getElementById('email-feedback'), "Email is required.", "Invalid email format.");
            
            // Non-required but validated if filled
            isFormValid &= validateField(document.getElementById('houseNo'), null, document.getElementById('houseNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('lotNo'), null, document.getElementById('lotNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('blockNo'), null, document.getElementById('blockNo').nextElementSibling);

            return isFormValid;
        };
        
        // --- Form Data Collection (UPDATED TO USE normalizeWhitespace) ---
        const getGuestData = () => {
            return {
                firstName: normalizeWhitespace(document.getElementById('first-name').value),
                lastName: normalizeWhitespace(document.getElementById('last-name').value),
                phone: document.getElementById('phone').value.trim(),
                email: document.getElementById('email').value.trim(),
                houseNo: document.getElementById('houseNo').value.trim(),
                lotNo: document.getElementById('lotNo').value.trim(),
                blockNo: document.getElementById('blockNo').value.trim(),
                street: normalizeWhitespace(document.getElementById('street').value),
                barangay: normalizeWhitespace(document.getElementById('barangay').value),
                municipality: normalizeWhitespace(document.getElementById('municipality').value),
                province: normalizeWhitespace(document.getElementById('province').value)
            };
        };
        
        // --- "Place Order" Button Click Handler ---
        placeOrderBtn.addEventListener('click', function(event) {
            console.log("Public 'Place Order' button clicked.");
            
            if (!validateGuestForm()) {
                console.log("Form validation failed.");
                
                // Show an error toast
                if (typeof queueToast === 'function' && typeof showToastNotifications === 'function') {
                    queueToast("Please fill out all required fields correctly.", true);
                    showToastNotifications();
                } else {
                    console.error("Toast functions (queueToast, showToastNotifications) not found.");
                    alert("Please fill out all required fields correctly.");
                }
                
                // Scroll to the first error
                const firstInvalid = orderForm.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstInvalid.focus();
                }
            } else {
                // Validation passed
                console.log("Form validation success. Saving data to sessionStorage.");
                const guestData = getGuestData();
                sessionStorage.setItem(GUEST_DATA_KEY, JSON.stringify(guestData));
                
                console.log("Redirecting to /signup?source=checkout");
                window.location.href = '/signup?source=checkout';
            }
        });
        
        // --- Enable/Disable Button based on Cart ---
        // We observe the "order-summary" div, which is controlled by public-cart.js
        const cartSummary = document.querySelector('.order-summary');
        if (cartSummary) {
            const checkoutButton = cartSummary.querySelector('.btn-checkout');

            const observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(mutation) {
                    // We check if the 'disabled' class was added or removed from the button
                    if (mutation.target === checkoutButton && mutation.attributeName === 'class') {
                        const isDisabledByCart = checkoutButton.classList.contains('disabled');
                        
                        // We also need to check our form's validity, but for now
                        // we can just enable/disable based on cart.
                        // The final validation happens on click.
                        placeOrderBtn.disabled = isDisabledByCart;
                        
                        if(isDisabledByCart) {
							placeOrderBtn.classList.add('disabled');
						} else {
							placeOrderBtn.classList.remove('disabled');
						}
                    }
                });
            });

            observer.observe(checkoutButton, {
                attributes: true // Listen for attribute changes
            });
            
            // Initial check
            placeOrderBtn.disabled = checkoutButton.classList.contains('disabled');
            if(placeOrderBtn.disabled) {
				placeOrderBtn.classList.add('disabled');
			}
        }
        

    }
    // ========================================================================
    // == END: PUBLIC (GUEST) ORDER FORM LOGIC ==
    // ========================================================================
});