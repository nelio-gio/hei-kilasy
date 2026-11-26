# Kilasy

Kilasy is a programming exercise platform. Teachers publish exercises with a deadline, students submit their code or a link to their GitHub repository, and teachers then grade them. Students receive their grade by email.

Production version: 1.3.0.

## Stack

- Java 21, Spring Boot 4 (Web MVC, JDBC, Mail)
- PostgreSQL 17
- Maven

## Getting started

Pre-requisites: JDK 21, Maven and Docker (see the installation guide on hei-lms.app).

```bash
docker compose up -d
mvn spring-boot:run
```

The API responds on http://localhost:8080.

`docker compose` starts two services:

- PostgreSQL on port 5432;
- Mailpit, a fake mail server: emails sent by Kilasy are displayed at http://localhost:8025.

**The SQL scripts are idempotent**: at every startup the schema and the demo data are created only if missing, and existing data is kept. To start from scratch, run `docker compose down` (PostgreSQL has no persistent volume, so removing the container wipes the data). The demo data is: 2 teachers, 6 students, 5 exercises and 12 submissions.

After changing `schema.sql`, run `docker compose down` then `docker compose up -d`: the script does not alter an existing table.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/exercises` | List exercises |
| GET | `/exercises/{id}` | Exercise details |
| POST | `/exercises` | Create an exercise (teacher) |
| POST | `/exercises/{id}/start` | Start the timer for a timed exercise (student) |
| POST | `/exercises/{id}/submissions` | Submit an exercise (student) |
| GET | `/submissions/{id}` | Submission details |
| PUT | `/submissions/{id}/grade` | Grade a submission (teacher) |
| GET | `/students` | List students |
| GET | `/students/{id}/transcript` | A student's grade transcript |
| GET | `/exports/grades.csv` | Grades export for the registrar's office |

There is no authentication yet: the student or teacher identifier is passed in the request body.

## Examples

Create an exercise:

```bash
curl -X POST http://localhost:8080/exercises \
  -H "Content-Type: application/json" \
  -d '{"title": "Recherche dichotomique", "maxPoints": 20, "deadline": "2027-03-01", "teacherId": 1}'
```

Create a timed exercise without a deadline:

```bash
curl -X POST http://localhost:8080/exercises \
  -H "Content-Type: application/json" \
  -d '{"title": "Exercice minuté", "maxPoints": 20, "durationMinutes": 30, "teacherId": 1}'
```

Start a timed exercise, then submit it:

```bash
curl -X POST http://localhost:8080/exercises/4/start \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1}'

curl -X POST http://localhost:8080/exercises/4/submissions \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "repoUrl": "https://github.com/alice/reservation-api"}'
```

Grade a submission, then check the email received at http://localhost:8025:

```bash
curl -X PUT http://localhost:8080/submissions/5/grade \
  -H "Content-Type: application/json" \
  -d '{"points": 15, "feedback": "Bien, mais commente ton code."}'
```

View a grade transcript and the export:

```bash
curl http://localhost:8080/students/1/transcript
curl http://localhost:8080/exports/grades.csv
```

## Business rules

- A submission contains code, a link to a GitHub repository, or both.
- A student can only submit once per exercise.
- An exercise can have a deadline, a duration in minutes, or both.
- For a timed exercise, the student must call `POST /exercises/{id}/start` before submitting. A submission after the timer ends is refused with HTTP 422.
- A submission sent after the deadline day is marked as late, even for a timed exercise.
- Points are entered on the exercise's scale (10, 20, 40…) and converted to a grade out of 20.
- The transcript shows the time used for each timed exercise.
