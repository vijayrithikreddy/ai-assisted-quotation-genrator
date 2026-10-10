# System Architecture Plan

## Handwritten Quotation → Digital Quotation System

**Document Version:** 1.0\
**Date:** 2026-10-08\
**Application:** Android application for Vijaya Laxmi Gears (VLG)\
**Primary users:** Business owners / non-technical users\
**Architecture style:** React Native Android client + Spring Boot
backend + local-first persistence + multimodal AI extraction

------------------------------------------------------------------------

# 1. Executive Summary

The system converts a physical handwritten quotation into a professional
digital quotation PDF.

The user captures or selects a handwritten quotation image. The system
uses a multimodal vision-capable LLM to understand the image and extract
only the business information that needs interpretation:

-   Customer/company details when the customer is new
-   Quotation item/product details
-   Product specifications
-   Quantity
-   Unit
-   Unit price/rate

The system does **not** rely on the LLM for business calculations,
quotation numbering, dates, GST configuration, company configuration, or
PDF generation.

The core principle is:

> **AI proposes → Human verifies/edits → Application validates →
> Application calculates → Application generates PDF.**

The original handwritten image and optional supporting document are
retained with the quotation.

The application is designed for a business owner who should be able to
complete the workflow with minimal technical knowledge.

------------------------------------------------------------------------

# 2. Problem Definition

The current business process starts with a handwritten quotation and
requires creation of a clean digital quotation.

Manual conversion creates several problems:

-   Re-entering customer information is time-consuming.
-   Handwritten product descriptions and specifications can be difficult
    to interpret.
-   Re-entering quantities and rates introduces arithmetic or typing
    errors.
-   Repeated customer information has to be entered repeatedly.
-   Generating a consistent professional quotation manually takes time.
-   The final quotation should follow a fixed company format.

The proposed system automates the data-entry portion while keeping the
business owner in control of the final quotation.

------------------------------------------------------------------------

# 3. Product Goal

The system should reduce the process to:

``` text
New Quotation
    ↓
New Customer / Existing Customer
    ↓
Capture/select handwritten quotation
    ↓
Optional supporting document
    ↓
AI extraction
    ↓
Review and edit
    ↓
Application calculation
    ↓
Generate PDF
    ↓
Preview
    ↓
Save / Share
```

------------------------------------------------------------------------

# 4. Scope

## 4.1 V1 In Scope

-   Android application
-   React Native frontend
-   Java Spring Boot backend
-   Camera image capture
-   Gallery image selection
-   One optional supporting document
-   New/existing customer selection
-   Local customer/company memory
-   Multimodal LLM extraction
-   Customer detail extraction for new customers
-   Item extraction
-   Multiple quotation items
-   Editable specifications text block
-   Confidence/uncertainty flags
-   Human review
-   Local quotation persistence
-   Original handwritten image retention
-   Supporting-document retention
-   Fixed quotation PDF template
-   PDF preview
-   PDF save
-   Android native PDF sharing
-   Quotation history
-   Existing quotation editing
-   Same quotation number when an existing quotation is edited
-   Configurable quotation numbering
-   Configurable GST
-   Configurable business information
-   Configurable default terms
-   Optional Google Drive backup
-   Provider/model/prompt configuration without rebuilding the
    application

## 4.2 V1 Out of Scope

-   Automatic year transition of quotation numbering
-   Multi-user SaaS
-   Cloud quotation database
-   Online customer portal
-   Automatic customer-data synchronization between devices
-   Automatic sending through WhatsApp APIs
-   Automatic email sending
-   Multiple supporting documents per quotation
-   Arbitrary unsupported file processing
-   User replacement of logo/stamp/signature
-   Automatic alteration of saved customer records
-   Automatic acceptance of AI extraction

------------------------------------------------------------------------

# 5. Core Functional Requirements

## FR-01: Create quotation

The user can start a new quotation.

## FR-02: Customer type selection

The first major decision is:

``` text
New Customer
Existing Customer
```

### New Customer

The system extracts customer details from the supplied source material.

### Existing Customer

The user selects a previously saved customer. Customer details are
loaded from local storage and the AI primarily extracts quotation items.

The handwritten quotation may contain only the customer name in this
case.

------------------------------------------------------------------------

# 6. Input Sources

## 6.1 Handwritten quotation

Supported input:

-   Camera capture
-   Gallery image

The handwritten quotation is the primary source for item extraction.

The handwriting is expected to be English and generally follows a
consistent quotation layout, but spelling and handwriting may vary.

## 6.2 Supporting document

V1 permits **one optional supporting document**.

The supporting document has no fixed business template.

Examples include:

-   Image
-   PDF
-   DOC/DOCX
-   XLS/XLSX

The application should validate the supported V1 file formats and reject
unsupported types clearly.

The supporting document can provide customer/company information.

Example:

A delivery challan may contain several organizations. The AI must
identify which organization is the quotation customer rather than
extracting every company name.

------------------------------------------------------------------------

# 7. Customer Workflow

## 7.1 New Customer

``` text
New Quotation
      ↓
New Customer
      ↓
Handwritten quotation
      +
Optional supporting document
      ↓
Multimodal LLM
      ↓
Customer details + Items
      ↓
Review/Edit
```

## 7.2 Existing Customer

``` text
New Quotation
      ↓
Existing Customer
      ↓
Search/select customer
      ↓
Load customer details
      ↓
Handwritten quotation
      ↓
Multimodal LLM
      ↓
Extract items
      ↓
Review/Edit
```

The saved customer data is not silently overwritten when a new quotation
contains different customer information. The user can edit the
quotation's customer details during review.

------------------------------------------------------------------------

# 8. Customer Data Model

The customer address is structured as:

``` text
Company Name
Address Line 1
Address Line 2 (optional)
Area
District
State
PIN
GSTIN
```

GSTIN is displayed directly below the customer address in the quotation.

Suggested domain object:

``` text
Customer
├── id
├── companyName
├── addressLine1
├── addressLine2
├── area
├── district
├── state
├── pin
├── gstin
├── createdAt
└── updatedAt
```

Customer records are persisted locally.

------------------------------------------------------------------------

# 9. Customer Suggestions

When the user starts a quotation for an existing customer, the
application provides local suggestions.

Example:

``` text
Search customer:
EXEL

EXEL RUBBER PVT. LTD.
Cheriyal Road, Cheriyal
GSTIN: 36AAACE4495J1ZD
```

The user selects the customer.

Customer data is then auto-filled.

For a repeat quotation, AI does not need to rediscover the customer
details.

------------------------------------------------------------------------

# 10. Company Information vs Customer Information

The application has one fixed business owner:

**Vijaya Laxmi Gears (VLG)**.

The system must distinguish:

-   The user's own company
-   The customer being quoted to
-   Other organizations appearing in supporting documents

This is critical for documents containing multiple parties.

The AI extraction instruction must explicitly identify the intended
customer rather than extracting arbitrary company names.

------------------------------------------------------------------------

# 11. Quotation Item Structure

A quotation may contain multiple items.

Each item contains:

``` text
QuotationItem
├── sequenceNumber
├── productName
├── specifications
├── quantity
├── unit
├── unitPrice
└── amount
```

## Specifications

Specifications remain one editable text block.

Example:

``` text
P.B WORM GEAR
Ø662 × 46
126 TEETH
```

The system must not unnecessarily split this into separate fields such
as diameter, pitch, teeth, etc.

This reduces extraction complexity and preserves the business wording.

------------------------------------------------------------------------

# 12. AI Extraction Boundary

The multimodal LLM is responsible for interpreting visual information.

### LLM extracts

For new customers:

-   Customer company name
-   Customer address
-   Customer GSTIN, if present
-   Item/product names
-   Item specifications
-   Quantity
-   Unit
-   Unit price/rate
-   Optional process information if present
-   Relevant custom quotation information if explicitly provided

For existing customers:

-   Item/product names
-   Item specifications
-   Quantity
-   Unit
-   Unit price/rate
-   Optional process information

### Application owns

-   Quotation number
-   Quotation date default
-   GST configuration
-   CGST
-   SGST
-   Subtotal
-   Tax calculation
-   Grand total
-   Amount in words
-   User's business details
-   Bank details
-   Footer
-   Fixed terms defaults
-   PDF rendering
-   Persistence

------------------------------------------------------------------------

# 13. Multimodal LLM Architecture

A separate OCR stage is not mandatory for V1.

The preferred pipeline is:

``` text
Image
  ↓
Image Quality Validation
  ↓
Multimodal LLM
  ↓
Structured JSON
  ↓
Schema Validation
  ↓
Business Validation
  ↓
Human Review
```

The reason is that the LLM can reason about both handwriting and
document layout.

For example, it can interpret:

``` text
Qty: 1
Rate: 210000
Amount: 210000
```

rather than depending on a separate OCR stage to first recognize every
character.

OCR can be introduced later as a supporting/fallback signal if testing
demonstrates that it improves extraction accuracy.

------------------------------------------------------------------------

# 14. Recommended Initial LLM

The architecture must remain provider-agnostic.

An initial implementation can use a current Google Gemini Flash-class
multimodal model because the Gemini Developer API supports
image/document inputs and currently provides free-tier access to
selected models. The exact model ID must be configured through
environment variables rather than hardcoded.

Google's current documentation lists multimodal Flash models and
free-tier availability, but model names, limits, and pricing can change.
Therefore the architecture must not depend permanently on a specific
model name.

Reference: https://ai.google.dev/gemini-api/docs/pricing

Gemini file input documentation:
https://ai.google.dev/gemini-api/docs/file-input-methods

------------------------------------------------------------------------

# 15. LLM Provider Abstraction

The Java backend must not directly couple quotation logic to a specific
provider.

Conceptual design:

``` text
             LLMService
                 │
       ┌─────────┼─────────┐
       │         │         │
 GeminiAdapter  OpenAIAdapter  OtherAdapter
```

The quotation system interacts with:

``` text
LLMService.extractQuotation(...)
```

not directly with:

``` text
GeminiClient
```

This makes provider replacement possible without changing the quotation
domain.

------------------------------------------------------------------------

# 16. LLM Configuration

The following must not be hardcoded:

``` text
LLM_PROVIDER
LLM_API_KEY
LLM_BASE_URL
LLM_MODEL
LLM_API_VERSION
LLM_TIMEOUT
LLM_MAX_RETRIES
LLM_MAX_OUTPUT_TOKENS
LLM_TEMPERATURE
LLM_EXTRACTION_PROMPT_VERSION
LLM_EXTRACTION_SCHEMA_VERSION
```

Example:

``` env
LLM_PROVIDER=gemini
LLM_BASE_URL=...
LLM_API_KEY=...
LLM_MODEL=...
LLM_TIMEOUT_SECONDS=60
LLM_MAX_RETRIES=2
LLM_EXTRACTION_PROMPT_VERSION=v1
LLM_EXTRACTION_SCHEMA_VERSION=v1
```

Changing provider/model/API endpoint should not require rebuilding the
React Native application.

------------------------------------------------------------------------

# 17. API Key Security

LLM API keys must never be embedded in the React Native APK.

Incorrect:

``` text
React Native
     ↓
LLM API
```

with an API key inside the application.

Correct:

``` text
React Native
     ↓
Spring Boot
     ↓
LLM Provider
```

The secret exists only in backend environment configuration.

------------------------------------------------------------------------

# 18. Prompt Management

The extraction prompt should not be embedded as a large Java string.

Use versioned prompt resources:

``` text
config/
└── prompts/
    ├── quotation-extraction-v1.txt
    └── quotation-extraction-v2.txt
```

The active version can be configured:

``` env
LLM_EXTRACTION_PROMPT_VERSION=v1
```

This allows prompt improvements without modifying business logic.

------------------------------------------------------------------------

# 19. Structured LLM Output

The LLM should return structured JSON rather than natural-language
output.

Example:

``` json
{
  "customer": {
    "companyName": "EXEL RUBBER PVT. LTD.",
    "addressLine1": "SY NO 107/A, AA, B, BB 108/A",
    "addressLine2": null,
    "area": "Cheriyal Road",
    "district": null,
    "state": "Telangana",
    "pin": "502285",
    "gstin": "36AAACE4495J1ZD"
  },
  "items": [
    {
      "productName": "Worm Wheel",
      "specifications": "DRG NO: ERB-02-3-0340\nSHAW MECHANICAL PRESS 36\" WARM WHEEL",
      "quantity": 1,
      "unit": "NOS",
      "unitPrice": 40000
    }
  ]
}
```

The exact schema must be versioned.

------------------------------------------------------------------------

# 20. LLM Extraction Rules

The prompt should explicitly instruct the model:

1.  Extract only fields in the defined schema.
2.  Do not invent missing values.
3.  Use `null` when a value cannot be reliably identified.
4.  Preserve product specifications as a single text block.
5.  Identify the quotation customer, not arbitrary companies in
    supporting documents.
6.  Distinguish the user's own company from the customer.
7.  Preserve numbers carefully.
8.  Do not calculate totals.
9.  Do not infer GST percentages as business configuration.
10. Return structured output only.
11. Identify uncertain values.
12. Never silently resolve conflicting source information when evidence
    is ambiguous.

------------------------------------------------------------------------

# 21. Image Quality Validation

Before sending an image to the LLM, the backend should perform basic
quality checks.

Potential checks:

-   File exists
-   Supported image type
-   File size within limit
-   Minimum resolution
-   Image is not severely blurred
-   Image is not excessively dark
-   Document occupies a usable portion of the frame

If quality is insufficient:

``` text
The image is unclear.

Please capture a clearer image or select another image.
```

The system should avoid wasting an LLM request on obviously unusable
images.

------------------------------------------------------------------------

# 22. Confidence and Uncertainty

AI extraction is never automatically trusted.

Each important extracted field should have an extraction status such as:

``` text
CONFIDENT
UNCERTAIN
MISSING
CONFLICTED
```

Example:

``` text
Product: P.B Worm Gear       ✓
Quantity: 1                  ✓
Rate: ₹210000                ⚠ Verify
GSTIN: 36...                 ✓
```

If the model cannot confidently determine a value, the field is flagged
for human verification.

------------------------------------------------------------------------

# 23. Human Review

Human review is mandatory.

The review screen should show:

``` text
Customer
-------------------------
Company Name       [Edit]
Address Line 1     [Edit]
Address Line 2     [Edit]
Area               [Edit]
District            [Edit]
State               [Edit]
PIN                 [Edit]
GSTIN               [Edit]

Items
-------------------------
1.
Product             [Edit]
Specifications      [Edit]
Quantity             [Edit]
Unit                 [Edit]
Unit Price           [Edit]

2.
...
```

The original handwritten image should remain accessible for comparison.

The user can correct any value.

------------------------------------------------------------------------

# 24. Review States

The quotation should have states:

``` text
DRAFT
↓
EXTRACTED
↓
UNDER_REVIEW
↓
VALIDATED
↓
PDF_GENERATED
↓
FINAL
```

An AI failure should not create a final quotation.

------------------------------------------------------------------------

# 25. Business Calculation Engine

The calculation engine is deterministic Java application logic.

The LLM does not calculate financial values.

## Line Amount

For each item:

``` text
amount = quantity × unitPrice
```

Example:

``` text
3 × ₹1,700 = ₹5,100
```

## Subtotal

``` text
subtotal = Σ lineAmount
```

Example:

``` text
5100
+4000
+9000
+10000
------
28100
```

## GST

Current configuration:

``` text
CGST = 9%
SGST = 9%
```

For taxable subtotal:

``` text
CGST = subtotal × CGST%
SGST = subtotal × SGST%
```

For ₹28,100:

``` text
CGST = ₹2,529
SGST = ₹2,529
```

## Grand Total

``` text
grandTotal = subtotal + CGST + SGST
```

Example:

``` text
28100 + 2529 + 2529
= 33158
```

## Amount in Words

The final integer grand total is converted into words.

Example:

``` text
₹33,158

Thirty Three Thousand One Hundred Fifty Eight Only
```

Paise/decimal amount-in-words is not required.

------------------------------------------------------------------------

# 26. GST Configuration

GST always applies.

Default configuration:

``` text
CGST: 9%
SGST: 9%
```

The rates are business configuration, not hardcoded business values.

The user can change them from Settings.

The handwritten `GST +18%` means the combined GST rate and corresponds
to:

``` text
9% CGST + 9% SGST = 18%
```

The application should use configured CGST/SGST rates for calculation.

------------------------------------------------------------------------

# 27. Quotation Numbering

Quotation numbers follow the configured format.

Example:

``` text
VLG-2025/008
VLG-2025/009
VLG-2025/010
```

The business year does not automatically change.

If the current configuration is:

``` text
Prefix = VLG
Business Year = 2025
Next Sequence = 008
```

the application continues using it until the user changes Settings.

Example manual transition:

``` text
VLG-2025/008
...
User changes Settings
Business Year = 2026
Next Sequence = 001
```

Then:

``` text
VLG-2026/001
```

The application must never automatically switch years because the
calendar changed.

------------------------------------------------------------------------

# 28. Custom Quotation Number

The user can enter a custom quotation number.

The system must protect uniqueness.

Example:

``` text
Current next number:
VLG-2025/008

User creates:
VLG-2025/100
```

The number is reserved as used.

The automatic sequence must not generate an already-used quotation
number.

The exact sequence advancement strategy should be implemented through a
transactional number-allocation service.

------------------------------------------------------------------------

# 29. Quotation Number Immutability

Once a quotation is created with:

``` text
VLG-2025/008
```

editing the quotation must not create:

``` text
VLG-2025/009
```

The quotation retains:

``` text
VLG-2025/008
```

even when its contents are edited and the PDF is regenerated.

------------------------------------------------------------------------

# 30. Date

Default quotation date:

``` text
Device local date
```

The user can manually change it through a date picker.

The application should store the date explicitly with the quotation.

Changing global settings later must not rewrite historical quotation
dates.

------------------------------------------------------------------------

# 31. Terms and Conditions

Default terms are maintained in Settings.

Example:

``` text
Purchase Order is required to confirm and initiate the process.

Delivery: Within 10–15 working days from the date of order confirmation.

Payment: 50% advance with purchase order, balance after completion of work.

Validity: This quotation is valid for 25 days from the date of issue.
```

Terms may change over time.

When a quotation is created, the applicable terms should be
**snapshotted into that quotation**.

Therefore changing Settings later does not change historical quotations.

------------------------------------------------------------------------

# 32. Quotation Domain Model

Conceptual model:

``` text
Quotation
├── id
├── quotationNumber
├── quotationDate
├── customerId
├── customerSnapshot
├── items[]
├── processes
├── subtotal
├── cgstRate
├── cgstAmount
├── sgstRate
├── sgstAmount
├── grandTotal
├── amountInWords
├── termsSnapshot
├── status
├── sourceImageReference
├── supportingDocumentReference
├── pdfReference
├── createdAt
└── updatedAt
```

A customer snapshot is recommended so that historical quotations remain
historically accurate even if the customer's master record changes
later.

------------------------------------------------------------------------

# 33. Why Customer Snapshot Is Required

Suppose:

``` text
Customer master:
EXEL RUBBER
Address A
```

Quotation #008 is created.

Later:

``` text
Customer master:
EXEL RUBBER
Address B
```

Quotation #008 must still display Address A.

Therefore:

``` text
Customer Master
        │
        │ copy at creation
        ▼
Quotation Customer Snapshot
```

The quotation is historically independent from later customer-master
changes.

------------------------------------------------------------------------

# 34. Settings

Business settings should be persisted locally.

Suggested structure:

``` text
Settings
├── Business Information
│   ├── companyName
│   ├── contactNumbers
│   ├── GSTIN
│   ├── MSME number
│   ├── address
│   ├── email
│   └── bankDetails
│
├── Quotation Number Configuration
│   ├── prefix
│   ├── businessYear
│   └── nextSequence
│
├── Tax Configuration
│   ├── cgstRate
│   └── sgstRate
│
└── Default Terms
    ├── delivery
    ├── payment
    └── validity
```

------------------------------------------------------------------------

# 35. What Must Not Be Hardcoded

## Environment configuration

``` text
LLM provider
LLM API key
LLM base URL
LLM model
LLM API version
timeouts
retry counts
AI limits
feature flags
```

## Versioned resources

``` text
AI prompts
Extraction schemas
PDF template version
```

## Business settings

``` text
GST %
Quotation prefix
Business year
Next sequence
Terms
Business contact details
Bank details
```

## Database data

``` text
Customers
Quotations
Quotation history
Generated PDF references
```

------------------------------------------------------------------------

# 36. What Should Remain in Code

Core domain rules should remain code:

``` text
Quantity × Rate
Subtotal calculation
GST calculation algorithm
Grand total calculation
Amount-in-words conversion
Quotation state transitions
Validation rules
Number uniqueness rules
PDF rendering logic
```

The values used by those rules can be configurable.

------------------------------------------------------------------------

# 37. PDF Output

The generated PDF follows the provided Vijaya Laxmi Gears digital
quotation design.

The supplied digital quotation establishes the reference layout:

## Header

``` text
Company Logo
VIJAYA LAXMI GEARS
Mobile Numbers
Types of Work / Services
Horizontal separator
```

## Quotation heading

``` text
Quotation

Quotation No.                 Date
VLG-2025/008                  06-12-2025
```

## Customer section

``` text
To

Company Name
Address Line 1
Address Line 2
Area
District - PIN, State
GSTIN
```

## Item table

``` text
S.No | Description | Qty | Unit Price | Amount
```

Description contains:

``` text
Product name
Specifications
```

## Optional Processes

If present:

``` text
Processes Included:
Material, Turning, Teeth Cutting, Keyway, ...
```

If absent, the section is omitted or remains empty according to the
fixed template.

## Terms and Conditions

Fixed-layout section containing the applicable quotation-specific terms
snapshot.

## Totals

``` text
Sub Total
CGST (9%)
SGST (9%)
----------------
Grand Total
```

## Amount in Words

``` text
Amount in words:
Thirty Three Thousand One Hundred Fifty Eight Only.
```

## Business information

``` text
GSTIN
MSME No.
Bank Account Details
Account Name
Bank Name
Account Type
Account Number
Branch
IFSC Code
```

## Authorization

``` text
Authorized Signature / Stamp
FOR VIJAYA LAXMI GEARS
```

## Footer

``` text
Business address
Email
```

------------------------------------------------------------------------

# 38. PDF Template Architecture

The PDF should not be generated by asking the LLM to format the
document.

Recommended:

``` text
ValidatedQuotation
       ↓
Calculation Engine
       ↓
PDF Data Model
       ↓
Fixed PDF Template Renderer
       ↓
PDF
```

The PDF template should be versioned:

``` text
pdf/
└── templates/
    └── quotation-v1/
```

Configuration:

``` env
PDF_TEMPLATE_VERSION=v1
```

A future design can become:

``` text
quotation-v2
```

without changing historical PDFs.

------------------------------------------------------------------------

# 39. PDF Preview

After PDF generation:

``` text
Quotation
   ↓
Generate PDF
   ↓
PDF Preview
   ↓
┌───────────────┐
│ Save          │
│ Share         │
│ Back/Edit     │
└───────────────┘
```

If the user selects Back/Edit:

``` text
PDF
 ↓
Edit quotation
 ↓
Recalculate
 ↓
Regenerate PDF
```

The quotation number remains unchanged.

------------------------------------------------------------------------

# 40. Quotation History

The application provides:

``` text
Quotation History
```

Example:

``` text
VLG-2025/008
EXEL RUBBER PVT LTD
₹47,200
18/07/2026

VLG-2025/007
SPARK ADHESIVES
₹33,158
...
```

Search/filter can be added later.

Selecting a quotation opens the saved quotation.

------------------------------------------------------------------------

# 41. Editing Existing Quotations

When an existing quotation is opened:

``` text
VLG-2025/008
```

the user can edit its data.

The application then:

``` text
Edit
 ↓
Validate
 ↓
Recalculate
 ↓
Regenerate PDF
```

The quotation number remains:

``` text
VLG-2025/008
```

The original handwritten source image remains associated with the
quotation.

------------------------------------------------------------------------

# 42. Deletion Rules

The user can delete older quotations.

Recently created quotations are protected from deletion.

The exact definition of "recently created" should be implemented as a
configurable retention/protection policy rather than scattered through
UI code.

Recommended V1 behavior:

``` text
Recent quotation
    → Delete disabled

Older quotation
    → Delete available
    → Confirmation required
```

Deleting a quotation should also remove its locally stored generated
assets, subject to backup-retention requirements.

------------------------------------------------------------------------

# 43. Local Storage Architecture

Because the application is intended for personal use, business records
are stored locally.

Recommended conceptual storage:

``` text
Android Device
│
├── Local Database
│   ├── customers
│   ├── quotations
│   ├── quotation_items
│   ├── settings
│   └── numbering state
│
└── Application File Storage
    ├── handwritten images
    ├── supporting documents
    └── generated PDFs
```

The database should contain references to large binary files rather than
unnecessarily storing all images/PDFs as database blobs.

------------------------------------------------------------------------

# 44. Recommended Database

For the Android/local side, use SQLite through a React Native-compatible
persistence layer.

For the Spring Boot backend, use a relational persistence model where
needed.

The architecture should keep the quotation domain relational because it
has clear entities and transactional requirements.

Recommended domain tables:

``` text
customers
quotations
quotation_items
application_settings
quotation_number_config
```

Optional:

``` text
quotation_assets
backup_metadata
```

------------------------------------------------------------------------

# 45. Backend Architecture

Spring Boot should expose a clean REST API.

High-level:

``` text
React Native
     │
     │ HTTPS/REST
     ▼
Spring Boot
     │
     ├── Quotation API
     ├── Customer API
     ├── Extraction API
     ├── Settings API
     ├── PDF API
     └── Backup API
```

------------------------------------------------------------------------

# 46. Spring Boot Internal Architecture

Recommended layers:

``` text
Controller
    ↓
Application Service
    ↓
Domain Service
    ↓
Repository
```

Example:

``` text
QuotationController
       ↓
QuotationApplicationService
       ↓
QuotationDomainService
       ↓
QuotationRepository
```

AI integration should be isolated:

``` text
ExtractionController
       ↓
QuotationExtractionService
       ↓
LLMService
       ↓
Provider Adapter
```

------------------------------------------------------------------------

# 47. Suggested Backend Modules

``` text
quotation/
├── api/
├── application/
├── domain/
├── infrastructure/
└── validation/

customer/
├── api/
├── application/
├── domain/
└── infrastructure/

extraction/
├── api/
├── application/
├── domain/
└── infrastructure/

pdf/
├── application/
└── infrastructure/

settings/
├── api/
├── application/
└── infrastructure/

backup/
├── api/
├── application/
└── infrastructure/
```

This is a modular-monolith style architecture.

A microservice architecture is unnecessary for this application.

------------------------------------------------------------------------

# 48. API Design

## Create extraction request

``` http
POST /api/v1/extractions
Content-Type: multipart/form-data
```

Inputs:

``` text
quotationImage
customerMode
supportingDocument (optional)
customerId (when existing)
```

Response:

``` json
{
  "extractionId": "...",
  "status": "COMPLETED",
  "draft": {}
}
```

## Customer APIs

``` http
GET    /api/v1/customers
GET    /api/v1/customers/{id}
POST   /api/v1/customers
PUT    /api/v1/customers/{id}
```

## Quotation APIs

``` http
POST   /api/v1/quotations
GET    /api/v1/quotations
GET    /api/v1/quotations/{id}
PUT    /api/v1/quotations/{id}
DELETE /api/v1/quotations/{id}
```

## PDF

``` http
POST /api/v1/quotations/{id}/pdf
GET  /api/v1/quotations/{id}/pdf
```

## Settings

``` http
GET /api/v1/settings
PUT /api/v1/settings
```

The exact endpoint contract should be finalized during implementation.

------------------------------------------------------------------------

# 49. Extraction Workflow

Detailed workflow:

``` text
1. User selects New Quotation

2. User chooses:
   New Customer / Existing Customer

3. User captures handwritten image

4. User optionally selects one supporting document

5. Backend validates files

6. Image quality check

7. Existing customer:
      load customer record
      send image primarily for item extraction

   New customer:
      send handwritten image + supporting document
      request customer + item extraction

8. Multimodal LLM returns structured output

9. JSON/schema validation

10. Application validation

11. Uncertain/conflicted fields marked

12. Draft returned to mobile application

13. User reviews against source image

14. User edits

15. User accepts

16. Backend allocates quotation number

17. Calculation engine executes

18. Validated quotation persisted

19. PDF generated

20. PDF preview

21. User saves/shares
```

------------------------------------------------------------------------

# 50. Important Ordering Rule

Quotation number allocation should occur at a controlled point in the
transaction.

Do not allocate permanent numbers repeatedly while the user is still
experimenting with extraction.

Recommended:

``` text
Draft
 ↓
Human acceptance
 ↓
Number allocation
 ↓
Final validation
 ↓
Persistence
```

If the business requires the number to be displayed earlier, a temporary
draft identifier can be used.

This prevents unused numbers caused by abandoned drafts.

------------------------------------------------------------------------

# 51. Error Handling

## Image quality failure

``` text
Image unclear
→ ask user for new image
```

## LLM timeout

``` text
Retry
→ fallback provider if configured
→ show understandable failure
```

## LLM rate limit

``` text
Do not crash application.
Show:
"AI service is temporarily unavailable. Please try again."
```

## Invalid LLM JSON

``` text
LLM response
 ↓
Schema validation fails
 ↓
Retry with controlled strategy
 ↓
If still invalid:
manual entry/retry
```

## Missing required item fields

Example:

``` text
Product = present
Quantity = present
Unit Price = missing
```

Show:

``` text
⚠ Unit price needs verification
```

The quotation cannot become final until required values are valid.

## Conflicting customer information

``` text
Document A → Address A
Document B → Address B
```

Show conflict for human review.

------------------------------------------------------------------------

# 52. Validation

The system should distinguish:

### AI validation

Does the response match the expected JSON schema?

### Business validation

Are required values usable?

Examples:

``` text
quantity > 0
unitPrice >= 0
companyName not empty
quotationDate valid
GST rate valid
```

### Calculation validation

Check:

``` text
lineAmount == quantity × unitPrice
```

### Persistence validation

Ensure:

``` text
quotationNumber unique
```

------------------------------------------------------------------------

# 53. Transactional Integrity

Quotation creation/editing should be transactional.

The final operation should guarantee:

``` text
Quotation
+
Items
+
Customer snapshot
+
Terms snapshot
+
Tax values
+
Number
```

are persisted consistently.

A quotation must not exist with half-written item data.

------------------------------------------------------------------------

# 54. Backup Architecture

Google Drive backup is optional.

Primary storage:

``` text
Android local storage
```

Optional backup:

``` text
Local application data
        ↓
Backup/export package
        ↓
Google Drive
```

Backup should not become a hard dependency for normal quotation
creation.

If Google Drive is unavailable, the user should still be able to create
quotations locally.

------------------------------------------------------------------------

# 55. Backup Strategy

A backup package could contain:

``` text
backup/
├── database/export.json
├── customers/
├── quotations/
├── images/
└── pdfs/
```

The exact format can be finalized during implementation.

Backup should include enough metadata to restore:

-   Customers
-   Quotations
-   Quotation items
-   Settings
-   Original images
-   Supporting documents
-   PDFs

------------------------------------------------------------------------

# 56. Security

## Backend

-   API keys stored in environment/secret configuration.
-   Never return LLM API keys to the mobile application.
-   Validate uploaded files.
-   Restrict file sizes.
-   Validate MIME types.
-   Sanitize filenames.
-   Do not trust client-side calculations.

## Mobile

-   Store sensitive local data in application-private storage.
-   Use secure network communication.
-   Avoid logging customer data or API responses unnecessarily.

## AI privacy

Quotation/customer documents may contain sensitive business information.

The system should send only required information to the configured AI
provider.

Provider data-retention/privacy policies must be reviewed before
production use.

------------------------------------------------------------------------

# 57. Configuration Architecture

Configuration is divided into three categories.

## Environment configuration

Deployment/provider specific:

``` text
LLM_PROVIDER
LLM_API_KEY
LLM_BASE_URL
LLM_MODEL
LLM_TIMEOUT
LLM_MAX_RETRIES
LLM_PROMPT_VERSION
LLM_SCHEMA_VERSION
PDF_TEMPLATE_VERSION
```

## Application business configuration

Stored locally:

``` text
GST rates
Quotation prefix
Business year
Next sequence
Terms
Business details
Bank details
```

## Versioned resources

Stored separately from business logic:

``` text
AI prompts
JSON schemas
PDF templates
Static assets
```

------------------------------------------------------------------------

# 58. Configuration Principle

> **Do not hardcode values that are expected to change.**

However:

> **Do not externalize core business invariants merely to avoid
> hardcoding.**

Example:

``` text
Quantity × Rate = Amount
```

is business logic and belongs in code.

But:

``` text
CGST = 9%
```

is business configuration and belongs in Settings.

------------------------------------------------------------------------

# 59. React Native Architecture

Recommended UI structure:

``` text
src/
├── screens/
│   ├── Home
│   ├── NewQuotation
│   ├── CustomerSelection
│   ├── CaptureQuotation
│   ├── ReviewQuotation
│   ├── QuotationPreview
│   ├── QuotationHistory
│   ├── QuotationDetails
│   └── Settings
│
├── components/
├── services/
│   ├── api
│   ├── quotation
│   ├── customer
│   └── file
│
├── state/
├── models/
├── validation/
└── navigation/
```

The UI should remain simple and touch-friendly.

------------------------------------------------------------------------

# 60. User Experience Principles

Because the users are business owners and may not be highly technical:

-   Use large buttons.
-   Minimize typing.
-   Prefer suggestions/autocomplete.
-   Avoid technical AI terminology.
-   Explain failures in plain language.
-   Highlight fields requiring attention.
-   Keep the handwritten image accessible during review.
-   Avoid requiring the user to understand OCR/LLM concepts.
-   Keep the number of screens low.
-   Preserve progress when processing fails.

Instead of:

``` text
LLM extraction failed with HTTP 429
```

show:

``` text
We couldn't process the quotation right now.
Please try again in a moment.
```

------------------------------------------------------------------------

# 61. Main Screens

## Home

``` text
[ + New Quotation ]

Recent Quotations
-----------------
VLG-2025/008
EXEL RUBBER PVT LTD

VLG-2025/007
SPARK ADHESIVES

[Quotation History]
[Settings]
```

## New Quotation

``` text
New Quotation

○ New Customer
○ Existing Customer
```

## Existing Customer

``` text
Search Customer

[ EXEL RUBBER PVT LTD ]

[ Continue ]
```

## Source Selection

``` text
Quotation Image

[ Take Photo ]
[ Choose from Gallery ]

Supporting document (optional)

[ Add Document ]
```

## Review

``` text
Source Image

Customer
...
Items
...

⚠ Please verify highlighted fields

[Accept & Generate]
```

## PDF Preview

``` text
[PDF Preview]

[Back & Edit]
[Save]
[Share]
```

------------------------------------------------------------------------

# 62. End-to-End System Diagram

``` text
                         ANDROID
┌──────────────────────────────────────────────────────┐
│                  React Native UI                     │
│                                                      │
│ New Quotation → Customer → Source → Review → Preview │
└───────────────────────┬──────────────────────────────┘
                        │ HTTPS
                        ▼
┌──────────────────────────────────────────────────────┐
│                  SPRING BOOT                         │
│                                                      │
│ ┌─────────────┐ ┌─────────────┐ ┌────────────────┐ │
│ │ Quotation   │ │ Customer    │ │ Settings       │ │
│ │ Module      │ │ Module      │ │ Module         │ │
│ └──────┬──────┘ └──────┬──────┘ └────────────────┘ │
│        │               │                            │
│        ▼               ▼                            │
│ ┌─────────────────────────────────────────────────┐ │
│ │             Application Services                │ │
│ └──────────────────────┬──────────────────────────┘ │
│                        │                             │
│        ┌───────────────┼───────────────┐             │
│        ▼               ▼               ▼             │
│   Extraction      Calculation       PDF Renderer      │
│    Service         Engine                              │
│        │               │               │              │
│        ▼               │               ▼              │
│   LLM Adapter          │          PDF Template        │
│        │               │                              │
└────────┼───────────────┼──────────────────────────────┘
         │               │
         ▼               ▼
┌─────────────────┐   Local/Configured
│ Multimodal LLM  │   Persistence
│ Provider        │
└─────────────────┘
```

------------------------------------------------------------------------

# 63. Recommended Technology Stack

## Mobile

-   React Native
-   TypeScript
-   Android
-   React Navigation
-   Camera/image picker library
-   PDF preview component
-   Native Android share functionality

## Backend

-   Java
-   Spring Boot
-   Spring Web
-   Spring Validation
-   Spring Data JPA where relational persistence is used
-   Jackson for JSON
-   HTTP client/WebClient for external AI APIs

## Database

-   SQLite/local relational persistence for business data
-   Backend persistence layer kept relational

## AI

-   Multimodal vision-capable LLM
-   Provider abstraction
-   Structured JSON output
-   Versioned prompt and schema

Initial provider candidate:

-   Google Gemini Flash-class multimodal model, configured externally

Provider choice must remain replaceable.

## PDF

-   Java PDF rendering library
-   Fixed template
-   Versioned template resources

## Backup

-   Google Drive API
-   OAuth-based authorization
-   Optional feature

## Build/deployment

-   Gradle
-   Docker for Spring Boot development/deployment if desired
-   Environment-specific configuration

------------------------------------------------------------------------

# 64. Why Not Microservices?

This is a personal-use application with one business and one primary
workflow.

Microservices would introduce unnecessary:

-   network boundaries
-   deployment complexity
-   service discovery
-   authentication overhead
-   monitoring requirements

A modular Spring Boot monolith is more appropriate.

The code should still have clear module boundaries so components can be
extracted later if necessary.

------------------------------------------------------------------------

# 65. Why Not OCR in V1?

The system's objective is not general text transcription.

It needs semantic extraction from a visual document.

A multimodal LLM can directly reason about:

-   handwriting
-   table structure
-   quantities
-   rates
-   product names
-   specifications
-   document context

Therefore:

``` text
Image → Multimodal LLM
```

is preferred for V1.

OCR remains an optional future enhancement:

``` text
Image
 ├── Vision LLM
 └── OCR
       ↓
 Combined validation
```

OCR should only be introduced if testing shows measurable improvement.

------------------------------------------------------------------------

# 66. Important Design Principle: AI Does Not Own the Truth

The LLM is an extraction assistant.

It is not the source of truth for:

-   money calculations
-   quotation number
-   GST configuration
-   final customer data
-   business settings
-   PDF totals

The source of truth becomes:

``` text
Human-approved structured quotation
+
Deterministic application calculations
```

------------------------------------------------------------------------

# 67. AI Failure Does Not Mean Application Failure

The application should remain useful even when AI is unavailable.

Possible behavior:

``` text
AI available
    ↓
Automatic extraction

AI unavailable
    ↓
Manual quotation entry
```

A manual-entry fallback is recommended because a business owner should
not be blocked from creating a quotation solely because a free AI tier
has expired.

------------------------------------------------------------------------

# 68. Provider Fallback

The configuration should allow a fallback provider.

Conceptually:

``` text
Primary LLM
    ↓
Failure?
    │
 ┌──┴───┐
 No     Yes
 │       │
 ▼       ▼
Result  Fallback LLM
```

Configuration:

``` env
LLM_PRIMARY_PROVIDER=...
LLM_FALLBACK_PROVIDER=...
LLM_FALLBACK_ENABLED=true
```

The fallback provider must support the required multimodal and
structured-output capabilities.

------------------------------------------------------------------------

# 69. Testing Strategy

## Unit tests

Test:

-   GST calculations
-   Subtotal
-   Grand total
-   Amount in words
-   Number generation
-   Custom number uniqueness
-   Terms snapshot
-   Customer snapshot
-   Validation

## AI extraction tests

Maintain a dataset of real handwritten quotations.

Evaluate:

-   Customer name accuracy
-   Address accuracy
-   GSTIN accuracy
-   Product accuracy
-   Specification preservation
-   Quantity accuracy
-   Unit accuracy
-   Rate accuracy
-   Multiple-item extraction

## Integration tests

Test:

``` text
Image
→ API
→ LLM adapter
→ structured extraction
→ validation
```

## PDF tests

Verify:

-   correct number
-   correct date
-   correct customer
-   correct items
-   correct totals
-   correct GST
-   correct amount in words
-   correct footer/header

## Regression tests

Every prompt/model change should be evaluated against known handwritten
quotation samples.

This is especially important because changing the LLM provider or model
may change extraction behavior.

------------------------------------------------------------------------

# 70. AI Evaluation Dataset

Create a small local test set:

``` text
ai-evaluation/
├── quotation-001/
│   ├── handwritten.jpg
│   ├── supporting.pdf
│   └── expected.json
├── quotation-002/
│   ├── handwritten.jpg
│   └── expected.json
└── ...
```

Expected JSON represents the manually verified ground truth.

When changing:

-   model
-   prompt
-   schema
-   provider

run the evaluation again.

This prevents a new model from silently degrading extraction quality.

------------------------------------------------------------------------

# 71. Observability

The backend should log:

-   request ID
-   extraction ID
-   provider
-   model
-   prompt version
-   schema version
-   processing duration
-   success/failure
-   validation failure type

Do not log sensitive document contents or API keys unnecessarily.

This makes provider/model migration easier to diagnose.

------------------------------------------------------------------------

# 72. Versioning

Important versions:

``` text
Application version
Extraction schema version
Prompt version
PDF template version
LLM provider/model configuration
```

A quotation should record enough metadata to know how it was produced.

Example:

``` text
Quotation
├── extractionSchemaVersion
├── promptVersion
├── modelIdentifier
└── pdfTemplateVersion
```

This is valuable for debugging and reproducibility.

------------------------------------------------------------------------

# 73. Data Lifecycle

``` text
Source image
      ↓
AI extraction
      ↓
Draft
      ↓
Human correction
      ↓
Validated quotation
      ↓
PDF
      ↓
History
      ↓
Optional backup
```

The original source image remains associated with the quotation.

------------------------------------------------------------------------

# 74. Key Architectural Trade-offs

## Multimodal LLM vs OCR + LLM

### Multimodal LLM

Pros:

-   understands document context
-   handles handwriting semantically
-   fewer processing stages
-   simpler architecture

Cons:

-   provider dependent
-   potentially less deterministic
-   model changes can affect extraction

Decision:

**Use multimodal LLM for V1.**

## Local-first vs cloud-first storage

Local-first is appropriate because:

-   personal business use
-   simple deployment
-   no need for multi-user synchronization
-   works when internet is unavailable for storage/history

AI still requires internet.

## Modular monolith vs microservices

Decision:

**Modular Spring Boot monolith.**

------------------------------------------------------------------------

# 75. Non-Functional Requirements

## Reliability

-   AI failures must not corrupt quotations.
-   Calculations must be deterministic.
-   Quotation numbers must be unique.

## Performance

Normal local operations should feel immediate.

AI processing can take longer but must display progress.

## Usability

A business owner should be able to create a quotation without
understanding AI/OCR concepts.

## Maintainability

Provider-specific code must be isolated.

Prompts, schemas and templates must be versioned.

## Security

Secrets must remain server-side.

## Recoverability

Optional Google Drive backup provides protection against device loss.

## Extensibility

The system should support future:

-   LLM providers
-   OCR fallback
-   additional PDF templates
-   cloud synchronization
-   additional document sources
-   additional tax configurations

------------------------------------------------------------------------

# 76. Implementation Phases

## Phase 1 --- Domain foundation

Implement:

-   Customer model
-   Quotation model
-   Quotation items
-   Settings
-   Numbering
-   GST calculation
-   Amount in words
-   Terms snapshot
-   Customer snapshot

## Phase 2 --- Basic manual quotation

Implement:

``` text
New quotation
→ manually enter details
→ calculate
→ PDF
```

This establishes a stable baseline without AI.

## Phase 3 --- Multimodal extraction

Implement:

``` text
Image
→ LLM
→ structured JSON
→ validation
```

## Phase 4 --- Human review

Implement:

-   confidence flags
-   editable fields
-   source-image comparison

## Phase 5 --- Supporting documents

Implement one supporting document.

## Phase 6 --- Customer memory

Implement:

-   customer storage
-   search
-   suggestions
-   existing customer workflow

## Phase 7 --- History/editing

Implement:

-   quotation history
-   editing
-   PDF regeneration
-   deletion rules

## Phase 8 --- Google Drive backup

Implement optional backup and restore.

## Phase 9 --- AI evaluation and provider fallback

Implement:

-   extraction regression dataset
-   provider adapters
-   fallback provider
-   model switching

------------------------------------------------------------------------

# 77. Final Architecture

The final conceptual architecture is:

``` text
                         USER
                          │
                          ▼
                  React Native Android
                          │
              ┌───────────┴───────────┐
              │                       │
       New / Existing          Camera / Gallery
       Customer                     │
              │                     │
              └───────────┬─────────┘
                          ▼
                    Source Manager
                          │
             ┌────────────┴────────────┐
             │                         │
       Handwritten Image       Supporting Document
             │                         │
             └────────────┬────────────┘
                          ▼
                 Spring Boot API
                          │
                          ▼
                 Image Quality Check
                          │
                          ▼
                  Extraction Service
                          │
                          ▼
                  LLM Provider Layer
                          │
                 ┌────────┴────────┐
                 │                 │
          Primary Provider    Fallback Provider
                 │                 │
                 └────────┬────────┘
                          ▼
                   Structured Draft
                          │
                          ▼
                Schema + Business Validation
                          │
                          ▼
                    Human Review
                          │
                          ▼
                  Accepted Quotation
                          │
            ┌─────────────┼─────────────┐
            ▼             ▼             ▼
        Numbering    Calculation     Snapshot
        Service        Engine        Terms/Customer
            │             │             │
            └─────────────┼─────────────┘
                          ▼
                   Final Quotation
                          │
                          ▼
                    PDF Renderer
                          │
                          ▼
                     PDF Preview
                          │
                    ┌─────┴─────┐
                    ▼           ▼
                  Save        Share
                    │
                    ▼
             Local Quotation History
                    │
                    ▼
             Optional Google Drive
                   Backup
```

------------------------------------------------------------------------

# 78. Architectural Principles to Freeze

1.  **AI proposes; human approves.**
2.  **Multimodal LLM is the primary extraction mechanism in V1.**
3.  **OCR is optional and not part of the mandatory V1 pipeline.**
4.  **LLM never owns financial calculations.**
5.  **Quotation numbers are configuration-driven, not calendar-driven.**
6.  **Quotation numbers are unique and remain immutable after
    creation.**
7.  **Historical quotations retain customer and terms snapshots.**
8.  **Existing customers are loaded from local memory rather than
    rediscovered by AI.**
9.  **One supporting document is allowed in V1.**
10. **Original handwritten images are retained.**
11. **Business data is stored locally.**
12. **Google Drive is optional backup, not a runtime dependency.**
13. **LLM provider/model/API configuration is externalized.**
14. **API keys never enter the Android application.**
15. **Prompts, schemas and PDF templates are versioned.**
16. **Business settings are editable without rebuilding the
    application.**
17. **Core business rules remain deterministic code.**
18. **Provider changes should require configuration changes or an
    adapter, not quotation-domain rewrites.**
19. **AI uncertainty must be visible to the user.**
20. **The final PDF is generated only from validated structured
    quotation data.**

------------------------------------------------------------------------

# 79. Final Technology Decision

  -----------------------------------------------------------------------
  Layer                               Technology
  ----------------------------------- -----------------------------------
  Mobile                              React Native + TypeScript

  Platform                            Android

  Backend                             Java + Spring Boot

  API                                 REST/HTTPS

  Local business persistence          SQLite-compatible relational
                                      storage

  AI                                  Multimodal vision-capable LLM

  Initial AI candidate                Gemini Flash-class multimodal model

  AI integration                      Provider adapter abstraction

  AI output                           Versioned structured JSON

  Prompt management                   Versioned external resources

  Calculations                        Deterministic Java domain service

  PDF                                 Fixed versioned Java PDF template

  File storage                        Android application-private storage

  Backup                              Optional Google Drive

  Configuration                       Environment variables + persisted
                                      application settings

  Architecture                        Modular monolith

  Authentication                      Minimal/local-first for V1; backend
                                      security can evolve

  Deployment                          Spring Boot deployable
                                      independently from Android client
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 80. Conclusion

This system should not be treated as an OCR application or as an AI
document-generation application.

It is better understood as a:

> **Human-verified quotation digitization system with multimodal
> AI-assisted data extraction and deterministic business processing.**

The AI handles the difficult interpretation problem:

``` text
handwriting + document context
        ↓
structured business facts
```

The application handles the reliable business problem:

``` text
structured facts
        ↓
validation
        ↓
calculations
        ↓
quotation numbering
        ↓
fixed business rules
        ↓
professional PDF
```

This separation keeps the system maintainable even when the AI provider,
model, free tier, prompt, or extraction strategy changes.
