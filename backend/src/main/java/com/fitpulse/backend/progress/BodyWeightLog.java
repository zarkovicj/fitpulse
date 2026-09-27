package com.fitpulse.backend.progress;

import com.fitpulse.backend.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "body_weight_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyWeightLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter
    @Column(nullable = false)
    private BigDecimal weight;

    @Column(nullable = false)
    private LocalDate date;

    public static BodyWeightLog create(User user, LocalDate date, BigDecimal weight) {
        BodyWeightLog log = new BodyWeightLog();
        log.user = user;
        log.date = date;
        log.weight = weight;
        return log;
    }
}
