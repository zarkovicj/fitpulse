package com.fitpulse.backend.template;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.exercise.ExerciseRepository;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.template.dto.TemplateRequest;
import com.fitpulse.backend.template.dto.TemplateResponse;
import com.fitpulse.backend.template.dto.TemplateExerciseRequest;
import com.fitpulse.backend.template.dto.TemplateExerciseResponse;
import com.fitpulse.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final OwnershipGuard ownershipGuard;

    public TemplateService(TemplateRepository templateRepository,
                            ExerciseRepository exerciseRepository,
                            UserRepository userRepository,
                            OwnershipGuard ownershipGuard) {
        this.templateRepository = templateRepository;
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> findAll(CustomUserDetails principal) {
        return templateRepository.findVisible(ownershipGuard.viewerIdForQuery(principal)).stream()
                .map(TemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getById(Long id, CustomUserDetails principal) {
        return TemplateResponse.from(findVisibleOrThrow(id, principal));
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request, CustomUserDetails principal) {
        Long ownerId = ownershipGuard.resolveOwnerIdForCreate(principal);
        Template template = Template.create(
                request.name(),
                request.description(),
                ownerId != null ? userRepository.getReferenceById(ownerId) : null);

        if (request.exercises() != null) {
            request.exercises().forEach(item -> addItem(template, item));
        }

        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public TemplateResponse update(Long id, TemplateRequest request, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);

        template.setName(request.name());
        template.setDescription(request.description());

        if (request.exercises() != null) {
            template.clearExercises();
            templateRepository.flush(); // stari redovi moraju da se obrisu pre inserta, zbog UNIQUE(id_template, redni_broj)
            request.exercises().forEach(item -> addItem(template, item));
            templateRepository.flush();
        }

        return TemplateResponse.from(template);
    }

    @Transactional
    public void delete(Long id, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        templateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateExerciseResponse> getExercises(Long id, CustomUserDetails principal) {
        return findVisibleOrThrow(id, principal).getExercises().stream()
                .map(TemplateExerciseResponse::from)
                .toList();
    }

    @Transactional
    public TemplateExerciseResponse addExercise(Long id, TemplateExerciseRequest request, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        TemplateExercise item = addItem(template, request);
        templateRepository.flush(); // da novi red dobije id pre mapiranja u response
        return TemplateExerciseResponse.from(item);
    }

    @Transactional
    public TemplateExerciseResponse updateExercise(Long id, Long itemId, TemplateExerciseRequest request,
                                                CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        TemplateExercise item = findItemOrThrow(template, itemId);

        item.setExercise(resolveExercise(request.exerciseId(), template.getOwnerId()));
        item.setSetCount(request.setCount());
        item.setReps(request.reps());
        item.setWeight(request.weight());

        return TemplateExerciseResponse.from(item);
    }

    @Transactional
    public void removeExercise(Long id, Long itemId, CustomUserDetails principal) {
        Template template = findModifiableOrThrow(id, principal);
        template.removeExercise(findItemOrThrow(template, itemId));
    }

    private TemplateExercise addItem(Template template, TemplateExerciseRequest item) {
        Exercise exercise = resolveExercise(item.exerciseId(), template.getOwnerId());
        return template.addExercise(exercise, item.setCount(), item.reps(), item.weight());
    }

    // u template moze samo sistemska vezba ili vezba koju je napravio vlasnik template
    private Exercise resolveExercise(Long exerciseId, Long templateOwnerId) {
        return exerciseRepository.findById(exerciseId)
                .filter(exercise -> exercise.getOwnerId() == null || exercise.getOwnerId().equals(templateOwnerId))
                .orElseThrow(() -> ApiException.badRequest("Vežba " + exerciseId + " nije dostupna"));
    }

    private Template findVisibleOrThrow(Long id, CustomUserDetails principal) {
        return templateRepository.findWithExercisesById(id)
                .filter(template -> ownershipGuard.canView(template.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Template nije pronađen"));
    }

    private Template findModifiableOrThrow(Long id, CustomUserDetails principal) {
        Template template = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(template.getOwnerId(), principal);
        return template;
    }

    private TemplateExercise findItemOrThrow(Template template, Long itemId) {
        return template.getExercises().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena u template-u"));
    }
}
