package com.toastedsiopao.service;

import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @Async
    public void createAdminNotification(String message, String link) {
        try {
            Notification notification = new Notification(null, message, link, true);
            notificationRepository.save(notification);
            log.info("Created admin notification: {}", message);
        } catch (Exception e) {
            log.error("Failed to create admin notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void createUserNotification(User user, String message, String link) {
        if (user == null) {
            log.warn("Cannot create user notification for null user.");
            return;
        }
        try {
            Notification notification = new Notification(user, message, link, false);
            notificationRepository.save(notification);
            log.info("Created user notification for {}: {}", user.getUsername(), message);
        } catch (Exception e) {
            log.error("Failed to create user notification for {}: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadAdminNotifications(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findByIsForAdminTrueAndIsReadFalseOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadAdminNotifications() {
        return notificationRepository.countByIsForAdminTrueAndIsReadFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadUserNotifications(User user, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadUserNotifications(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    public void markAdminNotificationAsRead(Long id) {
        notificationRepository.markAdminNotificationAsRead(id);
    }

    @Override
    public void markAllAdminNotificationsAsRead() {
        notificationRepository.markAllAdminNotificationsAsRead();
    }

    @Override
    public void markUserNotificationAsRead(Long id, User user) {
        notificationRepository.markUserNotificationAsRead(id, user);
    }

    @Override
    public void markAllUserNotificationsAsRead(User user) {
        notificationRepository.markAllUserNotificationsAsRead(user);
    }
}