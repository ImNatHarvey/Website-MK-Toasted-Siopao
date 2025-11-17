package com.toastedsiopao.service;

import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.User;

import java.util.List;

public interface NotificationService {

    void createAdminNotification(String message, String link);
    void createUserNotification(User user, String message, String link);

    List<Notification> getUnreadAdminNotifications(int limit);
    long countUnreadAdminNotifications();

    List<Notification> getUnreadUserNotifications(User user, int limit);
    long countUnreadUserNotifications(User user);

    void markAdminNotificationAsRead(Long id);
    void markAllAdminNotificationsAsRead();
    void markUserNotificationAsRead(Long id, User user);
    void markAllUserNotificationsAsRead(User user);
}