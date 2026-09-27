package com.fitpulse.backend.image;

import java.util.Optional;

// gde god da se slike cuvaju (baza, disk, S3), ostatak aplikacije vidi samo kljuc
public interface ImageStorage {

    String store(byte[] data, String contentType);

    Optional<StoredImage> load(String key);

    void delete(String key);
}
