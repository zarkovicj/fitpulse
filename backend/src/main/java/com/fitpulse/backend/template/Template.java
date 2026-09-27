package com.fitpulse.backend.template;

import com.fitpulse.backend.exercise.Exercise;
import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<TemplateExercise> exercises = new ArrayList<>();

    public static Template create(String name, String description, User createdBy) {
        Template template = new Template();
        template.name = name;
        template.description = description;
        template.createdBy = createdBy;
        return template;
    }

    public Long getOwnerId() {
        return createdBy != null ? createdBy.getId() : null;
    }

    public TemplateExercise addExercise(Exercise exercise, int setCount, int reps, BigDecimal weight) {
        int nextPosition = exercises.stream().mapToInt(TemplateExercise::getPosition).max().orElse(0) + 1;
        TemplateExercise item = TemplateExercise.create(this, exercise, setCount, reps, weight, nextPosition);
        exercises.add(item);
        return item;
    }

    public void removeExercise(TemplateExercise item) {
        exercises.remove(item);
    }

    public void clearExercises() {
        exercises.clear();
    }
}
