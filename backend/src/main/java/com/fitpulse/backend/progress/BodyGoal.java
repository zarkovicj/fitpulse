package com.fitpulse.backend.progress;

import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "body_goal")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter(AccessLevel.NONE)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private BigDecimal weight;

    @Column(name = "body_fat_percent")
    private BigDecimal bodyFatPercent;

    public static BodyGoal create(User user) {
        BodyGoal goal = new BodyGoal();
        goal.user = user;
        return goal;
    }
}
