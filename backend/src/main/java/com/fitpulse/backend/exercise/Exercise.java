package com.fitpulse.backend.exercise;

import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 30)
    private MuscleGroup muscleGroup;

    @Column(name = "image_key", length = 36)
    private String imageKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public static Exercise create(String name, MuscleGroup muscleGroup, String description, User createdBy) {
        Exercise exercise = new Exercise();
        exercise.name = name;
        exercise.muscleGroup = muscleGroup;
        exercise.description = description;
        exercise.createdBy = createdBy;
        return exercise;
    }

    public Long getOwnerId() {
        return createdBy != null ? createdBy.getId() : null;
    }
}
