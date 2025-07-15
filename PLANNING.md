# ***Product Requirements Document (PRD)***

## ***1\. Purpose & Scope***

*The **Subscription Manager** app helps users track and manage their paid and trial subscriptions privately and entirely on‑device (Android 15+). It provides timely reminders before renewals, clear cost analytics, and a modern Material Design 3 experience while guaranteeing that no personal data ever leaves the phone.*

## ***2\. Objectives***

* ***Transparency** — Give users an at‑a‑glance view of upcoming charges and cumulative costs.*

* ***Privacy‑first** — Store and process all data locally with strong encryption; no network access required.*

* ***Delightful UX** — Leverage Android 15 features (dynamic color, predictive back, partial screen recording, etc.) and Material 3 for a native, fluid feel.*

* ***Global reach** — Ship with full localization and RTL support from day one.*

* ***Quality & Velocity** — Follow Test‑Driven Development (TDD) and automated CI/CD to keep shipping reliable updates.*

## ***3\. User Personas & Pain Points***

| *Persona* | *Key Needs* |
| ----- | ----- |
| ***Budget‑Conscious Student*** | *Wants reminders for Spotify & Netflix renewals to avoid overdrafts.* |
| ***Freelancer*** | *Tracks dozens of SaaS tools; needs monthly and annual cost breakdowns for tax reporting.* |
| ***Privacy Advocate*** | *Refuses to share finance data with cloud services; demands local encryption.* |

## ***4\. User Stories (MoSCoW)***

### ***Must‑Have***

1. ***Add subscription** with name, category, price, billing cycle, next renewal date, free‑trial flag, notes.*

2. ***Dashboard** summarizing total monthly & yearly spend and next 7‑day renewals.*

3. ***Detailed list view** with sorting/filtering by category, price, next charge.*

4. ***Reminders & notifications** 1\) configurable days before renewal, 2\) a separate trial‑ending alert.*

5. ***Analytics**: Charts for monthly, yearly, and category breakdown; export CSV locally.*

6. ***Settings**: light/dark mode, language switcher, currency, backup/restore (local file).*

### ***Should‑Have***

* *Predictive suggestions when typing known services (icon \+ category auto‑fill).*

* *Widget / Quick Settings tile for glanceable spend.*

### ***Could‑Have***

* *Optical recognition of subscription emails (local on‑device ML).*

* *Wear OS companion glance.*

## ***5\. Functional Requirements***

| *ID* | *Description* |
| ----- | ----- |
| *FR‑001* | *The app **shall** run on Android 15 (API 35) and above only.* |
| *FR‑002* | *The app **shall** store all user data in **Encrypted Proto DataStore**.* |
| *FR‑003* | *The app **shall** schedule exact alarm reminders using **AlarmManager** \+ **SCHEDULE\_EXACT\_ALARM** permission; fall back to WorkManager if denied.* |
| *FR‑004* | *The app **shall** issue **POST\_NOTIFICATIONS** runtime permission request on first launch.* |
| *FR‑005* | *The app **shall** support at least English, Italian, Spanish, German, French at launch.* |
| *FR‑006* | *The app **shall** compute and display analytics offline without third‑party analytics SDKs.* |
| *FR‑007* | *The app **shall** export/import data using an AES‑256 encrypted JSON file selectable via SAF.* |

## ***6\. Non‑Functional Requirements***

* ***Performance** — Dashboard loads \<150 ms on Pixel 7\.*

* ***Security** — FIPS‑140‑2 compliant encryption; use Jetpack Security Crypto \+ hardware‑backed Keystore.*

* ***Accessibility** — WCAG 2.1 AA; TalkBack labels, minimum 48dp touch targets.*

* ***Battery** — Background jobs limited; idle‑optimized alarms.*

* ***Offline‑first** — No Internet permission declared.*

## ***7\. Data Model (Proto)***

*message Subscription {*

  *string id \= 1;*

  *string name \= 2;*

  *string category \= 3;*

  *double price \= 4;       // in default currency*

  *string currency \= 5;*

  *BillingCycle cycle \= 6; // MONTHLY, YEARLY, WEEKLY, CUSTOM*

  *int64 next\_renewal\_epoch \= 7;*

  *bool is\_trial \= 8;*

  *int32 trial\_days\_left \= 9;*

  *string notes \= 10;*

*}*

## ***7a. Encrypted Import / Export***

### ***Goals***

* *Give users full control of their data: back it up locally, migrate to a new device, or archive for bookkeeping—without ever exposing unencrypted information.*

### ***Encryption Scheme***

| *Item* | *Detail* |
| ----- | ----- |
| ***Algorithm*** | *AES-256–GCM* |
| ***Key source*** | *Android Keystore hardware-backed symmetric key (KEY\_SIZE \= 256, PURPOSE \= encrypt/decrypt, BLOCK\_MODE \= GCM)* |
| ***Export format*** | *Single subscriptions.enc file containing (12-byte IV)(ciphertext)(16-byte auth tag)* |
| ***Metadata*** | *Encrypted JSON payload identical to on-device DataStore schema for easy round-trip.* |
| ***Optional passphrase*** | *If user opts in, the symmetric key is re-encrypted with a scrypt-derived key from the passphrase to enable cross-device restore on a different device/Keystore.* |

### ***User Flows***

1. ***Export** — Settings ▸ Backup & Restore ▸ Export encrypted backup → SAF picker → generate encrypted file → snackbar confirmation.*

2. ***Import** — Settings ▸ Backup & Restore ▸ Import encrypted backup → SAF picker → (optional passphrase prompt) → decrypt → “Merge” or “Replace” dialog.*

### ***Error Handling***

* *Wrong passphrase → shake animation \+ error banner.*

* *Auth tag mismatch → “Backup is corrupt or has been tampered with.”*

### ***Functional Requirement Additions***

| *ID* | *Description* |
| ----- | ----- |
| *FR-008* | *The app **shall** support encrypted backup/restore as described in §7a, with optional passphrase for cross-device use.* |

### ***Testing Additions***

* ***Unit** — round-trip encryption/decryption with random payload.*

* ***Instrumentation** — import/export flow on Pixel 8 & Emulator API 35\.*

* ***Fuzz** — corrupt random byte; verify graceful error.*

## ***8\. UX/UI Guidelines***

* ***Jetpack Compose \+ Material 3** components only.*

* *Dynamic Color support (Material You) with automatic palette.*

* *Navigation Rail on large screens; Bottom Bar on phones.*

* *Predictive back animations and edge‑to‑edge layout.*

* *Charts built with androidx.compose.ui.graphics & recharts-android (local lib).*

## ***9\. Technical Architecture***

| *Layer* | *Tech* |
| ----- | ----- |
| *UI* | *Jetpack Compose, Material 3, Accompanist‑Navigation, Hilt‑ViewModel* |
| *Domain* | *Kotlin Coroutines \+ Flow, Clean Architecture* |
| *Data* | *Encrypted Proto DataStore, Kotlin Serialization* |
| *Notifications* | *AlarmManager \+ WorkManager fallback* |
| *Analytics* | *Kotlin Multiplatform kotlin‑statistics for aggregation* |

## ***10\. CI/CD & Repository Standards***

* ***Repo**: github.com/your‑org/subscription‑manager*

* ***Branching**: trunk‑based; PRs require 90% coverage.*

* ***GitHub Actions Workflows***

  * ***ci.yml** — Trigger on PR; steps: setup JDK 21, cache Gradle; run ./gradlew lint ktlintCheck testDebugUnitTest detekt.*

  * ***instrumentation.yml** — Trigger nightly; run Android Emulator API 35 headless; execute connectedDebugAndroidTest.*

  * ***release.yml** — Trigger on version tag v\*.\*.\*; build bundleRelease and assembleRelease, sign via secrets, create GitHub Release, upload APK & AAB artifacts.*

  * *All workflows publish JUnit & Kover reports as Check Run annotations.*

* ***Static Analysis**: Detekt, KtLint, Spotless.*

## ***11\. Testing Strategy***

* ***TDD**: Red‑Green‑Refactor cycle for every feature.*

* ***Unit Tests**: 100% domain layer, 90% data.*

* ***UI Tests**: Compose UI tests for critical flows.*

* ***Contract Tests**: Proto schema migration tests.*

## ***12\. Internationalization & Localization***

* *Use string‑resource qualifiers (values‑es, values‑it, etc.).*

* *Plural support via plurals, number/currency formatting with ICU APIs.*

* *Pseudolocale tests in CI.*

## ***13\. Accessibility***

* *Ensure semantic labels on Compose nodes.*

* *Provide high‑contrast theme toggle.*

* *Test with Android Accessibility Test Suite in CI.*

## ***14\. Privacy & Compliance***

* *No analytics SDKs; optional anonymous crash logs via App Ops consent.*

* *GDPR & CCPA compliant (data never leaves device by default).*

## ***15\. Milestones***

| *Phase* | *Duration* | *Deliverables* |
| ----- | ----- | ----- |
| *0 — Setup* | *1 wk* | *CI/CD, templates, data schema* |
| *1 — Core CRUD* | *3 wks* | *Add/edit/delete subscription, local storage* |
| *2 — Reminder Engine* | *2 wks* | *Alarm \+ notifications* |
| *3 — Analytics & Dashboard* | *3 wks* | *Charts, cost calc* |
| *4 — Settings & i18n* | *2 wks* | *Dark/light, languages* |
| *5 — Hardening* | *2 wks* | *Instrumentation tests, accessibility* |
| *6 — Launch Beta* | *1 wk* | *Tagged release, Play Console internal* |

