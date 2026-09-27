package com.fitpulse.backend.image;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

    @Test
    void detect_shouldRecognizeJpegPngAndWebpByContent() {
        assertThat(ImageType.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00))).contains("image/jpeg");
        assertThat(ImageType.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00))).contains("image/png");
        assertThat(ImageType.detect(bytes('R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'))).contains("image/webp");
    }

    @Test
    void detect_shouldRejectOtherContentEvenWithImageLikeName() {
        assertThat(ImageType.detect("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(ImageType.detect(bytes('G', 'I', 'F', '8', '9', 'a'))).isEmpty();
        assertThat(ImageType.detect(bytes(0xFF, 0xD8))).isEmpty();
        assertThat(ImageType.detect(null)).isEmpty();
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
