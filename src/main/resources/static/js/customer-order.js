document.addEventListener('DOMContentLoaded', function() {
    // ========================================================================
    // == START: CUSTOMER ORDER FORM & MODAL LOGIC ==
    // ========================================================================
    const mainElement = document.querySelector('main.container-fluid');
    const orderForm = document.getElementById('customer-order-form');
    const copyDetailsBtn = document.getElementById('copy-my-details-btn');
    const placeOrderBtn = document.getElementById('place-order-btn-trigger');
    const paymentModalEl = document.getElementById('paymentModal');

    if (mainElement && orderForm && copyDetailsBtn && placeOrderBtn && paymentModalEl) {
        console.log("Customer order form and modal logic initialized.");

        const paymentModal = new bootstrap.Modal(paymentModalEl);

        // FIX 1: "Copy My Details" Button Logic
        // Map of form IDs to main element data-c-* attribute suffixes (now camelCase)
        const fieldMap = {
            'shipping-firstName': 'cFirstName',
            'shipping-lastName': 'cLastName',
            'shipping-phone': 'cPhone',
            'shipping-email': 'cEmail',
            'shipping-houseNo': 'cHouseNo',
            'shipping-lotNo': 'cLotNo',
            'shipping-blockNo': 'cBlockNo',
            'shipping-street': 'cStreet',
            'shipping-barangay': 'cBarangay',
            'shipping-municipality': 'cMunicipality',
            'shipping-province': 'cProvince'
        };

        copyDetailsBtn.addEventListener('click', function() {
            for (const [fieldId, dataKey] of Object.entries(fieldMap)) {
                const input = document.getElementById(fieldId);
                const dataValue = mainElement.dataset[dataKey]; // Access camelCase dataset key

                // BUG 1 FIX: Only populate if dataValue exists.
                // Removed the 'else' block that was clearing the fields.
                if (input && dataValue && dataValue !== 'null') {
                    input.value = dataValue;
                }
            }

            // Show a confirmation toast (relies on admin-utils.js)
            if (typeof queueToast === 'function') {
                queueToast("Your profile details have been copied to the form.", false);
                showToastNotifications();
            }

            copyDetailsBtn.blur();
        });

        // FIX 2: "Place Order" Button Validation Logic
        const phoneRegex = /^(09|\+639)\d{9}$/;
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        const validateField = (field, regex = null, feedbackEl = null, emptyMsg = "This field is required.", invalidMsg = "Invalid format.") => {
            let isValid = true;
            const value = field.value.trim();
            const isRequired = field.hasAttribute('required');

            if (!value) {
                if (isRequired) {
                    feedbackEl.textContent = emptyMsg;
                    field.classList.add('is-invalid');
                    isValid = false;
                } else {
                    field.classList.remove('is-invalid');
                }
            } else if (regex && !regex.test(value)) {
                feedbackEl.textContent = invalidMsg;
                field.classList.add('is-invalid');
                isValid = false;
            } else if ((field.id === 'shipping-houseNo' || field.id === 'shipping-lotNo' || field.id === 'shipping-blockNo') && value.length > 50) {
                feedbackEl.textContent = `Cannot exceed 50 characters.`;
                field.classList.add('is-invalid');
                isValid = false;
            } else {
                field.classList.remove('is-invalid');
            }
            return isValid;
        };

        const validateShippingForm = () => {
            let isFormValid = true;
            
            isFormValid &= validateField(document.getElementById('shipping-firstName'), null, document.getElementById('shipping-firstName').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-lastName'), null, document.getElementById('shipping-lastName').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-street'), null, document.getElementById('shipping-street').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-barangay'), null, document.getElementById('shipping-barangay').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-municipality'), null, document.getElementById('shipping-municipality').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-province'), null, document.getElementById('shipping-province').nextElementSibling);
            
            isFormValid &= validateField(document.getElementById('shipping-phone'), phoneRegex, document.getElementById('shipping-phone-feedback'), "Phone number is required.", "Invalid format (e.g., 09xxxxxxxxx).");
            isFormValid &= validateField(document.getElementById('shipping-email'), emailRegex, document.getElementById('shipping-email-feedback'), "Email is required.", "Invalid email format.");
            
            isFormValid &= validateField(document.getElementById('shipping-houseNo'), null, document.getElementById('shipping-houseNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-lotNo'), null, document.getElementById('shipping-lotNo').nextElementSibling);
            isFormValid &= validateField(document.getElementById('shipping-blockNo'), null, document.getElementById('shipping-blockNo').nextElementSibling);

            return isFormValid;
        };

        // Map of modal's hidden field IDs to form IDs
        const modalFieldMap = {
            'form_firstName': 'shipping-firstName',
            'form_lastName': 'shipping-lastName',
            'form_phone': 'shipping-phone',
            'form_email': 'shipping-email',
            'form_houseNo': 'shipping-houseNo',
            'form_lotNo': 'shipping-lotNo',
            'form_blockNo': 'shipping-blockNo',
            'form_street': 'shipping-street',
            'form_barangay': 'shipping-barangay',
            'form_municipality': 'shipping-municipality',
            'form_province': 'shipping-province'
        };

        // BUG 2 FIX: Manually control modal display
        placeOrderBtn.addEventListener('click', function(event) {
            console.log("Place Order button clicked. Validating form...");
            if (!validateShippingForm()) {
                console.log("Form validation failed. Stopping modal.");
                
                if (typeof queueToast === 'function') {
                    queueToast("Please fill out all required fields correctly.", true);
                    showToastNotifications();
                }
                
                const firstInvalid = orderForm.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstInvalid.focus();
                }
            } else {
                console.log("Form validation success. Copying data to modal.");
                // Copy data from visible form to modal's hidden form
                for (const [modalFieldId, formFieldId] of Object.entries(modalFieldMap)) {
                    const modalInput = document.getElementById(modalFieldId);
                    const formInput = document.getElementById(formFieldId);
                    if (modalInput && formInput) {
                        modalInput.value = formInput.value;
                    }
                }
                // Manually show the modal
                paymentModal.show();
            }
        });

        // --- PAYMENT MODAL LOGIC (Copied from public-cart.js) ---
        const CART_KEY = 'mkSiopaoCart';
        const cartFormInput = document.getElementById('form_cartDataJson');
        const modalSummaryList = paymentModalEl.querySelector('.order-summary-modal-list');
        const modalTotalPriceElements = paymentModalEl.querySelectorAll('.total-price-modal');
        
        const gcashInstructions = document.getElementById('gcash-payment-instructions');
        const codInstructions = document.getElementById('cod-payment-instructions');
        const gcashRadio = document.getElementById('payment_gcash');
        const codRadio = document.getElementById('payment_cod');
        const receiptUploader = document.getElementById('receiptUploader');
        
        const formatCurrency = (value) => {
            return new Intl.NumberFormat('en-PH', {
                style: 'currency',
                currency: 'PHP'
            }).format(value);
        };
        const getCartTotal = (cart) => {
            let total = 0;
            Object.keys(cart).forEach(id => {
                const item = cart[id];
                total += item.price * item.quantity;
            });
            return total;
        };

        if (receiptUploader && typeof setupImageUploader === 'function') {
            setupImageUploader('receiptUploader');
        }

        paymentModalEl.addEventListener('show.bs.modal', function() {
            console.log("Payment modal opening. Populating cart data.");
            
            const cart = JSON.parse(sessionStorage.getItem(CART_KEY) || '{}');
            const productIds = Object.keys(cart);
            
            modalSummaryList.innerHTML = '';
            if (productIds.length === 0) {
                modalSummaryList.innerHTML = '<p class="text-danger">Your cart is empty.</p>';
            } else {
                productIds.forEach(id => {
                    const item = cart[id];
                    const itemTotal = item.price * item.quantity;
                    const itemEl = document.createElement('div');
                    itemEl.className = 'd-flex justify-content-between';
                    itemEl.innerHTML = `
						<span>${item.quantity}x ${item.name}</span>
						<span class="fw-bold">${formatCurrency(itemTotal)}</span>
					`;
                    modalSummaryList.appendChild(itemEl);
                });
            }
            
            const totalPrice = getCartTotal(cart);
            modalTotalPriceElements.forEach(el => {
                el.textContent = formatCurrency(totalPrice);
            });
            
            cartFormInput.value = JSON.stringify(cart);
            
            if (receiptUploader) receiptUploader.resetUploader();
            
            const notesField = document.getElementById('form_notes');
            if (notesField) notesField.value = document.getElementById('customer-order-form').querySelector('#notes')?.value || '';
            
            gcashRadio.checked = true;
            togglePaymentMethod();

            const submitBtn = paymentModalEl.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Confirm & Place Order';
            }
        });
        
        const togglePaymentMethod = () => {
            if (codRadio.checked) {
                codInstructions.style.display = 'block';
                gcashInstructions.style.display = 'none';
                if (receiptUploader) receiptUploader.querySelector('.image-uploader-input').required = false;
            } else {
                codInstructions.style.display = 'none';
                gcashInstructions.style.display = 'block';
                if (receiptUploader) receiptUploader.querySelector('.image-uploader-input').required = true;
            }
        };
        
        if (gcashRadio) gcashRadio.addEventListener('change', togglePaymentMethod);
        if (codRadio) codRadio.addEventListener('change', togglePaymentMethod);
        
        const paymentForm = document.getElementById('payment-form');
        if(paymentForm) {
            paymentForm.addEventListener('submit', function(event) {
                if (gcashRadio.checked) {
                    const receiptInput = receiptUploader.querySelector('.image-uploader-input');
                    if (!receiptInput.files || receiptInput.files.length === 0) {
                        event.preventDefault();
                        if (typeof queueToast === 'function') {
                            queueToast("Please upload your payment receipt to proceed.", true);
                            showToastNotifications();
                        }
                        return;
                    }
                }
                const submitBtn = paymentForm.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Placing Order...';
                }
            });
        }
    }
    // ========================================================================
    // == END: CUSTOMER ORDER FORM & MODAL LOGIC ==
    // ========================================================================
});