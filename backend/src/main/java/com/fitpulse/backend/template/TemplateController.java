package com.fitpulse.backend.template;

import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateVezbaRequest;
import com.fitpulse.backend.template.dto.TemplateVezbaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateResponse> findAll(@AuthenticationPrincipal CustomUserDetails principal) {
        return templateService.findAll(principal);
    }

    @GetMapping("/{id}")
    public TemplateResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return templateService.getById(id, principal);
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest request,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request, principal));
    }

    @PutMapping("/{id}")
    public TemplateResponse update(@PathVariable Long id,
                                   @Valid @RequestBody TemplateRequest request,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        return templateService.update(id, request, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        templateService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exercises")
    public List<TemplateVezbaResponse> getExercises(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        return templateService.getExercises(id, principal);
    }

    @PostMapping("/{id}/exercises")
    public ResponseEntity<TemplateVezbaResponse> addExercise(@PathVariable Long id,
                                                             @Valid @RequestBody TemplateVezbaRequest request,
                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.addExercise(id, request, principal));
    }

    @PutMapping("/{id}/exercises/{itemId}")
    public TemplateVezbaResponse updateExercise(@PathVariable Long id,
                                                @PathVariable Long itemId,
                                                @Valid @RequestBody TemplateVezbaRequest request,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        return templateService.updateExercise(id, itemId, request, principal);
    }

    @DeleteMapping("/{id}/exercises/{itemId}")
    public ResponseEntity<Void> removeExercise(@PathVariable Long id,
                                               @PathVariable Long itemId,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        templateService.removeExercise(id, itemId, principal);
        return ResponseEntity.noContent().build();
    }
}
