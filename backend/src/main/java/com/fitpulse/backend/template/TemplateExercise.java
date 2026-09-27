package com.fitpulse.backend.template;

import com.fitpulse.backend.exercise.Exercise;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "template_exercise")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_count", nullable = false)
    private int setCount;

    @Column(name = "reps", nullable = false)
    private int reps;

    private BigDecimal weight;

    @Setter(AccessLevel.NONE)
    @Column(name = "position", nullable = false)
    private int position;

    static TemplateExercise create(Template template, Exercise exercise, int setCount, int reps,
                                BigDecimal weight, int position) {
        TemplateExercise item = new TemplateExercise();
        item.template = template;
        item.exercise = exercise;
        item.setCount = setCount;
        item.reps = reps;
        item.weight = weight;
        item.position = position;
        return item;
    }
}
