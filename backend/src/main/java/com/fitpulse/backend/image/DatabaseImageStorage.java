package com.fitpulse.backend.image;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "db", matchIfMissing = true)
class DatabaseImageStorage implements ImageStorage {

    private final ImageRepository imageRepository;

    DatabaseImageStorage(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional
    public String store(byte[] data, String contentType) {
        String key = UUID.randomUUID().toString();
        imageRepository.save(Image.create(key, data, contentType));
        return key;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredImage> load(String key) {
        return imageRepository.findById(key).map(image -> new StoredImage(image.getData(), image.getContentType()));
    }

    @Override
    @Transactional
    public void delete(String key) {
        imageRepository.deleteById(key);
    }
}
