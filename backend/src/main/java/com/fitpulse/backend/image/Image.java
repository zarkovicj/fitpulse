package com.fitpulse.backend.image;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Image {

    @Id
    @Column(name = "storage_key", length = 36)
    private String storageKey;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(nullable = false)
    private int sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    static Image create(String storageKey, byte[] data, String contentType) {
        Image image = new Image();
        image.storageKey = storageKey;
        image.data = data;
        image.contentType = contentType;
        image.sizeBytes = data.length;
        return image;
    }
}
