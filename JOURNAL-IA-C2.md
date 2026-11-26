# Journal IA — Kilasy — C2

Outil utilisé : OpenCode dans IntelliJ IDEA.

## Changement C2 — notification SMS

### Échange 1 — envoi du SMS
Demande : ajouter un SMS simulé après la publication de la note, sans que l'échec du SMS remette en cause la note.
Proposition : l'IA a proposé d'envoyer l'email comme avant, puis un SMS avec le numéro `student.phone`, le titre et la note, dans un `try/catch`.
Décision : proposition gardée.
Pourquoi : la note est déjà enregistrée avant les notifications ; une panne SMS ne doit pas faire échouer la requête.
Vérification : soumission 7 notée 8/10 (1 jour de retard) → réponse 200, note 14,4 en base, email et SMS présents dans les logs.

### Échange 2 — fournisseur interchangeable
Demande : changer de fournisseur SMS sans modifier le code de la notation.
Proposition : l'IA a proposé une interface `SmsSender` et un bean simulé dans `KilasyApplication.java`, injecté dans `SubmissionController`.
Décision : approche validée.
Pourquoi : la notation doit dépendre de l'interface, pas d'un fournisseur concret ; changer de fournisseur doit seulement consister à remplacer le bean.
Vérification : relecture de `SubmissionController.grade` : appel uniquement à `smsSender.send(...)`.

### Échange 3 — panne simulée
Demande : simuler une panne SMS pour vérifier que la note reste publiée.
Proposition : l'IA a proposé de lever une exception temporaire dans le bean, tester, puis restaurer le code normal.
Décision : accepté pour le test, puis bean normal restauré.
Vérification : avec la panne, réponse 200 et note 14,4 en base, dans le relevé et le CSV ; email parti et log d'échec présent. Après restauration, SMS normal de nouveau. Données des soumissions 5 et 7 remises à zéro.

## Bilan

- Fichiers modifiés : `KilasyApplication.java` (interface + bean), `SubmissionController.java` (appel SMS protégé).
- Fichier ajouté : `JOURNAL-IA-C2.md`.
- Aucune nouvelle classe métier, aucun fichier déplacé.
