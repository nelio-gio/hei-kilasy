# Journal IA — Kilasy

Outil utilisé : OpenCode, dans IntelliJ IDEA.

## Changement C1 — pénalité de retard

### Échange 1 — implémentation
Demande : « Applique −10 % par jour de retard entamé, avec un plafond de −50 %, et la même note dans la correction, le relevé et l'export CSV. »
Proposition : ajouter une méthode statique `Exercise.gradeOutOf20(...)` qui calcule la note sur 20 avec `BigDecimal`, compte les jours de retard avec `ChronoUnit.DAYS`, plafonne la pénalité à 5 jours, borne le résultat à 0 et arrondit à une décimale en `HALF_UP`. Remplacer les trois calculs dans `SubmissionController` (`get` et `grade`), `GradeReport` et `CsvExport`.
Décision : gardé.
Pourquoi : une seule formule et un seul arrondi évitent les écarts constatés (13,8 dans le relevé contre 13,7 dans le CSV). Aucune nouvelle classe ni nouvelle couche, conformément à la consigne « ne pas réorganiser ».
Vérification :
- correction : soumission 2 (1 jour) → 14,4 ; soumission 1 (à l'heure) → 16,0 ; soumission 3 (3 jours) → 11,2 ; soumission 4 (8 jours) → 8,0 ; soumission 4 notée 0 → 0,0.
- relevé : David → 8,0 et 12,0 ; Alice → 16,0, 17,0 et 16,5.
- export CSV : 14,4 ; 11,2 ; 8,0 ; 12,0 ; 16,0 ; 17,0 ; 16,5 ; 13,8.
- Les trois chemins donnent la même note pour chaque soumission.

## Bilan

- Fichiers modifiés : `Exercise.java`, `SubmissionController.java`, `GradeReport.java`, `CsvExport.java`.
- Fichier ajouté : `JOURNAL-IA-C1.md`.
- Aucune nouvelle classe métier, aucun fichier déplacé.
