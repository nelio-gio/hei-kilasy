package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Grade transcript of a student.
 */
@Component
public class GradeReport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> build(long studentId) {
        List<Map<String, Object>> students = jdbcTemplate.queryForList("SELECT * FROM student WHERE id = ?", studentId);
        if (students.isEmpty()) {
            return null;
        }
        Map<String, Object> student = students.get(0);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT e.title, e.deadline, e.duration_minutes, e.max_points, s.id AS submission_id, "
                        + "s.submitted_at, s.raw_points, s.feedback, es.started_at "
                        + "FROM exercise e LEFT JOIN submission s ON s.exercise_id = e.id AND s.student_id = ? "
                        + "LEFT JOIN exercise_start es ON es.exercise_id = e.id AND es.student_id = ? "
                        + "ORDER BY e.deadline NULLS LAST, e.title",
                studentId, studentId);

        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int graded = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> row : rows) {
            Map<String, Object> line = new LinkedHashMap<>();
            LocalDate deadline = row.get("deadline") == null ? null
                    : ((Date) row.get("deadline")).toLocalDate();
            Integer durationMinutes = row.get("duration_minutes") == null ? null
                    : ((Number) row.get("duration_minutes")).intValue();
            LocalDateTime startedAt = row.get("started_at") == null ? null
                    : ((Timestamp) row.get("started_at")).toLocalDateTime();

            line.put("exercise", row.get("title"));
            if (deadline != null) {
                line.put("deadline", deadline);
            }
            if (durationMinutes != null) {
                line.put("durationMinutes", durationMinutes);
                line.put("startedAt", startedAt);
                line.put("timeUsedMinutes", null);
            }

            if (row.get("submission_id") == null) {
                boolean expired = deadline != null && deadline.isBefore(now.toLocalDate());
                if (durationMinutes != null && startedAt != null
                        && now.isAfter(startedAt.plusMinutes(durationMinutes))) {
                    expired = true;
                }
                line.put("status", expired ? "NON RENDU" : "À RENDRE");
                line.put("grade", null);
            } else {
                LocalDateTime submittedAt = ((Timestamp) row.get("submitted_at")).toLocalDateTime();
                line.put("submittedAt", submittedAt);
                line.put("late", deadline != null && submittedAt.toLocalDate().isAfter(deadline));
                if (durationMinutes != null && startedAt != null) {
                    line.put("timeUsedMinutes", timeUsedMinutes(startedAt, submittedAt, durationMinutes));
                }
                if (row.get("raw_points") == null) {
                    line.put("status", "EN ATTENTE DE CORRECTION");
                    line.put("grade", null);
                } else {
                    BigDecimal raw = (BigDecimal) row.get("raw_points");
                    BigDecimal max = (BigDecimal) row.get("max_points");
                    BigDecimal grade = Exercise.gradeOutOf20(raw.doubleValue(), max.doubleValue(), deadline,
                            submittedAt);
                    line.put("status", "CORRIGÉ");
                    line.put("grade", grade);
                    line.put("feedback", row.get("feedback"));
                    total = total.add(grade);
                    graded++;
                }
            }
            lines.add(line);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reference", student.get("reference"));
        report.put("student", student.get("full_name"));
        report.put("email", student.get("email"));
        report.put("cohort", student.get("cohort"));
        report.put("exercises", lines);
        report.put("average", graded == 0 ? null : total.divide(new BigDecimal(graded), 2, RoundingMode.HALF_UP));
        return report;
    }

    private double timeUsedMinutes(LocalDateTime startedAt, LocalDateTime endedAt, int durationMinutes) {
        long seconds = Duration.between(startedAt, endedAt).getSeconds();
        long maxSeconds = durationMinutes * 60L;
        seconds = Math.max(0, Math.min(seconds, maxSeconds));
        return BigDecimal.valueOf(seconds / 60.0).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
