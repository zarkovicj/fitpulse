package com.fitpulse.backend.exercise;

import com.fitpulse.backend.TestcontainersConfig;
import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.user.AuthService;
import com.fitpulse.backend.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class ExerciseImageIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 5, 6, 7, 8};

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private AuthService authService;

    private String registerAndLogin(String email) {
        return authService.register(new RegisterRequest("Test", "User", email, "lozinka123", null)).token();
    }

    private ExerciseResponse createExercise(String token) {
        return restTestClient.post().uri("/api/exercises")
                .header("Authorization", "Bearer " + token)
                .body(new ExerciseRequest("Vežba sa slikom", MuscleGroup.ARMS, null, null))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CREATED)
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private RestTestClient.ResponseSpec upload(String token, Long exerciseId, byte[] data, String filename) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        }, partHeaders));

        return restTestClient.put().uri("/api/exercises/" + exerciseId + "/image")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .exchange();
    }

    private String uploadedUrl(String token, Long exerciseId, byte[] data) {
        return upload(token, exerciseId, data, "image.png")
                .expectStatus().isOk()
                .expectBody(ExerciseResponse.class)
                .returnResult()
                .getResponseBody()
                .imageUrl();
    }

    @Test
    void upload_shouldStoreImageAndServeItPubliclyByKey() {
        String token = registerAndLogin("image-owner@example.com");
        ExerciseResponse exercise = createExercise(token);

        String url = uploadedUrl(token, exercise.id(), PNG);
        assertThat(url).matches("/api/images/[0-9a-f-]{36}");

        byte[] served = restTestClient.get().uri(url)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().value("Cache-Control", value -> assertThat(value).contains("immutable"))
                .expectBody(byte[].class)
                .returnResult()
                .getResponseBody();
        assertThat(Arrays.equals(served, PNG)).isTrue();
    }

    @Test
    void upload_shouldCheckContentNotFilename() {
        String token = registerAndLogin("image-fake@example.com");
        ExerciseResponse exercise = createExercise(token);

        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        upload(token, exercise.id(), html, "image.jpg").expectStatus().isBadRequest();
    }

    @Test
    void replaceAndDelete_shouldRemoveOldImages() {
        String token = registerAndLogin("image-replace@example.com");
        ExerciseResponse exercise = createExercise(token);
        String first = uploadedUrl(token, exercise.id(), PNG);

        String second = uploadedUrl(token, exercise.id(), JPEG);
        assertThat(second).isNotEqualTo(first);
        restTestClient.get().uri(first).exchange().expectStatus().isNotFound();
        restTestClient.get().uri(second).exchange().expectHeader().contentType(MediaType.IMAGE_JPEG);

        restTestClient.delete().uri("/api/exercises/" + exercise.id() + "/image")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
        restTestClient.get().uri(second).exchange().expectStatus().isNotFound();

        String third = uploadedUrl(token, exercise.id(), PNG);
        restTestClient.delete().uri("/api/exercises/" + exercise.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();
        restTestClient.get().uri(third).exchange().expectStatus().isNotFound();
    }

    @Test
    void upload_toSomeoneElsesOrSystemExercise_shouldBeRejected() {
        String owner = registerAndLogin("image-a@example.com");
        String other = registerAndLogin("image-b@example.com");
        ExerciseResponse exercise = createExercise(owner);

        upload(other, exercise.id(), PNG, "image.png").expectStatus().isNotFound();

        Long systemExerciseId = restTestClient.get().uri("/api/exercises?search=Plank")
                .header("Authorization", "Bearer " + other)
                .exchange()
                .expectBody(ExerciseResponse[].class)
                .returnResult()
                .getResponseBody()[0].id();
        upload(other, systemExerciseId, PNG, "image.png").expectStatus().isForbidden();
    }
}
