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

    private final LicniRekordService licniRekordService;
    private final NapredakService napredakService;

    public ProgressController(LicniRekordService licniRekordService, NapredakService napredakService) {
        this.licniRekordService = licniRekordService;
        this.napredakService = napredakService;
    }

    @GetMapping("/summary")
    public ProgressSummaryResponse summary(@AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.summary(principal);
    }

    @GetMapping("/records")
    public List<LicniRekordResponse> records(@RequestParam(required = false) Long vezbaId,
                                             @RequestParam(required = false) Long treningId,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        return licniRekordService.search(vezbaId, treningId, principal);
    }

    @GetMapping("/exercises/{vezbaId}")
    public List<VezbaNapredakResponse> exerciseHistory(@PathVariable Long vezbaId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.exerciseHistory(vezbaId, principal);
    }

    @GetMapping("/goal")
    public BodyGoalResponse getGoal(@AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.getGoal(principal);
    }

    @PutMapping("/goal")
    public BodyGoalResponse updateGoal(@Valid @RequestBody BodyGoalRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.updateGoal(request, principal);
    }

    @GetMapping("/weight")
    public List<MasaLogResponse> weightHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.weightHistory(from, to, principal);
    }

    @PutMapping("/weight/{datum}")
    public MasaLogResponse logWeight(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum,
                                     @Valid @RequestBody MasaRequest request,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return napredakService.logWeight(datum, request, principal);
    }

    @DeleteMapping("/weight/{datum}")
    public ResponseEntity<Void> deleteWeight(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datum,
                                             @AuthenticationPrincipal CustomUserDetails principal) {
        napredakService.deleteWeight(datum, principal);
        return ResponseEntity.noContent().build();
    }
}
