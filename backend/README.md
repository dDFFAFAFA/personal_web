# Backend

## Development
1. Ensure Java 17 and Maven are installed.
2. Start the server: `mvn spring-boot:run`

The default profile is `dev` (H2 in-memory DB, `create-drop`, Flyway disabled).

## Production
Set the active profile and database variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

File uploads are stored at `app.upload.path` (default: `./uploads/papers`).
