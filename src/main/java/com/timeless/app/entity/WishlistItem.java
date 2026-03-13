package com.timeless.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_watch_wishlist")
public class WishlistItem {

    @EmbeddedId
    private WishlistItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("watchId")
    @JoinColumn(name = "watch_id", nullable = false)
    private Watch watch;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        if (id == null && buyer != null && buyer.getId() != null && watch != null && watch.getId() != null) {
            id = new WishlistItemId(buyer.getId(), watch.getId());
        }
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class WishlistItemId implements Serializable {
        private Long userId;
        private Long watchId;
    }
}
