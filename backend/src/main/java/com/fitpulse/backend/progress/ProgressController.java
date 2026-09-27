package com.fitpulse.backend.progress;

import com.fitpulse.backend.progress.dto.*;
import com.fitpulse.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final PersonalRecordService personalRecordService;
    private final ProgressService progressService;

    public ProgressController(PersonalRecordService personalRecordService, ProgressService progressService) {
        this.personalRecordService = personalRecordService;
        this.progressService = progressService;
    }

    @GetMapping("/summary")
    public ProgressSummaryResponse summary(@AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.summary(principal);
    }

    @GetMapping("/records")
    public List<PersonalRecordResponse> records(@RequestParam(required = false) Long exerciseId,
                                             @RequestParam(required = false) Long workoutId,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        return personalRecordService.search(exerciseId, workoutId, principal);
    }

    @GetMapping("/exercises/{exerciseId}")
    public List<ExerciseProgressResponse> exerciseHistory(@PathVariable Long exerciseId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.exerciseHistory(exerciseId, principal);
    }

    @GetMapping("/goal")
    public BodyGoalResponse getGoal(@AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.getGoal(principal);
    }

    @PutMapping("/goal")
    public BodyGoalResponse updateGoal(@Valid @RequestBody BodyGoalRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.updateGoal(request, principal);
    }

    @GetMapping("/weight")
    public List<WeightLogResponse> weightHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.weightHistory(from, to, principal);
    }

    @PutMapping("/weight/{date}")
    public WeightLogResponse logWeight(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @Valid @RequestBody WeightRequest request,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return progressService.logWeight(date, request, principal);
    }

    @DeleteMapping("/weight/{date}")
    public ResponseEntity<Void> deleteWeight(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        progressService.deleteWeight(date, principal);
        return ResponseEntity.noContent().build();
    }
}
