# Phase-by-Phase Backend Implementation Plan
## VLG Handwritten Quotation Digitization System

**Client:** React Native + TypeScript Android app  
**Backend:** Java + Spring Boot REST API  
**AI:** Provider-agnostic multimodal LLM  
**Primary storage:** Local Android database and app-private files  
**Backup:** Optional Google Drive  
**Approach:** Implement one phase at a time; run automated tests and complete the manual test before proceeding.

---

## 1. Coding standards

| Element | Convention | Example |
|---|---|---|
| Class/interface/record/enum | PascalCase | `QuotationCalculationService` |
| Method | camelCase | `calculateQuotation()` |
| Variable/parameter | descriptive camelCase | `configuredCgstRate` |
| Constant | UPPER_SNAKE_CASE | `MAX_IMAGE_SIZE_BYTES` |
| Package | lowercase | `com.vlg.quotation.calculation` |
| DB table/column | snake_case | `quotation_items`, `unit_price` |
| JSON property | camelCase | `unitPrice`, `productName` |
| Test method | descriptive | `shouldRejectNegativeQuantity()` |

Every variable name should explain what it stores and what it points/refers to. Prefer `quotationImageFile`, `extractedItemDrafts`, `calculatedSubtotal`, `llmResponseBody`, and `quotationNumberSequence`; avoid vague names such as `data`, `temp`, `obj`, `val`, or `res`.

Other rules:
- Controllers handle HTTP mapping only; business logic belongs in services.
- Use `BigDecimal` for money, never `double` or `float`.
- Use request/response DTOs rather than exposing persistence/domain objects directly.
- Keep provider-specific code behind an interface.
- Never trust LLM totals or client-calculated totals.
- Never log API keys or full customer documents.
- Version prompts, extraction schemas, and PDF templates.
- Use a modular monolith, not microservices, for V1.

---

## 2. Important storage boundary

The requirement says business data should be stored locally, while LLM secrets must remain off the Android device. A hosted Spring Boot API cannot directly write to Android app-private storage.

**Recommended local-first V1:**
- Android SQLite database: customers, quotations, quotation items, settings, numbering configuration, and history.
- Android app-private file storage: original handwritten images, supporting documents, and final PDFs.
- Spring Boot: stateless processing API for extraction, validation, calculations, and PDF rendering; temporary uploaded files only.
- LLM API key: backend environment configuration.
- Google Drive: optional backup/restore, not required for normal operation.

The mobile app uploads source files to the backend for processing and saves the returned data/PDF locally. If you later decide the backend must own all permanent quotation records, revise the storage and API design before coding.

---

## 3. Phase overview

| Phase | Scope | Exit gate |
|---|---|---|
| 0 | Project setup and API contracts | Contracts and storage boundary agreed |
| 1 | Spring Boot foundation/config/errors | Health endpoint and error JSON work |
| 2 | Extraction DTOs and schema validation | Valid/invalid JSON handled correctly |
| 3 | Calculation engine | Financial tests pass |
| 4 | Quotation numbering | Custom, automatic, and year rules pass |
| 5 | Multimodal LLM extraction | Real image produces schema-valid JSON |
| 6 | Image quality, uncertainty, conflicts | Poor/ambiguous inputs are flagged |
| 7 | Customer modes and reviewed-draft validation | New/existing flows work correctly |
| 8 | PDF generation | PDF matches the VLG reference layout |
| 9 | Quotation lifecycle and editing | Edit preserves number and snapshots |
| 10 | File lifecycle, deletion, manual fallback | Assets retained; recent deletion blocked |
| 11 | Google Drive backup/restore | Backup can be restored |
| 12 | Regression and release readiness | End-to-end checklist passes |

---

# Phase 0 — Project setup and API contracts

### What
Create the Spring Boot project, package layout, DTO contracts, error format, and API documentation before implementing business logic.

### Why
The React Native client and backend must agree on field names and meanings. This also lets phases be tested independently.

### How
Suggested stack:
- Java 21 (or a Java version supported by the chosen Spring Boot release)
- Spring Boot, Spring Web, Spring Validation
- Jackson, JUnit 5, Mockito, Spring Boot Test
- Maven
- OpenAPI/Swagger UI
- `WebClient` or `RestClient` for external provider calls

Suggested packages:

```text
com.vlg.quotation
├── api.controller
├── api.request
├── api.response
├── application
├── domain.quotation
├── domain.calculation
├── domain.numbering
├── extraction.provider
├── extraction.schema
├── extraction.prompt
├── pdf
├── configuration
├── infrastructure.files
└── common.error
```

Initial endpoints:
- `GET /api/v1/health`
- `POST /api/v1/extractions`
- `POST /api/v1/quotations/calculate`
- `POST /api/v1/quotation-numbers/preview`
- `POST /api/v1/quotations/validate`
- `POST /api/v1/quotations/{quotationId}/pdf`
- `GET /api/v1/configuration/capabilities`

Because V1 is local-first, do not add backend quotation CRUD endpoints unless the persistence decision changes.

### Practical example
Both client and server agree to use `productName`, `specifications`, `quantity`, `unit`, and `unitPrice`. No layer calls the same field `qty`, `rate`, or `description` inconsistently.

### Tests
- Spring context starts.
- OpenAPI is generated.
- DTO JSON property names match the contract.
- Invalid requests use the standard error format.

### Manual test
Start the backend, open Swagger UI, call `GET /api/v1/health`, and confirm HTTP 200.

**Complete when:** project starts and the API contracts are documented.

---

# Phase 1 — Configuration, health endpoint, and error handling

### What
Implement environment-based configuration, health checks, request IDs, and centralized exception handling.

### Why
LLM providers, models, URLs, limits, and free tiers can change. Those changes should not require rebuilding the Android app.

### How
Use typed Spring configuration properties, not repeated direct calls to environment variables.

Example `.env` values for local development:

```env
SERVER_PORT=8080
LLM_PROVIDER=gemini
LLM_API_KEY=replace-with-secret
LLM_BASE_URL=https://configured-provider-endpoint
LLM_MODEL=replace-with-current-vision-model
LLM_TIMEOUT_SECONDS=60
LLM_MAX_RETRIES=2
LLM_EXTRACTION_PROMPT_VERSION=v1
LLM_EXTRACTION_SCHEMA_VERSION=v1
PDF_TEMPLATE_VERSION=v1
MAX_IMAGE_SIZE_BYTES=10485760
MAX_SUPPORTING_DOCUMENT_SIZE_BYTES=15728640
```

These are placeholders, not guaranteed provider values. Keep a `.env.example` with placeholders; never commit real secrets.

Create `LlmProperties`, `FileUploadProperties`, `PdfProperties`, `GlobalExceptionHandler`, and `ApiErrorResponse`.

Standard error example:

```json
{
  "timestamp": "2026-10-09T10:30:00Z",
  "status": 400,
  "code": "INVALID_REQUEST",
  "message": "The uploaded image format is not supported.",
  "requestId": "req-123"
}
```

### Practical example
Changing `LLM_MODEL` in the backend environment and restarting changes the configured model without rebuilding React Native.

### Tests
- Missing required configuration is reported clearly.
- Health endpoint works without AI credentials.
- Exceptions map to stable error JSON.
- API keys never appear in logs or responses.

### Manual test
Call health; then intentionally misconfigure an AI setting and verify the failure is understandable. Inspect logs for secrets.

**Complete when:** configuration and error handling are predictable.

---

# Phase 2 — Extraction DTOs and schema validation

### What
Define the structured JSON contract returned by the multimodal LLM. Implement parsing and validation without calling a real model yet.

### Why
This isolates schema errors from provider errors and prevents malformed AI output from entering the quotation workflow.

### How
Create DTOs such as:
- `QuotationExtractionResponse`
- `ExtractedCustomerDetails`
- `ExtractedQuotationItem`
- `ExtractedField<T>`
- `ExtractionWarning`

Field status enum:
- `CONFIDENT`
- `UNCERTAIN`
- `MISSING`
- `CONFLICTED`

Example JSON:

```json
{
  "schemaVersion": "v1",
  "customer": {
    "companyName": {"value": "EXEL RUBBER PVT. LTD.", "status": "CONFIDENT"},
    "addressLine1": {"value": "SY NO 107/A, AA, B, BB 108/A", "status": "UNCERTAIN"},
    "addressLine2": {"value": null, "status": "MISSING"},
    "area": {"value": "Cheriyal Road", "status": "CONFIDENT"},
    "district": {"value": null, "status": "MISSING"},
    "state": {"value": "Telangana", "status": "CONFIDENT"},
    "pin": {"value": "502285", "status": "CONFIDENT"},
    "gstin": {"value": "36AAACE4495J1ZD", "status": "UNCERTAIN"}
  },
  "items": [
    {
      "productName": {"value": "P.B WORM GEAR", "status": "CONFIDENT"},
      "specifications": {"value": "Ø662 × 46\n126 TEETH", "status": "UNCERTAIN"},
      "quantity": {"value": 1, "status": "CONFIDENT"},
      "unit": {"value": "No", "status": "CONFIDENT"},
      "unitPrice": {"value": 210000, "status": "UNCERTAIN"}
    }
  ],
  "warnings": []
}
```

This is illustrative, not a claim that every field can be read confidently from every image. Do not include calculated amount, subtotal, GST, grand total, or amount in words as authoritative LLM fields. A model's self-reported confidence is only a hint; use deterministic validation and human review too.

### Practical example
A response with `"quantity": "one"` is rejected unless a specific safe normalization rule is intentionally implemented. Missing price is flagged rather than converted to zero.

### Tests
- Valid JSON parses.
- Missing fields become null/MISSING as designed.
- Invalid quantity type is rejected.
- Unknown status is rejected.
- Multiple items are supported.
- Empty item list is rejected.

### Manual test
Submit valid JSON, JSON with `quantity: "three"`, a missing `unitPrice`, and a two-item array through a temporary test endpoint or controller test. Inspect results.

**Complete when:** schema parsing is stable without external AI calls.

---

# Phase 3 — Deterministic calculation engine

### What
Implement item amounts, subtotal, CGST, SGST, grand total, and amount-in-words.

### Why
The LLM extracts facts; Java owns money calculations. The same input must always produce the same output.

### How
Create `QuotationCalculationService`, `QuotationCalculationRequest`, `CalculatedQuotation`, and an explicit rounding policy. Use `BigDecimal`.

Rules:
```text
lineAmount = quantity × unitPrice
subtotal = sum(lineAmount)
cgstAmount = taxableSubtotal × cgstRate / 100
sgstAmount = taxableSubtotal × sgstRate / 100
grandTotal = subtotal + cgstAmount + sgstAmount
```

V1 defaults: CGST 9%, SGST 9%, GST always applies. Rates are configuration values copied into each accepted quotation. Do not invent discounts or other charges. Freeze rounding policy before production; avoid mixing whole-rupee and decimal behavior.

Endpoint: `POST /api/v1/quotations/calculate`

Example input:

```json
{
  "cgstRate": 9,
  "sgstRate": 9,
  "items": [
    {"productName": "Duplex Sprocket EN19", "quantity": 3, "unit": "PCS", "unitPrice": 1700},
    {"productName": "Sprocket EN19", "quantity": 1, "unit": "PCS", "unitPrice": 4000}
  ]
}
```

Expected output:

```json
{
  "items": [
    {"productName": "Duplex Sprocket EN19", "amount": 5100},
    {"productName": "Sprocket EN19", "amount": 4000}
  ],
  "subtotal": 9100,
  "cgstRate": 9,
  "cgstAmount": 819,
  "sgstRate": 9,
  "sgstAmount": 819,
  "grandTotal": 10738,
  "amountInWords": "Ten Thousand Seven Hundred Thirty Eight Only"
}
```

### Tests
- Single/multiple items.
- Quantity × rate.
- Zero/negative quantity rejected.
- Negative rate rejected.
- CGST/SGST.
- Rounding edge cases.
- Large amounts.
- Amount words.
- Empty items rejected.

### Manual test
Call the endpoint with the example and verify 5100, 4000, subtotal 9100, tax 819 each, grand total 10738, and matching words.

**Complete when:** all financial calculations pass independently of AI.

---

# Phase 4 — Quotation numbering

### What
Implement number formatting, custom numbers, duplicate protection, and user-controlled business-year changes.

### Why
The year never changes automatically. Settings govern the sequence, and editing an existing quotation must preserve its number.

### How
Create `QuotationNumberService`, `QuotationNumberConfiguration`, and `QuotationNumberPreview`.

Configuration includes prefix, business year, next sequence, and sequence padding. Example format: `VLG-2025/008`.

The mobile database must enforce uniqueness because it is the local source of truth. If number allocation is moved to a backend database later, allocation must be transactional.

A custom number is accepted only if unused. It is marked used when the quotation is accepted/saved. Automatic allocation skips already-used numbers. Do not jump to one greater than a custom number unless the user-defined numbering policy explicitly says so.

Endpoint: `POST /api/v1/quotation-numbers/preview`

Input:
```json
{"prefix":"VLG","businessYear":"2025","nextSequence":8,"sequencePadding":3}
```

Output:
```json
{"formattedNumber":"VLG-2025/008","isPreviewOnly":true}
```

Preview does not reserve a number.

### Tests
- Correct format and padding.
- Year never changes automatically.
- Duplicate custom number rejected.
- Existing number unchanged on edit.
- Used numbers skipped.
- Concurrency tested in the store that owns numbering.

### Manual test
Preview `VLG-2025/008`; change Settings to year 2026 and sequence 1; verify `VLG-2026/001`. Change the device date without changing Settings; confirm no numbering transition.

**Complete when:** numbering follows Settings and uniqueness is enforced.

---

# Phase 5 — Multimodal LLM extraction

### What
Accept the handwritten quotation image and optional one supporting document, then return structured JSON.

### Why
This is the highest-risk feature. It should be implemented after the schema and validation contract are stable.

### How
Create:
- `QuotationExtractionController`
- `QuotationExtractionService`
- `LlmProvider` interface
- `LlmProviderFactory`
- provider adapter, initially for the selected vision-capable model
- `PromptTemplateService`
- `ExtractionResponseParser`
- `ExtractionSchemaValidator`

Request: `POST /api/v1/extractions` as multipart form data:
- `quotationImage` (required)
- `customerMode` (`NEW` or `EXISTING`)
- `supportingDocument` (optional, one maximum)
- `customerId` (required for existing mode if used by the client contract)

For `NEW`, extract customer details from the handwritten image and optional supporting document, plus item details from the handwritten image. For `EXISTING`, saved customer data is authoritative; focus AI on item extraction.

Processing:
1. Validate file type/size.
2. Validate mode and required parameters.
3. Run basic image-quality checks.
4. Load prompt/schema versions.
5. Call configured multimodal provider.
6. Parse and validate JSON.
7. Return draft and warnings.
8. Clean up temporary uploads.

Keep provider SDK types out of domain DTOs. Do not expose API keys.

### Tests
Mock `LlmProvider` for valid response, malformed JSON, timeout, rate limit, missing price, unsupported file, and missing existing `customerId`. Real-provider tests should be opt-in because they use quota and models can change.

### Manual test — your key checkpoint
1. Start backend with valid LLM configuration.
2. Upload your handwritten VLG quotation through Swagger/Postman.
3. Set `customerMode=NEW`.
4. Optionally upload the delivery challan as the supporting document.
5. Inspect JSON and verify customer identity, item count, product names, single specifications text block, quantity, unit, and rate.
6. Verify uncertain/missing fields are flagged and totals are not treated as LLM truth.
7. Repeat in `EXISTING` mode; confirm AI does not replace saved customer details.

**Complete when:** multiple real samples return schema-valid JSON and ambiguity is visible.

---

# Phase 6 — Image quality, uncertainty, and conflicts

### What
Add quality checks and warnings for missing, uncertain, or conflicting data.

### Why
Valid JSON can still contain incorrect values. The system must not silently guess handwriting or choose conflicting customer details.

### How
Create `ImageQualityService`, `ExtractionReviewService`, and a provider failure classifier.

Checks include:
- unsupported type/oversized image
- too low resolution, blurred, too dark
- no items detected
- missing product, quantity, or price
- non-positive quantity
- conflicting addresses across sources
- likely malformed PIN/GSTIN format (format check only; do not claim legal validity)

Example error:
```json
{
  "status": "IMAGE_QUALITY_FAILED",
  "code": "IMAGE_TOO_BLURRY",
  "message": "The image is too blurry to read. Please capture a clearer image."
}
```

Example conflict:
```json
{
  "code": "CUSTOMER_ADDRESS_CONFLICT",
  "message": "The supplied sources contain different customer addresses.",
  "fields": ["addressLine1", "area"],
  "requiresUserReview": true
}
```

### Tests
- Blurry image flagged.
- Good image not rejected by one weak heuristic.
- Conflicting sources create warnings.
- Missing price never silently becomes zero.
- Rate-limit/timeout map to stable errors.
- Saved customer record is never auto-overwritten.

### Manual test
Upload a blurred copy and a fixture with conflicting addresses. Confirm the app requests a clearer image or human review rather than silently choosing a value.

**Complete when:** uncertainty and conflicts are actionable.

---

# Phase 7 — Customer modes and reviewed-draft validation

### What
Validate user-reviewed data and enforce the new/existing customer distinction.

### Why
The app should not infer the customer mode from text in the image. The user explicitly chooses it.

### How
Use `NEW` and `EXISTING` as explicit modes. For existing customers, local saved details are used and the image may contain only the company name. For new customers, customer details can come from both input sources.

Endpoint: `POST /api/v1/quotations/validate`

Validate:
- required customer fields for the selected mode
- at least one item
- non-empty product name
- positive quantity
- valid unit price
- valid date
- terms according to the template rules
- no client-supplied totals trusted as authoritative

Example reviewed payload:
```json
{
  "quotationDate": "2026-10-09",
  "customer": {
    "companyName": "EXEL RUBBER PVT. LTD.",
    "addressLine1": "SY NO 107/A",
    "addressLine2": null,
    "area": "Cheriyal Road",
    "district": "Medak",
    "state": "Telangana",
    "pin": "502285",
    "gstin": "36AAACE4495J1ZD"
  },
  "items": [
    {
      "productName": "Worm Wheel",
      "specifications": "DRG NO: ERB-02-3-0340\nSHAW MECHANICAL PRESS 36 inch WARM WHEEL",
      "quantity": 1,
      "unit": "NOS",
      "unitPrice": 40000
    }
  ],
  "processesIncluded": null,
  "terms": {
    "purchaseOrder": "Purchase Order is required to confirm and initiate the process.",
    "delivery": "Within 10–15 working days from the date of order confirmation.",
    "payment": "50% advance with purchase order, balance after completion of work.",
    "validity": "This quotation is valid for 25 days from the date of issue."
  }
}
```

These fields are example data for testing, not assumed values for every customer.

### Tests
- New mode requires reviewed customer details.
- Existing mode can use supplied saved customer snapshot.
- Missing rate is flagged.
- Empty product name rejected.
- Customer master is not silently updated.
- Terms are preserved per quotation.

### Manual test
Submit one new-customer draft and one existing-customer draft. Verify correct validation behavior for each.

**Complete when:** only reviewed, valid data proceeds to calculation/PDF.

---

# Phase 8 — PDF generation

### What
Generate the fixed VLG PDF from validated quotation data and calculation results.

### Why
PDF layout and totals must be deterministic. The LLM must not write the final PDF.

### How
Create `QuotationPdfService`, `QuotationPdfTemplateRenderer`, `QuotationPdfModel`, and `PdfGenerationResponse`. Select a Java PDF library whose license and layout features fit the project.

Template sections:
1. Header: logo, VIJAYA LAXMI GEARS, services/work types, phone numbers, separator.
2. Quotation title.
3. Quotation number left, date right.
4. Customer name, address hierarchy, GSTIN immediately below address.
5. Table: S.No, Description, Qty, Unit Price, Amount.
6. Product name plus one multiline specifications block.
7. Optional Processes Included section.
8. Terms & Conditions.
9. Subtotal, CGST, SGST, Grand Total.
10. Amount in Words.
11. Business GSTIN, MSME, bank details.
12. Fixed authorized stamp/signature artwork.
13. Footer address and email.

Support long descriptions, multiple items, page overflow, long addresses, and wrapped terms. Logo and stamp are fixed assets in V1.

Endpoint: `POST /api/v1/quotations/{quotationId}/pdf`. In local-first V1, send the validated quotation snapshot or use a short-lived draft token; do not assume the backend has a permanent quotation record.

Example response:
```json
{
  "pdfGenerationId": "pdf-123",
  "status": "GENERATED",
  "fileName": "VLG-2025-008.pdf",
  "contentType": "application/pdf",
  "downloadUrl": "/api/v1/generated-pdfs/pdf-123",
  "templateVersion": "v1"
}
```

Client downloads the PDF and stores it in app-private storage. Use short-lived or authenticated download access.

### Tests
- One/multiple items.
- Long specification wraps.
- Totals match calculation service.
- Optional processes omitted when absent.
- Page overflow works.
- PDF is parseable and records template version.

### Manual test
Generate a PDF using the sample digital quotation data. Compare header, number/date, customer block, table, GST, total in words, bank details, stamp, and footer against the provided VLG template. Test enough items to cause page overflow.

**Complete when:** PDF looks correct and totals match exactly.

---

# Phase 9 — Quotation lifecycle, editing, and local persistence

### What
Define how the React Native app saves and edits accepted quotations, then requests recalculation and PDF regeneration.

### Why
Local-first history belongs on the device. A remote/stateless backend cannot be treated as the permanent source of quotation history.

### How
Mobile SQLite stores:
- quotation ID and immutable number
- date
- customer snapshot
- items
- tax rates/amounts, subtotal, grand total, amount in words
- terms snapshot
- source image/supporting document/PDF paths
- status/timestamps
- extraction model, prompt, and schema metadata

On edit:
1. Load local quotation.
2. Preserve its number.
3. Edit fields.
4. Validate.
5. Recalculate.
6. Regenerate PDF.
7. Update the same local record and PDF reference.
8. Preserve the original image unless the user explicitly replaces it.

Customer master changes must not rewrite historical quotation snapshots.

### Tests
- Editing does not change number.
- Quantity changes update all calculations.
- Customer master change does not change historical snapshot.
- PDF regeneration updates file reference.
- Failed save does not mark final.

### Manual test
Create/save a quotation; reopen it; change quantity from 1 to 2; regenerate. Confirm same quotation number, changed totals, and retained source image.

**Complete when:** editing preserves historical integrity.

---

# Phase 10 — File lifecycle, deletion protection, manual entry

### What
Implement temporary server-file cleanup, local asset retention, recent-quotation deletion protection, and manual-entry fallback.

### Why
Original images are part of the reference trail. AI downtime must not block quotation creation.

### How
Backend:
- Use generated internal filenames.
- Validate file type/size.
- Delete temporary uploads after processing/failure according to policy.
- Never let temporary cleanup delete the retained mobile source image.
- Expire generated download artifacts after a configured period if applicable.

Mobile:
- Retain original image and supporting document.
- Retain latest PDF.
- Delete assets only when an eligible quotation is deleted and the user confirms.
- Protect recently created quotations.

The exact period defining “recent” has not been specified. Make it one named policy and get the business decision before implementing a duration; do not scatter date checks in UI code.

Manual fallback should allow direct entry of customer/items through the same validation, calculation, PDF, and local-save path.

### Tests
- Temporary uploads deleted after success/failure.
- Retained source image unaffected.
- Recent deletion denied.
- Eligible deletion requires confirmation.
- Manual entry works without LLM credentials.

### Manual test
Create a quotation; verify image persists after PDF generation. Simulate provider failure and enter details manually. Try deleting a recent quotation and an older eligible one.

**Complete when:** cleanup is safe and AI is not a hard dependency for manual creation.

---

# Phase 11 — Optional Google Drive backup/restore

### What
Back up and restore the local database export and associated files.

### Why
The device remains primary storage, but the user can recover after device loss.

### How
Version a backup package containing customers, quotations, items, settings, numbering configuration, images, supporting documents, PDFs, and a manifest/schema version.

Use the correct Google OAuth flow. Never embed a confidential client secret in the APK. Restore should validate the package and ask for confirmation before changing local data; create a pre-restore export when practical.

### Tests
- Manifest/schema validation.
- Corrupt backup rejected.
- Missing asset reported.
- Restore preserves numbers and snapshots.
- Backup failure does not affect local data.

### Manual test
Create two quotations, back up to Drive, restore into a test profile/database, and verify customer records, quotation history, numbering, images, and PDFs.

**Complete when:** backup is optional and restore works.

---

# Phase 12 — Observability, regression, release readiness

### What
Add safe diagnostics, real-image evaluation, provider-switch tests, and an end-to-end acceptance checklist.

### Why
Free-tier models and provider behavior can change. A provider switch must be evaluated on real quotations, not merely on valid JSON output.

### How
Log request ID, extraction ID, provider, model identifier, prompt/schema versions, duration, status, and failure category. Do not log API keys, full images, or full customer documents by default.

Maintain a manually verified dataset:

```text
ai-evaluation/
├── quotation-001/
│   ├── handwritten.jpg
│   ├── supporting.pdf
│   └── expected.json
└── quotation-002/
    ├── handwritten.jpg
    └── expected.json
```

Compare customer name/address/GSTIN, item count, product name, specification preservation, quantity, unit, rate, and warnings after every model/prompt change. Numeric values and item count must be correct; harmless formatting differences can be normalized.

### Tests
Run all unit, API contract, provider adapter, calculation, PDF, and extraction regression tests. Confirm secrets do not appear in logs/responses.

### Manual end-to-end test
1. Create a new customer quotation from an image.
2. Add one supporting document.
3. Inspect extracted JSON.
4. Correct uncertain fields.
5. Accept and calculate.
6. Preview, save, and share PDF.
7. Open it in history.
8. Edit and regenerate with the same number.
9. Create a repeat quotation using customer suggestions.
10. Simulate AI failure and use manual entry.
11. Test backup/restore if enabled.

**Complete when:** the whole workflow passes and provider/model changes can be regression-tested.

---

# 4. API reference

## Health
`GET /api/v1/health`

```json
{"status":"UP","service":"vlg-quotation-api"}
```

## Extraction
`POST /api/v1/extractions` (`multipart/form-data`)

Parts: `quotationImage`, `customerMode`, optional `supportingDocument`, and `customerId` for existing mode.

## Calculate
`POST /api/v1/quotations/calculate`

Request:
```json
{
  "cgstRate": 9,
  "sgstRate": 9,
  "items": [
    {"productName":"Worm Wheel","quantity":1,"unit":"NOS","unitPrice":40000}
  ]
}
```

Response:
```json
{
  "items": [
    {"productName":"Worm Wheel","quantity":1,"unit":"NOS","unitPrice":40000,"amount":40000}
  ],
  "subtotal":40000,
  "cgstRate":9,
  "cgstAmount":3600,
  "sgstRate":9,
  "sgstAmount":3600,
  "grandTotal":47200,
  "amountInWords":"Forty Seven Thousand Two Hundred Only"
}
```

## Validate reviewed draft
`POST /api/v1/quotations/validate`

Possible statuses: `VALID`, `INVALID`, `REQUIRES_REVIEW`.

## PDF
`POST /api/v1/quotations/{quotationId}/pdf`

In local-first V1, provide a validated quotation snapshot or a short-lived draft token.

## Standard error
```json
{
  "timestamp":"2026-10-09T10:30:00Z",
  "status":503,
  "code":"AI_PROVIDER_UNAVAILABLE",
  "message":"Quotation extraction is temporarily unavailable. Please retry or enter details manually.",
  "requestId":"req-123"
}
```

---

# 5. Final phase checklist

Before closing any phase:
- [ ] Naming conventions followed.
- [ ] Variables are descriptive.
- [ ] Controllers are thin.
- [ ] Money uses `BigDecimal`.
- [ ] Request and response DTOs are separate from domain models.
- [ ] No secrets are committed, logged, or returned.
- [ ] Error JSON is stable.
- [ ] Unit and integration tests pass.
- [ ] Manual test is completed.
- [ ] Prompt/schema/template versions are recorded where relevant.
- [ ] No LLM totals are trusted.
- [ ] Historical quotation number, customer snapshot, and terms snapshot remain stable.

## Start here

Implement **Phase 0 and Phase 1 only**. First prove that Spring Boot starts, reads configuration externally, exposes health, and returns consistent errors. Then implement the extraction DTO/schema and calculation engine independently. Connect the real LLM only after those contracts are stable.

This keeps debugging focused: if a value is wrong, you can determine whether the cause is the model, schema, validation, calculation, numbering, or PDF renderer.
