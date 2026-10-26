# SBSA Fraud Monitoring Service

Real-time transaction processing with synchronous fraud screening, built for the
Mindworx WM03 software development module (SDP simulation of the Standard Bank
project brief).

Every transaction is validated and screened by the fraud engine inside the same
request, before it is saved, so a suspicious payment is held as `Flagged` and never
settles automatically.

## Tech stack

| Area | Tool |
| --- | --- |
| Language | Java 21 (LTS) |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Build | Gradle 9 (wrapper included) |
| Database | MySQL 8, schema managed by Liquibase |
| Artifacts | Sonatype Nexus Repository (Docker) |
| Tests | JUnit 5 |

## Repository layout

```
.
├── .gitignore                  ignores IDE settings at the repository root
├── README.md
└── sbsa-fraud/                 Gradle project
    ├── build.gradle            dependencies and Nexus publishing
    ├── settings.gradle         routes every download through Nexus
    ├── docker-compose.yml      local Nexus
    └── src/main/resources/
        ├── application.yml     reads DB_USERNAME / DB_PASSWORD from the environment
        └── db/changelog/       Liquibase changesets (schema + dev seed data)
```

## Prerequisites

- JDK 21
- MySQL 8 running on `localhost:3306`
- Docker Desktop (for Nexus)
- Git

## Getting started

### 1. Database (once)

As MySQL root, create the empty database and the application login:

```sql
CREATE DATABASE sbsa_fraud CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'sbsa_app'@'localhost' IDENTIFIED BY '<password>';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES, TRIGGER
    ON sbsa_fraud.* TO 'sbsa_app'@'localhost';
SET PERSIST log_bin_trust_function_creators = 1;   -- lets Liquibase create triggers
```

Do not create tables by hand. Liquibase builds the whole schema on first start.

### 2. Nexus (once)

```
cd sbsa-fraud
docker compose up -d
```

Open http://localhost:8081, complete the setup wizard (disable anonymous access),
then:

- create a `maven2 (proxy)` repository `gradle-plugins` for `https://plugins.gradle.org/m2/`
  and add it to the `maven-public` group
- create a role `deployer` with privilege `nx-repository-view-maven2-*-*`
- create a user `gradle-deployer` with that role

Save the Nexus login outside the repository, in `%USERPROFILE%\.gradle\gradle.properties`:

```
nexusUsername=gradle-deployer
nexusPassword=<password>
```

### 3. Database login for the app

```
setx DB_USERNAME sbsa_app
setx DB_PASSWORD <password>
```

Restart the IDE afterwards so it picks up the new variables.

### 4. Build and run

```
cd sbsa-fraud
.\gradlew build       # compile and test; downloads come through Nexus
.\gradlew bootRun     # applies Liquibase changesets, starts on port 8080
.\gradlew publish     # uploads the JAR to Nexus maven-snapshots
```

## Working on the code

- `main` is protected: changes arrive only by pull request.
- Branch from `develop` as `feature/<short-name>` or `fix/<short-name>`, and open the
  pull request into `develop`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`.
- Schema changes go in a **new** Liquibase changeset. A changeset that has been
  applied is never edited.
- Never commit passwords. Database and Nexus credentials live in environment
  variables and `~/.gradle/gradle.properties`.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Access denied for user 'sbsa_app'` | Check `DB_USERNAME` is lower case and `DB_PASSWORD` matches MySQL; restart the IDE after `setx` |
| `no changelog could be found` | `db.changelog-master.yaml` must be in `src/main/resources/db/changelog/` |
| `401 Unauthorized` from Nexus | `gradle.properties` must be named exactly that, in `%USERPROFILE%\.gradle\` |
| `repository 'MavenRepo' was added by build file` | Remove `repositories { mavenCentral() }` from `build.gradle`; repositories belong in `settings.gradle` |
| `ERROR 1419 ... binary logging is enabled` | Run `SET PERSIST log_bin_trust_function_creators = 1` as root |
| Docker: `failed to connect to the docker API` | Start Docker Desktop and wait for **Engine running** |
