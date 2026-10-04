# Codex Context — Backend

Internal implementation handoff for Codex. This file describes this repository specifically. Read it before editing, inspect git status and relevant source, and preserve unrelated user changes. Current code wins if this note becomes stale. Do not commit unless asked.

## Service ownership

Spring Boot REST API, normally port 8080 with context path /api/v1, backed by PostgreSQL. This is the source of truth for SpendWise users and user IDs, roles/scopes, browser sessions, financial records, Telegram invitations/claims/accounts, audit events, and business ownership.

Other repositories are spendwise-frontend (React browser client), spendwise-mcp (agent-facing business tools), and task-automation-bot (Telegram webhook and orchestration). Backend does not parse Telegram updates or call Telegram Bot API. Telegram authorization and enrollment are backend REST/security concerns, never MCP tools.

## Source map

Current Java package root is com.spendwise.

- controller: HTTP adapters for auth, user profile, expenses, budgets, categories, analytics, Telegram admin/internal, Telegram memory, etc.
- service: application/domain behavior and transaction boundaries. TelegramAuthorizationService handles invite/claim/approval/account association; TelegramCredentialSetupService issues one-time setup links; JwtAuthService issues/rotates browser sessions.
- entity and repository: JPA model/repositories. Important entities: UserProfile, TelegramAccount, TelegramInvite, RefreshToken, TelegramCredentialSetupToken, plus business entities.
- model: enums such as ApplicationRole, TelegramAccountStatus, TelegramInviteStatus.
- security: SecurityConfig, ServiceTokenVerifier, BrowserOriginVerifier; security/filter has JWT, API-key and Telegram service authentication; security/oauth handles Google success; security/principal defines authenticated identity.
- config/properties: typed JWT/auth/Telegram configuration. web/filter/RequestLoggingFilter adds request IDs and request logs.

Trace controller → service → repository/entity. Keep authorization, user ownership and validation on backend.

## Authentication and authorization

There are separate credential paths; do not interchange them.

1. Password registration/login and Google OAuth create browser sessions. Password login returns short-lived RS256 access JWT and sets opaque SPENDWISE_REFRESH HttpOnly cookie. Google success sets cookie and redirects to frontend, which then refreshes for an access JWT.
2. JWT subject is the SpendWise user UUID; claims include role, scopes, issuer/audience, and timestamps. Refresh tokens are random but only hashes are stored. Refresh rotates token; reuse of a revoked token revokes its active family. Refresh/logout verify configured frontend Origin.
3. Personal API keys use X-API-Key.
4. MCP business calls use Authorization Bearer SPENDWISE_AUTOMATION_SERVICE_TOKEN plus X-Telegram-User-Id. TelegramServiceAuthenticationFilter resolves an ACTIVE Telegram mapping and sets the mapped SpendWise user UUID as principal. Never accept an LLM/user-body supplied identity.
5. Bot internal auth/claim/setup/memory routes validate Authorization Bearer SPENDWISE_TELEGRAM_SERVICE_TOKEN through ServiceTokenVerifier. This differs from MCP automation token, bot-to-MCP token, Telegram bot token, webhook secret, and personal API keys.

New public password registrations get USER. Admin assignment is trusted backend provisioning only. JwtAuthService currently maps USER to profile/expense/budget/category/analytics/API-key scopes and adds Telegram invite/claim/user scopes for ADMIN. TelegramAdminController also checks method scopes with PreAuthorize; frontend visibility is not security.

## Telegram route behavior

TelegramAdminController is under /admin/telegram and supports create invite, list pending claims, approve/reject, revoke invite, and set Telegram account status. The browser JWT must carry matching scopes.

TelegramAuthorizationController is under /internal/telegram and validates bot service token. Current routes include GET /users/{telegramUserId} for status/user lookup, POST /claim for invitation claim, and POST /users/{telegramUserId}/credential-setup for a one-time setup URL. Telegram memory routes are separate internal controller calls.

Invite token is returned only in the generated deep link; backend should store token hash. Claim binds invitation to claimant and stays pending until an administrator verifies identity out of band and approves. Approval creates SpendWise profile and Telegram mapping. Credential setup adds web credentials to that same profile. Inspect TelegramAuthorizationService for idempotency, transaction and locking behavior before changing it.

## JWT migration and known auth fixes

JWT uses SPENDWISE_JWT_PRIVATE_KEY and SPENDWISE_JWT_PUBLIC_KEY, RSA PKCS#8 private and X.509 public PEM; literal escaped newlines are normalized. Defaults: access 10 minutes, refresh 30 days; Secure/SameSite cookie settings are configurable.

Past frontend issue: refresh fetch followed a 302 into Google OAuth, causing a browser CORS error. Refresh is an API flow; anonymous refresh should yield unauthenticated response, never OAuth navigation. If it recurs, inspect AuthSessionController and frontend fetch behavior.

Past backend issue: a controller generated 403 but servlet ERROR dispatch was reprocessed by Spring Security and response became 401. SecurityConfig permits DispatcherType.ERROR. User confirmed the fix; preserve it. Check response X-Request-Id against server logs.

SecurityConfig currently groups some business route scopes. Standard USER tokens carry the full expected scope set. If introducing reduced/custom scopes, ensure a read on one business resource cannot pass because of a scope for another resource.

## Persistence and runtime

application.properties currently uses spring.jpa.hibernate.ddl-auto=update. At review, no Flyway/Liquibase migration system was present; re-check pom/resources before schema work. Use any migration framework if added later; never edit production schema manually.

JDK 21/Maven/PostgreSQL. Typical local start is mvn spring-boot:run. Compose starts backend and database. Important configuration families: DB, Google OAuth, frontend origin/base/success redirect, service tokens, Telegram bot name/invite expiry, JWT PEM/issuer/audience/TTLs/cookie flags, logging. Exact environment bindings are in application.properties and the operational setup was previously in README (README is now deliberately purpose/role-only).

## Debugging history

- /auth/automation/api-key-exchange was assumed to exist but was not a backend controller; Spring redirected to Google OAuth (302). Inspect controller mappings/context path before auth debugging.
- Internal endpoint may correctly return 403 for inactive Telegram user. If Postman/Python reports 401, check Spring ERROR dispatch/entry point and any client exception translation.
- GET /internal/telegram/users/{id} returning 404 means there is no corresponding active/known mapping depending service semantics; it is not equivalent to missing URL.
- Do not log bearer values, raw invite tokens, financial request bodies, or private PEMs. Request logs should preserve method/path/status/duration/requestId.

## Cross-repo current date incident

Actual date was Oct 4, 2026 in Asia/Kolkata. Telegram previously answered “What is the date today?” with Oct 4, 2023; Telegram expense had old spentAt though createdAt was current. Backend stores received LocalDate. MCP expense tool requires spent_at and sends spentAt. Bot now has date context built from APP_TIMEZONE and inserted into every agent invocation. Verify deployed process/image and actual request payload before changing backend; backend should not rewrite explicit past/future user dates.

## Existing changes

Before documentation corrections, backend had user changes in README, docker-compose, pom, PasswordAuthController, TelegramAuthorizationController, SecurityConfig, and untracked OpenApiConfig. README was intentionally rewritten per request. Preserve non-documentation changes.
