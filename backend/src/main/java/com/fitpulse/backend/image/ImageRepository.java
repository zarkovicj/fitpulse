package com.fitpulse.backend.image;

import org.springframework.data.jpa.repository.JpaRepository;

interface ImageRepository extends JpaRepository<Image, String> {
}
