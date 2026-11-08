# User module integration contract

This document describes the User module contract for Student and Organization module developers.

## Creating an account during onboarding

Call `UserService.createPendingUser(...)` from the onboarding service. Do not write directly to the `users` table and do not expose this operation as a public generic account-creation endpoint.

Student onboarding:

```java
UserResponse account = userService.createPendingUser(
        new CreatePendingUserCommand(fullName, email),
        UserRole.STUDENT,
        null);
```

Employer administrator onboarding:

```java
UserResponse account = userService.createPendingUser(
        new CreatePendingUserCommand(fullName, email),
        UserRole.EMPLOYER_ADMIN,
        organizationId);
```

The organization must already exist. The database foreign key rejects an unknown organization ID. Use `account.id()` to link the related Student profile or other onboarding record. Put account creation and related profile creation in the same service transaction so either both records commit or neither does.

The operation validates name and email, normalizes the email to lowercase, rejects a case-insensitive duplicate, assigns `PENDING_ACTIVATION`, leaves the password unset, and asks Auth to send an activation email. It accepts only `STUDENT` with no organization or `EMPLOYER_ADMIN` with a positive organization ID; callers cannot create privileged administrator accounts through this method.

Activation email delivery depends on valid SMTP configuration. Coordinate with the Auth owner before changing activation behavior. The operation currently invokes Auth synchronously; a mail failure propagates to the caller and rolls back the database transaction.

## User API available to administrators

All routes require a valid JWT for a `LOAN_BOARD_ADMIN` account:

| Method | Route | Result |
| --- | --- | --- |
| `GET` | `/api/users` | List safe account details |
| `GET` | `/api/users/{id}` | Retrieve safe account details; missing ID returns 404 |
| `PATCH` | `/api/users/{id}/status` | Set eligible account to `ACTIVE` or `DISABLED` |

Pending accounts cannot be changed through the status route; Auth activation owns that transition. Responses use `UserResponse` and do not include password hashes or raw tokens.

There is no public user creation route. `UpdateUserRequest` is currently unused and does not represent an available API operation.

## Shared test expectations

Before integrating, verify that onboarding creates the User and related module record transactionally; Student accounts have no organization; employer admins reference the correct existing organization; duplicate emails are rejected ignoring case; and activation completes before the account can log in. Verify admin endpoints reject missing/invalid authentication and non-admin roles, and that API responses contain no credential or token fields.
