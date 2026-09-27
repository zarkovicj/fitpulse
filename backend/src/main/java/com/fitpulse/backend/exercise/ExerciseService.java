package com.fitpulse.backend.exercise;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.common.LikeSearch;
import com.fitpulse.backend.common.OwnershipGuard;
import com.fitpulse.backend.exercise.dto.ExerciseRequest;
import com.fitpulse.backend.exercise.dto.ExerciseResponse;
import com.fitpulse.backend.image.ImageStorage;
import com.fitpulse.backend.image.ImageType;
import com.fitpulse.backend.security.CustomUserDetails;
import com.fitpulse.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final OwnershipGuard ownershipGuard;
    private final ImageStorage imageStorage;

    public ExerciseService(ExerciseRepository exerciseRepository,
                         UserRepository userRepository,
                         OwnershipGuard ownershipGuard,
                         ImageStorage imageStorage) {
        this.exerciseRepository = exerciseRepository;
        this.userRepository = userRepository;
        this.ownershipGuard = ownershipGuard;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> search(MuscleGroup muscleGroup, String search, CustomUserDetails principal) {
        return exerciseRepository.search(ownershipGuard.viewerIdForQuery(principal), muscleGroup, LikeSearch.escape(search)).stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponse getById(Long id, CustomUserDetails principal) {
        return ExerciseResponse.from(findVisibleOrThrow(id, principal));
    }

    @Transactional
    public ExerciseResponse create(ExerciseRequest request, CustomUserDetails principal) {
        Long ownerId = ownershipGuard.resolveOwnerIdForCreate(principal);

        Exercise exercise = Exercise.create(
                request.name(),
                request.muscleGroup(),
                request.description(),
                ownerId != null ? userRepository.getReferenceById(ownerId) : null);
        exercise.setVideoUrl(blankToNull(request.videoUrl()));

        return ExerciseResponse.from(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseResponse update(Long id, ExerciseRequest request, CustomUserDetails principal) {
        Exercise exercise = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(exercise.getOwnerId(), principal);

        exercise.setName(request.name());
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setDescription(request.description());
        exercise.setVideoUrl(blankToNull(request.videoUrl()));

        return ExerciseResponse.from(exercise);
    }

    @Transactional
    public void delete(Long id, CustomUserDetails principal) {
        Exercise exercise = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(exercise.getOwnerId(), principal);

        String imageKey = exercise.getImageKey();
        exerciseRepository.delete(exercise);
        exerciseRepository.flush();
        if (imageKey != null) {
            imageStorage.delete(imageKey);
        }
    }

    @Transactional
    public ExerciseResponse uploadImage(Long id, byte[] data, CustomUserDetails principal) {
        Exercise exercise = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(exercise.getOwnerId(), principal);

        if (data.length > ImageType.MAX_BYTES) {
            throw ApiException.badRequest("Slika može imati najviše 2 MB");
        }
        String contentType = ImageType.detect(data)
                .orElseThrow(() -> ApiException.badRequest("Dozvoljene su samo JPEG, PNG i WebP slike"));

        replaceImage(exercise, imageStorage.store(data, contentType));
        return ExerciseResponse.from(exercise);
    }

    @Transactional
    public void deleteImage(Long id, CustomUserDetails principal) {
        Exercise exercise = findVisibleOrThrow(id, principal);
        ownershipGuard.assertCanModify(exercise.getOwnerId(), principal);
        replaceImage(exercise, null);
    }

    private void replaceImage(Exercise exercise, String newKey) {
        String oldKey = exercise.getImageKey();
        exercise.setImageKey(newKey);
        exerciseRepository.flush();
        if (oldKey != null) {
            imageStorage.delete(oldKey);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // tudja vezba se ne prikazuje trenutnom korisniku
    private Exercise findVisibleOrThrow(Long id, CustomUserDetails principal) {
        return exerciseRepository.findById(id)
                .filter(exercise -> ownershipGuard.canView(exercise.getOwnerId(), principal))
                .orElseThrow(() -> ApiException.notFound("Vežba nije pronađena"));
    }
}
