package com.fitpulse.backend.exercise;

import com.fitpulse.backend.exercise.dto.VezbaRequest;
import com.fitpulse.backend.exercise.dto.VezbaResponse;
import com.fitpulse.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class VezbaController {

    private final VezbaService vezbaService;

    public VezbaController(VezbaService vezbaService) {
        this.vezbaService = vezbaService;
    }

    @GetMapping
    public List<VezbaResponse> search(@RequestParam(required = false) MisicnaGrupa muscleGroup,
                                       @RequestParam(required = false) String search,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return vezbaService.search(muscleGroup, search, principal);
    }

    @GetMapping("/{id}")
    public VezbaResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return vezbaService.getById(id, principal);
    }

    @PostMapping
    public ResponseEntity<VezbaResponse> create(@Valid @RequestBody VezbaRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vezbaService.create(request, principal));
    }

    @PutMapping("/{id}")
    public VezbaResponse update(@PathVariable Long id,
                                 @Valid @RequestBody VezbaRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return vezbaService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        vezbaService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}
