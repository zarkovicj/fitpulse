package com.fitpulse.backend.workout;

import com.fitpulse.backend.common.AppTime;
import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.template.Template;
import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutStatus status;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    public static Workout start(User user, Template template) {
        Workout workout = new Workout();
        workout.user = user;
        workout.template = template;
        workout.startedAt = Instant.now();
        workout.date = AppTime.today();
        workout.status = WorkoutStatus.IN_PROGRESS;
        return workout;
    }

    public WorkoutExercise addExercise(Exercise exercise) {
        WorkoutExercise workoutExercise = WorkoutExercise.create(this, exercise, exercises.size() + 1);
        exercises.add(workoutExercise);
        return workoutExercise;
    }

    public void removeExercise(WorkoutExercise exercise) {
        exercises.remove(exercise);
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).renumber(i + 1);
        }
    }

    public void finish() {
        status = WorkoutStatus.COMPLETED;
        finishedAt = Instant.now();
    }

    public void cancel() {
        status = WorkoutStatus.CANCELLED;
        finishedAt = Instant.now();
    }

    public boolean isInProgress() {
        return status == WorkoutStatus.IN_PROGRESS;
    }

    public Long getTemplateId() {
        return template != null ? template.getId() : null;
    }
}
