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
public class TreningController {

    private final TreningService treningService;

    public TreningController(TreningService treningService) {
        this.treningService = treningService;
    }

    @GetMapping
    public PageResponse<TreningSummaryResponse> history(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.history(page, size, principal);
    }

    @GetMapping("/active")
    public ResponseEntity<TreningResponse> active(@AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.getActive(principal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public TreningResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.getById(id, principal);
    }

    @PostMapping
    public ResponseEntity<TreningResponse> start(@Valid @RequestBody StartTreningRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treningService.start(request, principal));
    }

    @PutMapping("/{id}/finish")
    public TreningResponse finish(@PathVariable Long id,
                                  @RequestBody(required = false) FinishTreningRequest request,
                                  @AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.finish(id, request, principal);
    }

    @PutMapping("/{id}/cancel")
    public TreningResponse cancel(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.cancel(id, principal);
    }

    @PostMapping("/{id}/exercises")
    public ResponseEntity<TreningVezbaResponse> addExercise(@PathVariable Long id,
                                                            @Valid @RequestBody TreningVezbaRequest request,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treningService.addExercise(id, request, principal));
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}")
    public ResponseEntity<Void> removeExercise(@PathVariable Long id,
                                               @PathVariable Long exerciseId,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        treningService.removeExercise(id, exerciseId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/exercises/{exerciseId}/sets")
    public ResponseEntity<TreningSerijaResponse> addSet(@PathVariable Long id,
                                                        @PathVariable Long exerciseId,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(treningService.addSet(id, exerciseId, principal));
    }

    @PutMapping("/{id}/exercises/{exerciseId}/sets/{setId}")
    public TreningSerijaResponse updateSet(@PathVariable Long id,
                                           @PathVariable Long exerciseId,
                                           @PathVariable Long setId,
                                           @Valid @RequestBody TreningSerijaRequest request,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return treningService.updateSet(id, exerciseId, setId, request, principal);
    }

    @DeleteMapping("/{id}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<Void> removeSet(@PathVariable Long id,
                                          @PathVariable Long exerciseId,
                                          @PathVariable Long setId,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        treningService.removeSet(id, exerciseId, setId, principal);
        return ResponseEntity.noContent().build();
    }
}
