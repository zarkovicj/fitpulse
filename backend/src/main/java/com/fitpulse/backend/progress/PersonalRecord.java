package com.fitpulse.backend.progress;

import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "personal_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecordType type;

    private BigDecimal weight;

    @Column(nullable = false)
    private int reps;

    @Column(name = "estimated_1rm")
    private BigDecimal estimated1rm;

    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;

    @Column(name = "workout_id")
    private Long workoutId;

    public static PersonalRecord create(User user, Exercise exercise, RecordType type) {
        PersonalRecord personalRecord = new PersonalRecord();
        personalRecord.user = user;
        personalRecord.exercise = exercise;
        personalRecord.type = type;
        return personalRecord;
    }

    public void record(int reps, BigDecimal weight, BigDecimal estimated1rm, Long workoutId, Instant achievedAt) {
        this.reps = reps;
        this.weight = weight;
        this.estimated1rm = estimated1rm;
        this.workoutId = workoutId;
        this.achievedAt = achievedAt;
    }
}
