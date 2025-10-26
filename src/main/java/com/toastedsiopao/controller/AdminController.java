package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin") // All URLs in this controller will start with /admin
public class AdminController {

    @GetMapping("/login")
    public String adminLogin() {
        // Renders templates/admin/login.html
        return "admin/login";
    }
    
    // NEW: Mapping for the admin dashboard
    @GetMapping("/dashboard")
    public String adminDashboard() {
        // Renders templates/admin/dashboard.html
        return "admin/dashboard";
    }

}
