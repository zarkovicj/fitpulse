package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.PageResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.workout.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping
    public PageResponse<WorkoutSummaryResponse> history(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.history(page, size, principal);
    }

    @GetMapping("/active")
    public ResponseEntity<WorkoutResponse> active(@AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.getActive(principal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public WorkoutResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.getById(id, principal);
    }

    @PostMapping
    public ResponseEntity<WorkoutResponse> start(@Valid @RequestBody StartWorkoutRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutService.start(request, principal));
    }

    @PutMapping("/{id}/finish")
    public WorkoutResponse finish(@PathVariable Long id,
                                  @RequestBody(required = false) FinishWorkoutRequest request,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.finish(id, request, principal);
    }

    @PutMapping("/{id}/cancel")
    public WorkoutResponse cancel(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.cancel(id, principal);
    }

    @PostMapping("/{id}/exercises")
    public ResponseEntity<WorkoutExerciseResponse> addExercise(@PathVariable Long id,
                                                            @Valid @RequestBody WorkoutExerciseRequest request,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutService.addExercise(id, request, principal));
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}")
    public ResponseEntity<Void> removeExercise(@PathVariable Long id,
                                               @PathVariable Long exerciseId,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        workoutService.removeExercise(id, exerciseId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/exercises/{exerciseId}/sets")
    public ResponseEntity<WorkoutSetResponse> addSet(@PathVariable Long id,
                                                        @PathVariable Long exerciseId,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutService.addSet(id, exerciseId, principal));
    }

    @PutMapping("/{id}/exercises/{exerciseId}/sets/{setId}")
    public WorkoutSetResponse updateSet(@PathVariable Long id,
                                           @PathVariable Long exerciseId,
                                           @PathVariable Long setId,
                                           @Valid @RequestBody WorkoutSetRequest request,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return workoutService.updateSet(id, exerciseId, setId, request, principal);
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<Void> removeSet(@PathVariable Long id,
                                          @PathVariable Long exerciseId,
                                          @PathVariable Long setId,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        workoutService.removeSet(id, exerciseId, setId, principal);
        return ResponseEntity.noContent().build();
    }
}
