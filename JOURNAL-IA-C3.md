# Journal IA — Kilasy — C3

Outil utilisé : OpenCode dans IntelliJ IDEA.

## Changement C3 — exercices minutés

### Échange 1 — durée et démarrage
Demande : ajouter une durée en minutes, en plus ou à la place de l'échéance, avec `POST /exercises/{id}/start`.
Proposition : rendre `deadline` nullable, ajouter `duration_minutes` et une table `exercise_start` (clé exercice/étudiant), avec un démarrage idempotent.
Décision : proposition retenue, car le chrono est propre à chaque étudiant et les exercices sans durée restent inchangés.
Vérification : exercice de 20 min → 1200 s restantes ; second appel → même `startedAt` ; non minuté → 400 ; déjà soumis → 409.

### Échange 2 — refus et relevé
Demande : refuser une soumission après la fin du temps (422) et afficher le temps utilisé dans le relevé, tout en gardant la pénalité C1.
Proposition : contrôler `started_at + durée` avant l'insertion ; ajouter `durationMinutes`, `startedAt` et `timeUsedMinutes` au relevé ; garder `Exercise.gradeOutOf20(...)` pour la note.
Décision : proposition retenue, car aucune soumission tardive n'est enregistrée et la pénalité reste liée à l'échéance.
Vérification : sans démarrage ou chrono expiré → 422 ; 10/20 avec 6 jours de retard → 5,0 ; 5,0 min utilisées ; CSV cohérent (`5,0`).

## Bilan

- Fichiers modifiés : `schema.sql`, `data.sql`, `Exercise.java`, `ExerciseController.java`, `SubmissionController.java`, `GradeReport.java`, `CsvExport.java`, `README.md` ; fichier ajouté : `JOURNAL-IA-C3.md`.
- Schéma : `deadline` nullable, `duration_minutes`, table `exercise_start` ; `docker compose down` puis `up -d` pour recréer la base.
- Exercices 4 et 5 minutés (90 et 60 min). `mvn test` passe, sans test automatisé.
- Aucune nouvelle classe métier, aucun fichier déplacé.
