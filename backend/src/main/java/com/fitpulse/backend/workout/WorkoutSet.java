package com.fitpulse.backend.workout;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "reps", nullable = false)
    private int reps;

    private BigDecimal weight;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "rest_after")
    private Integer restAfter;

    static WorkoutSet create(WorkoutExercise workoutExercise, int position, int reps, BigDecimal weight,
                                Integer restAfter) {
        WorkoutSet set = new WorkoutSet();
        set.workoutExercise = workoutExercise;
        set.position = position;
        set.reps = reps;
        set.weight = weight;
        set.restAfter = restAfter;
        return set;
    }

    void update(int reps, BigDecimal weight, boolean completed, Integer restAfter) {
        this.reps = reps;
        this.weight = weight;
        this.completed = completed;
        this.restAfter = restAfter;
    }

    void renumber(int position) {
        this.position = position;
    }
}
