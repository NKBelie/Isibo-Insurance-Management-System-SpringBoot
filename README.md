# ISIBO Insurance Management System

Spring Boot, Thymeleaf, Spring Security, Spring Data JPA, and PostgreSQL application for managing insurance members, plans, coverages, providers, claims, payments, officers, and users.

Project audit and supporting materials:

- [Assignment compliance matrix](./docs/requirements-compliance.md)
- [Project requirements and acceptance criteria](./docs/project-requirements.md)
- [Domain model and PostgreSQL relationships](./docs/data-model.md)

## Run locally

Requirements: Java 21, Maven, PostgreSQL, and a PostgreSQL database named `isibo_db` (create/manage it in pgAdmin if preferred).

Set your PostgreSQL username and password in PowerShell before launching the application:

```powershell
$env:DB_USERNAME = "your_postgres_username"
$env:DB_PASSWORD = "your_postgres_password"
mvn spring-boot:run
```

The default database URL is `jdbc:postgresql://localhost:5432/isibo_db`; set `DB_URL` to override it. Database credentials are required from the environment and are not supplied by the application.

Open `http://localhost:8080/register` to create a Customer account using a username, email, and password. After signing in, the customer can submit an insurance application from the private dashboard. New applications start as `PENDING`; an administrator reviews the applicant under **Members** and changes the status to `ACTIVE` or `REJECTED`. Registration uses server-side validation, duplicate checks, BCrypt password hashing, and Spring Security CSRF protection. Sign out using the authenticated **Logout** action.

Administrators can create additional administrator or officer accounts from **Users > Create User** and view data-backed operational reports at **Reports**. Officer accounts are not publicly self-registered. Administrators, officers, and customers have role-specific dashboards and permissions; the customer dashboard only loads the signed-in customer's own application, claims, and payments. Insurance Officers can create and edit member records, review claims through the claim-review workflow, and view insurance plans, coverage, and payments. Only Administrators can delete members, manage system users, reports, providers, officers, plans, coverage, and payment records.

The canonical account roles are `ADMINISTRATOR`, `INSURANCE_OFFICER`, and `CUSTOMER`, with dashboard routes `/admin/dashboard`, `/officer/dashboard`, and `/customer/dashboard`. Login at `/dashboard` redirects to the route assigned to the authenticated user's role. Public registration always creates a `CUSTOMER`; privileged account creation and role changes are available only through administrator-controlled user management. Customer claim and payment summaries are queried by the national ID of the member linked to that authenticated account.

To create the first Administrator, configure all of these environment variables before starting the app:

```powershell
$env:APP_BOOTSTRAP_ADMIN_USERNAME = "<administrator username>"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD = "<strong password of at least 8 characters>"
$env:APP_BOOTSTRAP_ADMIN_EMAIL = "<administrator email>"
```

The app hashes account passwords with BCrypt and does not include a built-in demo login. Login uses Spring Security with the database-backed users; public registration always grants the Customer role. Administrator bootstrap settings are optional; when supplied, they create an account only if the configured username does not already exist. Authentication pages are `/login`, `/register`, and `/access-denied`.

Hibernate manages these eight domain tables from the JPA entities:

- `members`
- `insurance_plans`
- `coverages`
- `healthcare_providers`
- `insurance_officers`
- `claims`
- `payments`
- `users`

The `users` table has an optional unique link to `members` for customer portal accounts; no additional application table is required.

For a database created by an earlier development version, remove the old seeded demo account explicitly in pgAdmin only if it should no longer be used:

```sql
DELETE FROM users
WHERE username = 'admin' AND email = 'admin@isibo.local';
```
## REST API

All API endpoints require authentication. Administrator-only writes are enforced by Spring Security.

| Resource | Endpoint |
|---|---|
| Users | `/api/users` |
| Members | `/api/members` |
| Insurance plans | `/api/insurance-plans` |
| Coverage | `/api/coverage` |
| Healthcare providers | `/api/providers` |
| Claims | `/api/claims` |
| Payments | `/api/payments` |
| Insurance officers | `/api/officers` |

Collection endpoints support `GET` and `POST`; item endpoints support `GET /{id}`, `PUT /{id}`, and `DELETE /{id}` where permitted. Claims also support `POST /{id}/decision?status=APPROVED&officerId={uuid}` for Insurance Officer claim review.
