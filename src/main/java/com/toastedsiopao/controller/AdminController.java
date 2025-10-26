package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // All URLs start with /admin
public class AdminController {

    @GetMapping("/login")
    public String adminLogin() {
        return "admin/login";
    }
    
    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }
    
    // --- MAPPINGS FOR ALL ADMIN PAGES ---

    @GetMapping("/orders")
    public String manageOrders() {
        return "admin/orders";
    }
    
    @GetMapping("/customers")
    public String manageCustomers() {
        return "admin/customers";
    }
    
    @GetMapping("/products")
    public String manageProducts() {
        return "admin/products";
    }
    
    @GetMapping("/inventory")
    public String manageInventory() {
        return "admin/inventory";
    }
    
    @GetMapping("/transactions")
    public String manageTransactions() {
        return "admin/transactions";
    }
    
    @GetMapping("/settings")
    public String manageSettings() {
        return "admin/settings";
    }
}

