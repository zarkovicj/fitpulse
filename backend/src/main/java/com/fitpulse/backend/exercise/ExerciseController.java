package com.fitpulse.backend.exercise;

import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> search(@RequestParam(required = false) MuscleGroup muscleGroup,
                                       @RequestParam(required = false) String search,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return exerciseService.search(muscleGroup, search, principal);
    }

    @GetMapping("/{id}")
    public ExerciseResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return exerciseService.getById(id, principal);
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody ExerciseRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exerciseService.create(request, principal));
    }

    @PutMapping("/{id}")
    public ExerciseResponse update(@PathVariable Long id,
                                 @Valid @RequestBody ExerciseRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return exerciseService.update(id, request, principal);
    }

    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExerciseResponse uploadImage(@PathVariable Long id,
                                     @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal CustomUserDetails principal) throws IOException {
        return exerciseService.uploadImage(id, file.getBytes(), principal);
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        exerciseService.deleteImage(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        exerciseService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
