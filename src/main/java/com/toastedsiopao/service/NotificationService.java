package com.toastedsiopao.service;

import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.User;

import java.util.List;

public interface NotificationService {

    // --- Create ---
    void createAdminNotification(String message, String link);
    void createUserNotification(User user, String message, String link);

    // --- Read (Admin) ---
    List<Notification> getUnreadAdminNotifications(int limit);
    long countUnreadAdminNotifications();

    // --- Read (User) ---
    List<Notification> getUnreadUserNotifications(User user, int limit);
    long countUnreadUserNotifications(User user);

    // --- Update (Admin) ---
    void markAdminNotificationAsRead(Long id);
    void markAllAdminNotificationsAsRead();

    // --- Update (User) ---
    void markUserNotificationAsRead(Long id, User user);
    void markAllUserNotificationsAsRead(User user);
}