package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Exercise {

    private Long id;
    private Long teacherId;
    private String title;
    private String description;
    private double maxPoints;
    private LocalDate deadline;
    private Integer durationMinutes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(double maxPoints) {
        this.maxPoints = maxPoints;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Grade out of 20, after the late penalty: 10% per started day late, capped at 50%.
     * The deadline day itself is not late. A null deadline means no late penalty.
     */
    public static BigDecimal gradeOutOf20(double rawPoints, double maxPoints, LocalDate deadline,
            LocalDateTime submittedAt) {
        BigDecimal base = BigDecimal.valueOf(rawPoints)
                .multiply(BigDecimal.valueOf(20))
                .divide(BigDecimal.valueOf(maxPoints), 10, RoundingMode.HALF_UP);
        long daysLate = deadline == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(deadline, submittedAt.toLocalDate()));
        BigDecimal penaltyRate = BigDecimal.valueOf(Math.min(daysLate, 5))
                .multiply(new BigDecimal("0.10"));
        BigDecimal grade = base.multiply(BigDecimal.ONE.subtract(penaltyRate));
        if (grade.signum() < 0) {
            grade = BigDecimal.ZERO;
        }
        return grade.setScale(1, RoundingMode.HALF_UP);
    }
}
