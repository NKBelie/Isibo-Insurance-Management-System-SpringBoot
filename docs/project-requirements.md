# ISIBO Project Requirements

## Problem, objectives, and scope

Insurance systems must keep member enrollment, plan and coverage definitions, provider information, claim decisions, payments, and account access consistent. ISIBO provides a browser-based application to record and manage these related workflows. This project statement describes the software problem; it does not assert that a particular organization currently has these difficulties.

The objectives are to provide authenticated role-based access, maintain related insurance records through the existing Spring Boot application, support administrator-controlled system-user roles, and give customers access to information associated with their linked member account.

The implemented scope is the eight existing record types, local account authentication, customer insurance application submission, role-specific dashboards, administrative reports, server-side validation, and the existing REST and Thymeleaf interfaces. Password reset, customer profile editing, document upload, external notification delivery, OAuth2, MongoDB, RabbitMQ, and production deployment are not represented as implemented in the current code.

## Target users

| User | Need |
|---|---|
| Administrator | Manage system-user access and authorized insurance records; review system-wide operational information. |
| Insurance Officer | Access permitted member, plan, coverage, claim-review, and payment workflows without authority to manage system roles. |
| Customer | Submit an insurance application and view the application, plan, plan coverage, claims, and payments associated with their own linked member account. |

## Functional requirements and acceptance criteria

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-01 | Authenticate users with local accounts. | Valid credentials establish a session; invalid credentials do not authenticate; logout invalidates the session. |
| FR-02 | Store passwords safely. | New and changed passwords are BCrypt-encoded; API responses do not serialize password hashes. |
| FR-03 | Enforce three primary roles. | Stored roles and granted authorities use `ADMINISTRATOR`, `INSURANCE_OFFICER`, and `CUSTOMER` / matching `ROLE_*` authorities. |
| FR-04 | Restrict role assignment to administrators. | Public registration creates `CUSTOMER` regardless of submitted role data; only an administrator may create or change system roles. |
| FR-05 | Route dashboards by role. | `/dashboard` sends administrators, officers, and customers to their corresponding role-specific dashboard. Direct access to another role's dashboard is denied. |
| FR-06 | Protect customer-owned records. | Resolve the authenticated username to its linked member; query claims/payments using that member's national ID; never use a request-supplied customer/member ID as proof of ownership. |
| FR-07 | Manage domain records. | Existing CRUD operations validate inputs and relationships and preserve service-level business rules for all eight entities. |
| FR-08 | Show role-appropriate insurance information. | Staff access follows route policy; a customer dashboard shows only that customer's linked membership and its plan/coverage, claims, and payments. |
| FR-09 | Record a customer insurance application. | An authenticated customer without an existing member link may submit an application; the server links the created member to that user and sets its initial status to `PENDING`. |
| FR-10 | Provide operational reports. | Only an administrator may access system-wide reports. |
| FR-11 | Generate record identifiers and validate National IDs. | Business IDs are assigned by the server in three-letter, sequential formats such as `PLN-001`; member National IDs contain exactly 16 digits. |

Notifications, document handling, password reset, OAuth2 account creation, and customer profile editing require implementation before they can be treated as functional requirements of the delivered system.

## Role-specific user stories

**Administrator:** As an Administrator, I want to create a user and select one of the three roles so that access is granted by a server-authorized administrator.

- Given an authenticated administrator, when they create a valid user with a supported role, then the account stores that canonical role and a BCrypt password hash.
- Given a non-administrator, when they access user management or submit an account/role-change request, then the request is rejected.
- Given a public registration request containing a privileged role value, when registration succeeds, then the stored role is `CUSTOMER`.

**Insurance Officer:** As an Insurance Officer, I want to review permitted claims and access relevant insurance records without being able to change system roles.

- Given an authenticated officer, when they open the officer dashboard and authorized operational pages, then those pages are available.
- Given an officer, when they request an administrator dashboard, user-management page, or administrator write operation, then the request is rejected.

**Customer:** As a Customer, I want to see my insurance application and related insurance activity without seeing another member's records.

- Given a customer account linked to a member, when the customer opens their dashboard, then the page shows information queried for that linked member and the member's selected plan.
- Given a customer, when they request an administrator/officer route or staff-wide member, claim, or payment list, then the request is rejected.
- Given a customer account not yet linked to a member, when the customer opens the dashboard, then they can start an application and no other member's information is shown.

## Non-functional acceptance targets

These are proposed verification targets, not measured claims about the current application.

| Area | Target | Verification condition |
|---|---|---|
| Usability/responsive layout | Core login, registration, dashboard, and entity list workflows remain usable at 360px, 768px, and 1280px viewport widths without horizontal page overflow. | Browser checks at the stated widths with representative data. |
| Accessibility | Every form input has an associated label; interactive controls are keyboard reachable; visible focus is retained; status is not conveyed by color alone. | Keyboard and automated accessibility review on core workflows. |
| Security/privacy | Zero successful unauthorized role-route or cross-customer access cases in the automated security suite. | Integration tests using all three roles and altered identifiers/request bodies. |
| Reliability | Invalid data produces a clear client error and does not persist partial records. | Service/controller tests and transaction-failure tests. |
| Performance | At 1,000 records per list and a local PostgreSQL database, paginated first-page list requests target p95 below 1 second after warm-up. | Repeatable benchmark with disclosed hardware, dataset, and warm-up; this target is not yet measured. |
| Maintainability | Business rules remain in services; controller authorization is enforced server-side; tests cover each newly added rule. | Code review and focused automated tests. |
| Data integrity | Database constraints prevent duplicate usernames/emails and duplicate member/account association; foreign keys prevent orphaned related records. | PostgreSQL schema and integration tests. |

## Known limitations

The current application uses PostgreSQL for its implemented transactional data. MongoDB, RabbitMQ, OAuth2, configured email/SMS delivery, document storage, a migration framework, automated deployment, and a Postman collection are not currently verified features. See the [compliance matrix](./requirements-compliance.md) for status and planned verification evidence.
