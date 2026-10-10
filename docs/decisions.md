# Decisions

Approved architectural and business-rule decisions. New entries require explicit user approval.

## D1 — Modular monolith for V1

Single Spring Boot application with module packages (quotation, customer,
extraction, pdf, settings, backup). No microservices.

## D2 — Local-first storage with a stateless backend

Android SQLite and app-private files own permanent records (customers,
quotations, items, settings, numbering, images, PDFs). The backend keeps
temporary uploads only and must not be treated as the record owner.

## D3 — Integer rupees with rounding only on the grand total

All money uses `BigDecimal`. Line amounts, subtotal, and tax amounts are
exact (division by 100 is exact, so taxes keep paise precision without
rounding). Only the final grand total is rounded to whole rupees with
`HALF_UP`. The amount in words derives from the rounded grand total and
never includes paise.

Example: subtotal 111 with CGST/SGST 9% gives taxes of 9.99 each and a
grand total of 131 ("One Hundred Thirty One Only").

## D4 — Deletion protection is days-based (duration undecided)

Recently created quotations are protected from deletion; the exact day
count is a single named policy to be decided before Phase 10. No scattered
date checks.

## D5 — Toolchain baseline

Java 25, Spring Boot 3.5.5, Maven. Backend persistence, if ever assigned to
the backend, uses SQLite; until then the backend stays stateless.

## D6 — V1 supporting-document allowlist

PDF, images (jpeg/png/webp), DOC, DOCX. One optional document per
quotation. XLS/XLSX and all other types are rejected.

## D7 — Single user for V1

No login or multi-user scope. No backend authentication in V1.

## D8 — PDF reference

`pdf/VIJAYA LAXMI GEARS4.docx` is the sole layout reference for the fixed
VLG template (Phase 8). Missing assets must be reported, never fabricated.

## D9 — Quotation numbering rules

Format is `PREFIX-YEAR/SEQ` (for example `VLG-2025/008`); prefix holds 1
to 12 letters or digits, the year is a four-digit value from manual
Settings, padding is 1 to 6, and sequences wider than the padding print in
full. Previews never reserve numbers. Automatic lookup skips used numbers
without jumping past a custom number. Custom numbers are trimmed, limited
to 30 characters of letters, digits, spaces, hyphens, and slashes, and must
be unused. Uniqueness is enforced by the Android store through an atomic
reserve-if-absent; matching is exact and case-sensitive. Editing a
quotation never allocates a new number. The service reads no clock, so the
year can only change through Settings.

## D10 — Gemini extraction wiring (Phase 5)

Provider `google-gemini`, model `gemini-3.8-flash` via the Generate Content
REST API from the backend only; the key arrives through `LLM_API_KEY` and
never enters the repo, logs, or responses. The provider-facing raw contract
(`value/confidence/reviewReason` with `HIGH/MEDIUM/LOW`, quantities and
prices as decimal strings) is translated into the application draft
(`value/status` with `CONFIDENT/UNCERTAIN/MISSING/CONFLICTED`), mapping
`pinCode` to `pin` and `LOW`-without-value to `MISSING`. Unparseable numerics,
workflow mismatches, and `requiresHumanReview: false` fail as provider bad
responses; model-reported `UNUSABLE` fails as an unreadable image. Images and
PDFs are forwarded to the provider; DOC/DOCX uploads are rejected with a
clear message until a controlled text-extraction component exists (open
follow-up, not silently supported).
