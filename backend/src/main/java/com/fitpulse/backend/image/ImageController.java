package com.fitpulse.backend.image;

import com.fitpulse.backend.common.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageStorage imageStorage;

    public ImageController(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    @GetMapping("/{key}")
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        StoredImage image = imageStorage.load(key)
                .orElseThrow(() -> ApiException.notFound("Slika nije pronađena"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.data());
    }
}
