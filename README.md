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
* **Reporting:** Apache POI (Excel), OpenPDF/iText (PDF)
* **Tools:** Maven, Lombok

## ⚙️ Requirements & Prerequisites

To run and develop this project, the following software must be installed and configured:

1.  **Java Development Kit (JDK) 17** (or compatible newer version).
2.  **Spring Tool Suite (STS) 4** (Preferred IDE).
3.  **MySQL Server 8.0** (Database).
4.  **Project Lombok** (Must be installed into the IDE to handle boilerplate code).

## 🔧 Configuration (Environment Variables)

The application does not use hardcoded credentials. You must set the following **System Environment Variables** on your Windows machine for the application to start:

| Variable Name | Description |
| :--- | :--- |
| `DB_USERNAME` | Your MySQL username (e.g., `root`) |
| `DB_PASSWORD` | Your MySQL password |
| `GMAIL_USERNAME` | Gmail address used for sending system emails |
| `GMAIL_APP_PASSWORD` | The 16-character Google App Password (required for 2FA) |
| `MK_ADMIN_USERNAME` | The username for the initial Admin account |
| `MK_ADMIN_PASSWORD` | The password for the initial Admin account |
| `GOOGLE_CLIENT_ID` | Client ID for Google OAuth Login |
| `GOOGLE_CLIENT_SECRET` | Client Secret for Google OAuth Login |

## 🔑 Default Credentials

Upon the first run, the application creates a default **Owner** account using the variables provided above.

* **Login URL:** `http://localhost:8080/login`
* **Username:** Value of `MK_ADMIN_USERNAME`
* **Password:** Value of `MK_ADMIN_PASSWORD`
