package com.toastedsiopao.repository;

import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByIsForAdminTrueAndIsReadFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByIsForAdminTrueAndIsReadFalse();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.isForAdmin = true")
    void markAdminNotificationAsRead(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.isForAdmin = true AND n.isRead = false")
    void markAllAdminNotificationsAsRead();

    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user = :user")
    void markUserNotificationAsRead(@Param("id") Long id, @Param("user") User user);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    void markAllUserNotificationsAsRead(@Param("user") User user);
}