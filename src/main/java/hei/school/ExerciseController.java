package hei.school;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExerciseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/exercises")
    public List<Exercise> list() {
        return jdbcTemplate.query("SELECT * FROM exercise ORDER BY deadline", (rs, i) -> {
            Exercise e = new Exercise();
            e.setId(rs.getLong("id"));
            e.setTeacherId(rs.getLong("teacher_id"));
            e.setTitle(rs.getString("title"));
            e.setDescription(rs.getString("description"));
            e.setMaxPoints(rs.getDouble("max_points"));
            java.sql.Date deadline = rs.getDate("deadline");
            e.setDeadline(deadline == null ? null : deadline.toLocalDate());
            int durationMinutes = rs.getInt("duration_minutes");
            e.setDurationMinutes(rs.wasNull() ? null : durationMinutes);
            return e;
        });
    }

    @GetMapping("/exercises/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        List<Exercise> found = jdbcTemplate.query("SELECT * FROM exercise WHERE id = ?", (rs, i) -> {
            Exercise e = new Exercise();
            e.setId(rs.getLong("id"));
            e.setTeacherId(rs.getLong("teacher_id"));
            e.setTitle(rs.getString("title"));
            e.setDescription(rs.getString("description"));
            e.setMaxPoints(rs.getDouble("max_points"));
            java.sql.Date deadline = rs.getDate("deadline");
            e.setDeadline(deadline == null ? null : deadline.toLocalDate());
            int durationMinutes = rs.getInt("duration_minutes");
            e.setDurationMinutes(rs.wasNull() ? null : durationMinutes);
            return e;
        }, id);
        if (found.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Exercice introuvable"));
        }
        return ResponseEntity.ok(found.get(0));
    }

    @PostMapping("/exercises")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le titre est obligatoire"));
        }

        LocalDate deadline = null;
        if (body.get("deadline") != null) {
            try {
                deadline = LocalDate.parse((String) body.get("deadline"));
            } catch (Exception ex) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "L'échéance doit être au format AAAA-MM-JJ"));
            }
            if (deadline.isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'échéance doit être dans le futur"));
            }
        }

        Integer durationMinutes = null;
        if (body.get("durationMinutes") != null) {
            if (!(body.get("durationMinutes") instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "La durée doit être un nombre de minutes"));
            }
            double duration = ((Number) body.get("durationMinutes")).doubleValue();
            if (duration <= 0 || duration != Math.rint(duration) || duration > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La durée doit être un nombre entier de minutes, strictement positif"));
            }
            durationMinutes = (int) duration;
        }
        if (deadline == null && durationMinutes == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "L'échéance ou la durée est obligatoire"));
        }

        double maxPoints = 20;
        if (body.get("maxPoints") != null) {
            maxPoints = ((Number) body.get("maxPoints")).doubleValue();
        }
        if (maxPoints <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le barème doit être positif"));
        }
        // TODO: get the logged-in teacher once authentication exists
        long teacherId = 1;
        if (body.get("teacherId") != null) {
            teacherId = ((Number) body.get("teacherId")).longValue();
        }

        jdbcTemplate.update(
                "INSERT INTO exercise (teacher_id, title, description, max_points, deadline, duration_minutes) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                teacherId, title, body.get("description"), maxPoints,
                deadline == null ? null : java.sql.Date.valueOf(deadline), durationMinutes);
        Long id = jdbcTemplate.queryForObject("SELECT max(id) FROM exercise", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("maxPoints", maxPoints);
        result.put("deadline", deadline);
        result.put("durationMinutes", durationMinutes);
        System.out.println("Nouvel exercice créé : " + title + " (id " + id + ")");
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/exercises/{id}/start")
    public ResponseEntity<?> start(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (body.get("studentId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "studentId est obligatoire"));
        }
        long studentId = ((Number) body.get("studentId")).longValue();

        List<Map<String, Object>> exercises = jdbcTemplate.queryForList("SELECT * FROM exercise WHERE id = ?", id);
        if (exercises.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Exercice introuvable"));
        }
        Map<String, Object> exercise = exercises.get(0);
        Integer durationMinutes = exercise.get("duration_minutes") == null ? null
                : ((Number) exercise.get("duration_minutes")).intValue();
        if (durationMinutes == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cet exercice n'est pas minuté"));
        }

        List<Map<String, Object>> students = jdbcTemplate.queryForList("SELECT * FROM student WHERE id = ?", studentId);
        if (students.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Étudiant introuvable"));
        }

        Integer alreadySubmitted = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM submission WHERE exercise_id = ? AND student_id = ?", Integer.class, id,
                studentId);
        if (alreadySubmitted != null && alreadySubmitted > 0) {
            return ResponseEntity.status(409).body(Map.of("error", "Cet étudiant a déjà soumis cet exercice"));
        }

        // The start time is written by the application, not by PostgreSQL: both clocks may use
        // different time zones, and the countdown is compared with the application clock.
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO exercise_start (exercise_id, student_id, started_at) VALUES (?, ?, ?) "
                        + "ON CONFLICT (exercise_id, student_id) DO NOTHING",
                id, studentId, Timestamp.valueOf(now));
        Map<String, Object> start = jdbcTemplate.queryForMap(
                "SELECT started_at FROM exercise_start WHERE exercise_id = ? AND student_id = ?", id, studentId);
        LocalDateTime startedAt = ((Timestamp) start.get("started_at")).toLocalDateTime();
        LocalDateTime endsAt = startedAt.plusMinutes(durationMinutes);
        long remainingSeconds = Math.max(0, Duration.between(now, endsAt).getSeconds());

        Map<String, Object> result = new HashMap<>();
        result.put("exerciseId", id);
        result.put("studentId", studentId);
        result.put("durationMinutes", durationMinutes);
        result.put("startedAt", startedAt);
        result.put("endsAt", endsAt);
        result.put("remainingSeconds", remainingSeconds);
        result.put("expired", remainingSeconds == 0);
        return ResponseEntity.ok(result);
    }
}
