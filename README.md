# 🥟 MK Toasted Siopao - Ordering & Inventory System

A full-stack web application built with **Spring Boot** and **Thymeleaf** designed to manage the operations of a Toasted Siopao business. This system handles the entire lifecycle of the business, from customer ordering and payments to inventory management, recipe-based stock deduction, and financial reporting.

## 🚀 Key Features

### 🛒 Customer Module
* **Online Ordering:** Customers can browse the menu (Siopao, Drinks, Combos), filter by category, and search for items.
* **Cart Management:** Session-based shopping cart with stock validation.
* **Checkout Options:**
    * **Cash on Delivery (COD):** Standard checkout.
    * **GCash:** Upload payment receipt screenshots and input transaction IDs for verification.
* **User Accounts:** Sign up via email or **Google OAuth2**. Manage profile and delivery addresses.
* **Order Tracking:** Real-time status updates (Pending, Processing, Out for Delivery, Delivered) via email and on-site notifications.
* **Issue Reporting:** Customers can file complaints/issues for delivered orders with image attachments.

### 🛡️ Admin Module
* **Dashboard:** Real-time analytics showing daily sales, estimated COGS, gross profit, order status breakdown, and top-selling products.
* **Order Fulfillment:** Workflow to Accept, Ship, and Complete orders.
    * *Stock Reversal:* Rejecting/Cancelling orders automatically restores inventory.
* **Inventory Management:**
    * **Raw Materials:** Track ingredients (Flour, Meat, etc.) with specific Units of Measure.
    * **Stock Adjustments:** Log Production (adds stock), Wastage/Spoilage (deducts stock), or Manual Adjustments.
    * **Automated Deduction:** Selling a product automatically deducts the raw ingredients based on the defined recipe.
* **Product Management:**
    * Define recipes (e.g., 1 Siopao requires 0.05kg Flour, 0.1kg Meat).
    * Set Low/Critical stock thresholds for alerts.
* **Site Management:** **Dynamic Content System.** Change homepage carousel images, featured products, promo texts, and payment QR codes directly from the admin panel without coding.
* **Reports:** Generate **PDF** and **Excel** reports for:
    * Financials (Sales vs COGS).
    * Inventory Levels.
    * Waste & Spoilage Logs.
    * Activity Logs (Audit Trail).
* **Role-Based Access Control (RBAC):** Create custom roles (e.g., Supervisor, Inventory Clerk) with granular permissions.

## 🛠️ Tech Stack

* **Backend:** Java 17, Spring Boot 3.2.2 (Web, Security, Data JPA, Mail, OAuth2 Client)
* **Database:** MySQL 8.0
* **Frontend:** Thymeleaf, Bootstrap 5.3, JavaScript, Chart.js
* **Containerization:** Docker, Docker Compose
* **Reporting:** Apache POI (Excel), OpenPDF/iText (PDF)
* **Tools:** Maven, Lombok
