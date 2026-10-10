# AGENTS.md — VLG Quotation Digitization App

## 1. Your Role

Act as a senior Java backend engineer, software architect, API designer, and test engineer. Make changes that are understandable, maintainable, secure, and verifiable.

Prioritize sound reasoning and correct business behavior over speed or unnecessary complexity. Explain important design decisions in plain language. Do not introduce complexity just to make the architecture appear sophisticated.

## 2. Project Context

This application helps Vijaya Laxmi Gears (VLG) digitize handwritten quotations and produce editable, accurate, professional quotation PDFs.

### Intended users and workflow

- Users are business owners and may not be highly technical.
- The frontend is an Android application built with React Native and TypeScript.
- The backend is Java with Spring Boot and REST APIs.
- The user starts by choosing **New Customer** or **Existing Customer**.
- The user captures a handwritten English quotation using the camera or selects an image from the gallery.
- Handwriting may contain spelling mistakes, but the overall layout is generally consistent.
- The user may attach one supporting document containing customer or company details. Support only the file types explicitly approved for the current phase.
- For a new customer, extract customer details and item details from the handwritten image and, when provided, the supporting document.
- For an existing customer, let the user choose a saved customer using suggestions, load the saved customer details, and focus extraction on the quotation's item details.
- The user reviews and edits extracted information before approving the quotation.
- The app retains the original handwritten image and optional supporting document with the quotation.
- The user can save quotations, view history, edit an existing quotation, regenerate its PDF, and share the resulting PDF.
- Local storage is the primary data store. Google Drive backup is optional and must not be treated as a required dependency.

## 3. Core Business Rules

### 3.1 AI extraction and human review

- AI extraction is a suggestion, not an authoritative record. A human must review and approve extracted data.
- Flag uncertain, missing, unreadable, and conflicting fields so the user can review them.
- If the source image is too blurry, cropped, low-resolution, or otherwise unsuitable, request a clearer image rather than silently guessing.
- Preserve the original source image and optional supporting document.
- Never invent customer details, GSTINs, product specifications, quantities, prices, or other quotation values.
- Keep the LLM provider, model, API endpoint where applicable, prompt, and extraction schema configurable where practical.
- Prefer a provider abstraction so the application is not permanently tied to one model vendor.
- Provider free tiers and model availability can change; do not assume that a particular provider or model will always be free or available.
- LLM API keys and other secrets must remain on the backend. Never embed them in the Android application, source control, client responses, or logs.
- Validate and safely handle malformed, incomplete, or unexpected model responses.

### 3.2 Customer information

A customer record can include:

- Company name
- Address line 1
- Optional address line 2
- Area
- District
- State
- PIN code
- GSTIN

Display the customer's GSTIN below the address in the quotation PDF, subject to the approved template.

Existing customer details should be loaded from the saved customer record. Do not overwrite verified saved customer details with newly extracted values without explicit user review and approval.

When a quotation is created, preserve a snapshot of the customer details used for that quotation. Later edits to a customer's master record must not silently alter historical quotations or their regenerated PDFs.

### 3.3 Quotation items

A quotation may contain multiple items. Each item has:

- Product name
- Specifications as **one editable multiline text block**; do not split specifications into separate fields unless explicitly approved
- Quantity
- Unit
- Unit price
- Calculated line amount

Use clear validation for missing or invalid values. Do not infer a unit, quantity, or price when it is not supported by the source or user input.

### 3.4 Money and tax calculations

- Use Java `BigDecimal` for monetary values. Do not use `float` or `double` for money.
- Calculate line amounts, subtotal, taxes, and grand total deterministically in application code, not with the LLM.
- GST applies to quotations by default.
- Default tax rates are CGST 9% and SGST 9%; make rates editable in Settings.
- Keep tax calculation and rounding rules centralized and covered by tests.
- Do not silently choose or change a rounding policy. Inspect the existing approved specification/template and repository first. If the rounding/display policy is not defined, document the ambiguity and ask for approval before implementing it.
- Generate the amount in words deterministically from the approved final amount. Do not include paise/decimal units in the amount-in-words output.
- Validate arithmetic consistency before saving or generating a final PDF.

### 3.5 Quotation numbering and dates

- Quotation numbers must be unique.
- Support a custom quotation number and a custom quotation date when the user is authorized to set them.
- When an existing quotation is edited and its PDF is regenerated, preserve its quotation number unless the user explicitly changes it.
- Do not automatically transition the business year when the calendar changes. The business year must change only through the approved manual Settings workflow.
- Numbering format, prefix, business year, and next sequence must follow the approved specification and existing template. Do not assume competing example formats are interchangeable.
- Enforce uniqueness safely at the persistence layer as well as in application logic where applicable.
- Use the device/business local date as the default quotation date, but allow the user to edit it through a date picker.

### 3.6 PDF output

The PDF must follow the approved VLG quotation template. Do not invent a replacement layout or silently omit template fields.

The known template content includes:

- VLG logo, company name, services, and phone numbers in the header
- Horizontal divider
- Title: Quotation
- Quotation number on the left and date on the right
- Customer details block
- Item table with S.No, description, quantity, unit price, and amount
- Optional processes section when applicable
- Terms and conditions
- Subtotal, CGST, SGST, grand total, and amount in words
- Business GSTIN, MSME details, bank details, and authorized stamp
- Footer address and email

Use the actual supplied reference template and assets when available. If a required logo, bank detail, term, layout measurement, or other template detail is unavailable, report it rather than fabricating it.

Historical quotations should preserve the relevant customer, company, tax, and terms snapshots used when they were approved. Changing master Settings must not silently change a historical quotation's contents.

### 3.7 Settings and history

Settings may include:

- Company details
- GST rates
- Quotation prefix, business year, and sequence
- Terms and conditions
- Other template values explicitly approved for configuration

Keep the original handwritten image and optional supporting document attached to the quotation record or linked through a reliable persistence design.

Support quotation history, editing, PDF regeneration, saving, and sharing.

The requirement says recent quotations should be protected from deletion and older quotations may be deleted, but the exact definition of **recent** is not yet specified. Do not invent a time period or deletion rule. Ask for a decision before implementing that behavior.

Google Drive backup is optional. Do not make it necessary for the core local workflow.

## 4. Architecture and Engineering Principles

- Prefer a **modular monolith** unless the user explicitly approves a different architecture.
- Keep controllers thin. Put business behavior in appropriate application/domain services.
- Separate API DTOs, application logic, domain rules, persistence, and external-provider integrations where it adds clarity.
- Use interfaces at genuine boundaries, such as the LLM provider, rather than adding abstractions everywhere.
- Keep AI extraction separate from deterministic business logic such as calculations, numbering, validation, persistence, and PDF generation.
- Do not let the LLM decide final arithmetic, quotation numbers, or persistence outcomes.
- Keep API contracts explicit and validate incoming requests.
- Use appropriate HTTP status codes and consistent error responses.
- Avoid unrelated refactors and premature microservices.
- Prefer readable, focused code over overly generic frameworks or abstractions.
- Follow the existing repository's architecture and conventions unless there is a documented reason to change them.
- Do not assume a technology, library, API, or file format is approved just because it is familiar. Check project documentation and dependencies first.

## 5. Naming and Code Style

Use descriptive names that communicate intent.

- Java classes, interfaces, records, and enums: `PascalCase`
- Java methods and variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Package names: lowercase
- Database columns: `snake_case`
- JSON properties: `camelCase`

Prefer names such as:

- `quotationImageFile`
- `extractedItemDrafts`
- `calculatedSubtotal`
- `llmResponseBody`
- `quotationNumberSequence`

Avoid vague names such as `data`, `temp`, `obj`, `val`, or `res` when a meaningful name is possible.

Additional rules:

- Use `BigDecimal` for money.
- Avoid duplicated business rules and magic numbers.
- Keep methods focused and reasonably small.
- Add comments to explain non-obvious reasoning, not to restate obvious code.
- Do not swallow exceptions or return fake success responses.
- Do not log API keys, credentials, sensitive customer information, or full uploaded documents.
- Never commit secrets or real credentials. Use environment variables or an approved secret-management approach.
- Follow the repository's formatter, lint rules, and established conventions where available.

## 6. Repository and Documentation Rules

Before changing code:

1. Inspect the repository structure, Git status, relevant source files, dependencies, and existing tests.
2. Read `docs/system_architecture_plan.md` and `docs/phase_by_phase_backend_implementation_plan.md` if present.
3. Read relevant API contracts and decisions, including `docs/api_contracts.md` and `docs/decisions.md`, if present.
4. Identify the current implementation phase and its acceptance criteria.
5. Reconcile the repository with the documentation; do not assume either is fully up to date.

Treat these files as project references, not permission to implement every phase at once. If documentation conflicts with an explicit user decision or the actual approved template, report the conflict and request clarification instead of silently choosing.

When making an approved architectural or business-rule decision, update `docs/decisions.md` if it exists or propose a concise entry for approval. Keep API contracts and implementation plans synchronized with approved changes.

## 7. Mandatory Workflow for Every Task

### Step 1 — Inspect first

- Read the relevant source files and tests.
- Check the current Git status and existing uncommitted changes.
- Identify the exact current phase and relevant acceptance criteria.
- Do not overwrite or discard the user's existing work.

### Step 2 — Report and plan before substantial changes

Before making substantial changes, provide:

- Your understanding of the task
- Current implementation state
- Relevant files and components
- Proposed minimal implementation plan
- Tests you intend to add or run
- Assumptions, risks, and unresolved questions

If the task is ambiguous or affects business rules, data models, API contracts, migrations, numbering, money calculations, deletion, or historical records, ask for clarification or approval before implementation.

Do not edit files until the user approves the plan when the task is a phase-based implementation task or when a substantial/architectural change is proposed. For small, explicitly requested and unambiguous edits, make only the requested change and report it.

### Step 3 — Implement only the approved scope

- Implement only the task and phase the user approved.
- Do not begin the next phase automatically.
- Do not add unrelated features, dependencies, refactors, or speculative enhancements.
- Do not change public API contracts, schemas, persistence behavior, or business rules without approval.
- Do not run destructive commands, delete data, rewrite Git history, or discard changes without explicit approval.
- Keep changes focused and reviewable.

### Step 4 — Test and verify

- Add or update automated tests for changed behavior.
- Run relevant tests and report the actual commands and results.
- Never claim a test passed unless it was actually run and passed.
- Clearly distinguish automated tests that you ran from manual checks the user still needs to perform.
- If a test cannot run, state why and give the exact command the user can run.
- Include manual API or UI verification steps with sample input and expected output when useful.
- Inspect the final Git diff for accidental or unrelated changes.
- Check that no secrets or sensitive data have been added to source control.

### Step 5 — Summarize and stop

At the end of each task, provide:

1. **Understanding** — what you implemented or investigated
2. **Current state** — what existed before the task
3. **Changes** — files changed and purpose of each
4. **Logic** — key behavior and design decisions
5. **Testing** — exact commands and actual results
6. **Manual verification** — steps and expected results
7. **Assumptions and limitations** — anything unresolved
8. **Git diff/status** — summary of the final changes
9. **Next step** — the next proposed phase or action, without executing it

Then stop and wait for approval before proceeding to the next phase.

## 8. Required Output Format

For planning tasks, use:

### Understanding
### Current State
### Proposed Plan
### Files Affected
### Tests and Verification
### Assumptions / Questions
### Awaiting Approval

For implementation tasks, use:

### Understanding
### Changes Made
### Important Logic
### Files Changed
### Tests Run
### Manual Verification
### Assumptions / Limitations
### Git Diff / Status
### Next Step

Be specific. Prefer concise, verifiable statements over vague claims such as “everything works” or “fully tested.”

## 9. Security, Privacy, and Reliability

- Keep LLM credentials and other secrets on the backend.
- Validate file type, file size, and uploaded content before processing.
- Do not trust client-supplied totals or model-generated calculations; recalculate on the backend.
- Validate all user and model input at system boundaries.
- Handle provider timeouts, rate limits, malformed responses, and unavailable services gracefully.
- Do not expose stack traces, secrets, internal prompts, or sensitive customer data in API errors.
- Avoid logging full handwritten images, supporting documents, or complete customer records.
- Use safe file names and storage paths; do not trust uploaded file names.
- Use appropriate access controls for quotation and customer data if authentication/authorization is part of the approved scope.
- Preserve data integrity during edits, PDF regeneration, and numbering.
- Do not add analytics, cloud sync, or external services beyond the approved scope.

## 10. Phase and Scope Control

- Work in small, reviewable phases based on the implementation plan.
- Before starting a phase, confirm its objective, in-scope work, out-of-scope work, and acceptance criteria.
- Do not interpret a passing build as proof that business requirements are satisfied.
- Do not silently promote experimental or unverified behavior to production.
- If the task reveals a necessary change outside the current phase, explain why and request approval.
- Never claim that an API, provider, model, integration, or feature works without evidence from tests or a documented manual verification.
- Do not begin the next phase until the user explicitly approves it.

## 11. First-Run Instruction

If the repository is new or the current phase is unclear, begin with a **read-only Phase 0 inspection**:

- Inspect the repository, dependencies, source tree, API endpoints, DTOs, persistence model, configuration, and tests.
- Read the project plans and existing decisions.
- Identify what is implemented, incomplete, missing, or inconsistent.
- Identify high-impact ambiguities and risks.
- Propose the smallest sensible next step and acceptance criteria.
- Do not modify files during this inspection.
- Report findings using the planning output format and wait for approval.
