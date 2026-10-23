# BMW Buyer's Toolkit

A backend service for shopping used BMWs with more context than a normal listing site gives you. Search by chassis code, decode a VIN into its engine and options, see which known failure points apply to that exact car, and get a ready-to-send list of seller questions (with evidence requests) so you walk into a pre-purchase inspection knowing what to check.

Built with Java 21, Spring Boot, PostgreSQL, and Redis. Background work runs on the [Job Scheduler](../job-scheduler) project.

> Status: planning complete, development starting. See [PROGRESS.md](./PROGRESS.md) for the live checklist.

## Why this project exists

Buying a used BMW means checking the same things over and over: what exactly is this car, which engine does it have, what options does it carry, what typically fails on it, and what should I ask the seller. Generic listing sites don't answer any of that, and BMW specifics (LCI engine changes, Individual paint, option packages, platform-specific failures) are easy to miss as a first-time buyer.

It's also a personal project built for the joy of it. Sites like AutoTempest exist, but none of them focus on BMW depth. This one does.

## Features

- **Chassis-based search.** Pick a chassis (e.g. G12) and the search expands to its production years (2016 to 2022), then refine by exact year, LCI vs. pre-LCI, model, price, and mileage.
- **VIN decoding** into model, body, engine, transmission, plant, and production date.
- **Engine platform resolution** that handles mid-lifecycle changes (e.g. F30 pre-LCI 335i = N55, F30 LCI 340i = B58; 328i = N20, with N26 for SULEV variants), with a confidence indicator (decoded vs. inferred).
- **Option decoding and highlights.** Surface the interesting equipment: BMW Individual paint and interior (single/double), massage seats, panoramic/Sky Lounge roof, rear entertainment, Luxury/Lounge packages, and similar, driven by a curated option-code table.
- **Known-issues knowledge base** across engine, chassis, transmission, and model layers (e.g. N20 timing chain guides, N54 HPFP, E46 rear subframe), with severity, mileage ranges, repair cost ranges, and sources.
- **Seller question generator.** Per-listing questions built from the car's applicable issues, filtered by mileage and severity, each asking for evidence (invoice, shop record, photos). Includes a copy-paste message or email draft. The tool never contacts sellers itself.
- **Question tracking** per listing: not asked, asked, confirmed with evidence, not done, unknown.
- **High-impact issue handling.** Structural or total-loss-risk issues are flagged separately from the score and cap it until verified.
- **Recall lookup** via NHTSA, refreshed on a schedule.
- **Deal scoring** with a transparent factor breakdown.
- **Price history** with drop detection.
- **Deduplication** by VIN, with fuzzy matching when VIN is missing.
- **Alerts** (email or webhook) on price drops, new recalls, and watchlist matches.
- **Resilient enrichment:** failed external calls retry with backoff and dead-letter after repeated failure.

## Listing ingestion

Listings enter the system only through channels that respect source terms of service:

- Manual submission via API (VIN, price, mileage, URL, notes).
- CSV import.
- Official APIs where available (e.g. a marketplace developer API).
- Optional: a licensed listings data provider, if configured.
- Optional: generated deep links to other sites' pre-filtered searches (no data pulled).

This project does **not** scrape sites that prohibit it, and does **not** contact sellers automatically. Ingestion sits behind an adapter interface (`ListingSource`) so sources can be added or swapped without touching the pipeline.

## Architecture

```
Listing submitted (API / CSV / adapter)
        |
        v
  Listings API (Spring Boot)
        |
        +--> PostgreSQL
        |
        +--> Job Scheduler (Redis)
                 |
                 +--> decode-vin job       --> VinDecoder (NHTSA vPIC) + VinOptionsProvider (TBD)
                 +--> resolve-engine job   --> chassis/engine reference tables
                 +--> fetch-recalls job    --> NHTSA recalls
                 +--> build-questions job  --> known issues -> seller questions
                 +--> score-listing job    --> knowledge base + rules
                 +--> refresh-listing      (recurring)
                 +--> send-alert job       --> email / webhook
```

Each enrichment step is an independent job, so one failing API doesn't block the rest, and every step is retried and observable.

### Provider interfaces

| Interface | Purpose | Initial implementation |
|---|---|---|
| `ListingSource` | Bring listings in | Manual API + CSV |
| `VinDecoder` | Basic VIN decode | NHTSA vPIC |
| `VinOptionsProvider` | VIN to BMW option codes | TBD (see open questions in PROGRESS.md) |

> `VinOptionsProvider` is deliberately abstract. Check any candidate provider's terms and API availability before integrating. Do not automate a site that doesn't offer API access or permit it.

## Reference data (curated, versioned seed files)

Stored under `/data/` as YAML or CSV and loaded by Flyway/seed jobs. Every entry should cite a source. **Verify each row against reliable sources before trusting it; the examples below are illustrative.**

### Chassis reference

```yaml
- chassis: G12
  model_line: 7 Series
  years: [2016, 2022]
  lci_year: 2020          # verify
- chassis: F30
  model_line: 3 Series
  years: [2012, 2019]
  lci_year: 2015          # verify
```

Chassis search expands to the year range; the user can narrow to exact year or pre/post-LCI.

### Engine applications

```yaml
- chassis: F30
  model_name: 335i
  engine: N55
  lci: false
- chassis: F30
  model_name: 340i
  engine: B58
  lci: true
- chassis: F30
  model_name: 328i
  engine: N20          # N26 on SULEV variants; verify market/year
  lci: false
```

Resolution order: (1) BMW type code from the VIN, (2) decoder output, (3) fallback via chassis + production date + model name. Each result stores `source` and `confidence` (`decoded` | `inferred`).

### Option catalog

```yaml
- code: "6NL"          # verify code
  name: "BMW Individual Merino leather"
  category: interior
  highlight: true
  desirability: high
```

Highlighted categories: paint (incl. Individual), interior, comfort (massage seats, ventilated), roof, entertainment, packages, driver assistance, and sound.

### Known issues

Scoped across layers so a car collects issues from its engine, chassis, transmission, and model.

```yaml
- scope_type: engine          # engine | chassis | transmission | model | system
  scope_value: N20
  category: engine
  issue: Timing chain guide wear
  severity: high
  typical_mileage: [60000, 120000]
  repair_cost_usd: [2500, 4500]
  total_loss_risk: false
  inspection_required: false
  affected_variants: "verify"
  sources: [TODO]

- scope_type: chassis
  scope_value: E46
  category: body/structure
  issue: Rear subframe mounting reinforcement / cracking
  severity: critical
  total_loss_risk: true
  inspection_required: true
  sources: [TODO]
```

### Seller questions

```yaml
- issue_ref: N20-timing-chain-guides
  question: "Have the timing chain guides been replaced? Please send the invoice or shop record."
  why_it_matters: "Guide failure can lead to severe engine damage."
  good_answer: "Replaced with dated invoice, mileage, and shop name."
  red_flag_answer: "Unknown, or no records."
  evidence_required: [invoice, shop name, mileage at service]
  photo_request: []
  inspection_method: null
  recommend_specialist_ppi: false
```

For structural items (e.g. E46 subframe), questions request specific photos and records, and the checklist states plainly that a seller's word is not enough and a specialist pre-purchase inspection is recommended.

> This data is a starting point for an inspection, not a guarantee about any specific car.

## Seller question workflow

1. Listing is decoded (vehicle, engine, options).
2. `build-questions` collects applicable issues from all scope layers.
3. Filter and rank by mileage relevance, severity, repair cost, and total-loss risk. Skip items already confirmed.
4. Output a prioritized list plus a copy-paste message or email draft.
5. The user marks each question's state as answers arrive: `not_asked`, `asked`, `confirmed_with_evidence`, `not_done`, `unknown`.
6. States feed the scoring engine.

## Deal scoring

A transparent weighted sum; the API returns the full breakdown.

- Price vs. comparable listings (same chassis, mileage band)
- Mileage relative to age
- Applicable known issues, adjusted by question state (confirmed with evidence raises confidence; unknown on a high-severity issue lowers it)
- Open recalls
- Desirable options (from the option catalog)
- Service history completeness

**High-impact override:** any unresolved `total_loss_risk` issue is shown as a hard flag separate from the score, and caps the maximum score until the user records evidence of inspection or repair. Weights and caps live in configuration.

## Data model

| Table | Purpose |
|---|---|
| `chassis_ref` | Chassis code, model line, start/end year, LCI year. |
| `engine_applications` | Chassis, model name, engine code, LCI flag, market, production range. |
| `option_catalog` | Option code, name, category, highlight flag, desirability. |
| `vehicles` | VIN (unique), make, model, year, chassis, engine code, trim, resolution source and confidence. |
| `vehicle_options` | Vehicle id, option code. |
| `listings` | Source, URL, vehicle id, asking price, mileage, status, first/last seen. |
| `price_history` | Listing id, price, observed_at. Append-only. |
| `recalls` | Vehicle id, campaign number, component, summary, remedy status, fetched_at. |
| `known_issues` | Scope type/value, category, issue, severity, ranges, total_loss_risk, inspection_required, sources. |
| `seller_questions` | Issue id, question text, why it matters, good/red-flag answers, evidence and photo requests. |
| `listing_questions` | Listing id, question id, state, evidence note, updated_at. |
| `listing_scores` | Listing id, score, factor breakdown (JSON), flags, computed_at. |
| `watchlists` | Chassis, model, max price, max mileage, options of interest. |
| `alerts` | Rule, listing id, status, sent_at. |

## API (planned)

```
POST   /listings                          Submit a listing (VIN, price, mileage, url)
POST   /listings/import                   CSV import
GET    /listings/{id}                     Listing with vehicle, options, recalls, score, history
GET    /listings?chassis=G12&yearFrom=2018&yearTo=2020&engine=B58&maxPrice=40000&hasOption=...
GET    /listings/{id}/questions           Prioritized seller questions with state
PATCH  /listings/{id}/questions/{qid}     Update state / attach evidence note
GET    /listings/{id}/seller-message      Copy-paste message/email draft
GET    /vehicles/{vin}                    Decoded vehicle, options, recalls, applicable issues
GET    /chassis                           Chassis list with year ranges
GET    /chassis/{code}/issues             Known issues across layers
POST   /watchlists                        Create alert rule
GET    /alerts                            Recent alerts
```

## Testing

- Unit tests: scoring engine (including caps/flags), dedupe, chassis-to-year expansion, engine resolution (LCI edge cases), question filtering.
- Contract tests for external clients using recorded responses (WireMock).
- Integration tests with Testcontainers (PostgreSQL, Redis).
- Failure tests: external timeouts and 5xx responses trigger retries, then dead-letter, without losing the listing.
- Data validation tests: every known issue and question has a source and a valid scope.

```bash
./mvnw test
```

## Results

Fill in from real runs.

| Metric | Result |
|---|---|
| Listings processed end to end | TODO |
| Engine resolution: decoded vs. inferred | TODO % |
| Enrichment success rate with retries | TODO % |
| Duplicate listings collapsed | TODO |
| Median time from submission to scored listing | TODO sec |

## Running locally

Requirements: Java 21, Docker.

```bash
docker compose up -d postgres redis
./mvnw spring-boot:run
```

## Design decisions and tradeoffs

- **Jobs per enrichment step** vs. one big pipeline: isolates failures and makes retries cheap.
- **Scoped known issues** (engine, chassis, transmission, model) vs. engine-only: structural issues like the E46 subframe aren't engine problems.
- **Hard flag and score cap for catastrophic issues** vs. weighted sum only: a low price shouldn't offset a potential total loss.
- **Copy-paste seller messages** vs. automated contact: respects source terms and avoids spam.
- **Evidence-first questions:** "yes, it was done" without proof isn't verification.
- **Provider interfaces for sources and option decoding:** the riskiest dependencies stay swappable.
- **Append-only price history:** preserves the full timeline for drop detection.
- **Transparent scoring** vs. a black-box model: easier to explain, test, and tune.
- **Ingestion limited to permitted channels:** slower to populate, but legally and ethically clean.

## Roadmap

- [ ] Chassis/engine/option reference data for a first set of chassis (E46, F30, G20, G12, others as desired)
- [ ] Known issues + seller questions for N20, N55, N54, B48, B58, E46 subframe
- [ ] One real listing source via adapter
- [ ] Pre-purchase inspection checklist (extends seller questions)
- [ ] Maintenance reminders per VIN
- [ ] NHTSA complaints data as an additional risk signal
- [ ] Simple web UI
- [ ] Deployment to AWS with Terraform and GitHub Actions

## Tech stack

Java 21, Spring Boot, PostgreSQL, Redis, Flyway, WireMock, JUnit 5, Testcontainers, Docker, Maven.

## Data sources

- NHTSA vPIC API (VIN decoding)
- NHTSA recalls API
- BMW option decoding: provider TBD
- Listing sources: TBD (permitted channels only)
- Known-issue sources: service bulletins, repair-shop documentation, and reputable enthusiast references, cited per entry

Check each provider's current documentation and terms before use.
