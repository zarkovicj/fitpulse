package com.fitpulse.backend.workout;

import com.fitpulse.backend.exercise.Exercise;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_exercise")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_count", nullable = false)
    private int setCount;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "rest_after")
    private Integer restAfter;

    @OneToMany(mappedBy = "workoutExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<WorkoutSet> sets = new ArrayList<>();

    static WorkoutExercise create(Workout workout, Exercise exercise, int position) {
        WorkoutExercise workoutExercise = new WorkoutExercise();
        workoutExercise.workout = workout;
        workoutExercise.exercise = exercise;
        workoutExercise.position = position;
        return workoutExercise;
    }

    public WorkoutSet addSet(int reps, BigDecimal weight) {
        return addSet(reps, weight, null);
    }

    public WorkoutSet addSet(int reps, BigDecimal weight, Integer restAfter) {
        WorkoutSet set = WorkoutSet.create(this, sets.size() + 1, reps, weight, restAfter);
        sets.add(set);
        refresh();
        return set;
    }

    public void removeSet(WorkoutSet set) {
        sets.remove(set);
        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).renumber(i + 1);
        }
        refresh();
    }

    public void updateSet(WorkoutSet set, int reps, BigDecimal weight, boolean completed, Integer restAfter) {
        set.update(reps, weight, completed, restAfter);
        refresh();
    }

    void renumber(int position) {
        this.position = position;
    }

    // set_count i completed su izvedeni iz serija
    private void refresh() {
        setCount = sets.size();
        completed = !sets.isEmpty() && sets.stream().allMatch(WorkoutSet::isCompleted);
    }
}
