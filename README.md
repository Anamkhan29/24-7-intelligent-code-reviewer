# The 24/7 Intelligent Code Reviewer (Java / Spring Boot)

An always-on, multi-language code reviewer built entirely on Google Cloud. Authenticated
users submit source code and get back a standardized **1–10 quality rating** plus specific
findings (bugs, security, performance, architecture, formatting). Reviews are **grounded in
historical rules** learned from past reviews, and each user's history is tracked so their
code quality **trend over time** is visible.

This is the Java (Spring Boot) implementation of the AIM Code Kitchen track:
*Multi-language Reviews, Quality Rating & Historical Learning.*

## Architecture (all Google Cloud)

- **Cloud Run** — one serverless service (scales to zero) that serves the REST API and the frontend.
- **Firebase Authentication** — Google sign-in; every ID token is verified server-side.
- **Vertex AI · Gemini** — the review engine, returning structured JSON.
- **Vertex AI embeddings + in-process cosine search** — retrieves the most relevant historical
  rules per submission (the grounding), with no separate vector database to run.
- **Firestore** — per-user review history and the data behind the growth dashboard.

```
Browser ──(Firebase ID token)──> Cloud Run (Spring Boot)
                                    ├── retrieve top historical rules  (embeddings + cosine)
                                    ├── Gemini review  (structured JSON, 1–10 rating)
                                    └── save + read history            (Firestore)
                                  ──> review + rating + growth dashboard
```

## Project layout

```
src/main/java/com/aim/reviewer/
  ReviewerApplication.java      Spring Boot entry point
  AppConfig.java                Beans: Firebase, Firestore, Gemini client, services
  gemini/GeminiClient.java      The ONLY class that calls the Gen AI SDK
  model/                        ReviewRequest, Finding, ReviewResult
  review/Reviewer.java          Prompt + structured-output parsing
  review/RulesStore.java        CSV load, embeddings, cosine retrieval (+ fallback)
  store/HistoryStore.java       Firestore persistence + growth trends
  security/FirebaseAuthFilter   Verifies the ID token on /api routes
  security/RateLimitFilter      Per-IP throttle
  web/ReviewController.java     /api/health, /api/review, /api/history
src/main/resources/
  application.properties        Config, all from env vars
  historical_reviews.csv        Seed rules (schema: id,type,description)
  static/                       The frontend (index.html, styles.css, app.js)
```

## How each requirement is met

| Requirement in the brief | Where it lives |
| --- | --- |
| Authenticated, secure submission | `security/FirebaseAuthFilter.java` (token verification, deny-by-default) |
| Multi-language reviews | `review/Reviewer.java` (Gemini is language-agnostic; language auto-detected) |
| Bug / architecture / optimization insights | `Reviewer` prompt + `model/Finding.java` categories |
| Standardized 1–10 rating | `model/ReviewResult.java` `rating` |
| Persistent per-user history | `store/HistoryStore.java` (Firestore, user-scoped) |
| Growth / patterns over time | `HistoryStore.getTrends()` + the dashboard in `static/app.js` |
| Ingest & learn from the historical CSV | `review/RulesStore.java` + `resources/historical_reviews.csv` |
| Ground reviews in that data | retrieved rules injected into the prompt; matched rules shown as `learned rule #N` badges |

## Setup — 3 things you must fill in

1. **Your Google Cloud project.** Edit the three variables at the top of `deploy.sh`.

2. **Firebase web config.** In `src/main/resources/static/app.js`, replace the `FIREBASE_CONFIG`
   placeholder with your project's values (Firebase console → Project settings → Your apps →
   Web app), and enable **Google** as a sign-in provider in Firebase Authentication.

3. **Model IDs (verify).** `application.properties` defaults to `gemini-3.8-flash` and
   `text-embedding-005`. Confirm these exist in Vertex AI Model Garden for your region, or
   override via the `REVIEW_MODEL` / `EMBED_MODEL` env vars.

## ⚠️ Verify the Gen AI SDK calls

The Google Gen AI **Java** SDK moves quickly, and all of its calls are isolated in one file:
`gemini/GeminiClient.java`. If a method or accessor name differs in your installed version,
that is the only file to adjust — the generation call, the embeddings call, and the two
response accessors (`response.text()`, `embedding.values()`) are all there. Everything else
depends on those two plain methods, not on the SDK. Also confirm the versions in `pom.xml`
(`google-genai`, `firebase-admin`) against Maven Central.

The app degrades gracefully: if embeddings are unavailable, `RulesStore` still grounds reviews
by sending the top rules directly, so grounding works even before you tune the embeddings call.

## Deploy

```bash
bash deploy.sh
```

This enables the required APIs, creates the Firestore database, and builds + deploys the
container to Cloud Run. When it finishes, add the printed Cloud Run URL to **Firebase
Authentication → Settings → Authorized domains**, then open the URL and sign in.

## Run locally

```bash
export GCP_PROJECT=your-project GCP_LOCATION=us-central1 FIREBASE_PROJECT=your-project
gcloud auth application-default login    # gives local code your GCP credentials
mvn spring-boot:run
# open http://localhost:8080  (add localhost to Firebase authorized domains)
```

## Cost efficiency (for the "Mileage Kitna Deti Hai" round)

- Cloud Run **scales to zero** — nothing runs, nothing bills, while idle.
- Defaults to **Gemini 3.8 Flash**, which beats Gemini 3.1 Pro on coding at a fraction of the cost.
- Identical code from the same user is **served from cache** (`code_hash`) with no model call.
- Only the **top few** relevant rules are sent as grounding, not the whole rule set.
- Note: Spring Boot has a slower cold start than a lightweight runtime — set a Cloud Run
  min-instance of 1 if judges will hit it cold, or keep 0 to stay cheapest (review latency
  dominates the request anyway).

## Security decisions (documented)

- **Submitted code is never executed** — it is only ever passed to the model as text. There is
  no `Runtime.exec`, no reflection on input, no dynamic evaluation anywhere.
- **Deny by default** — `/api/review` and `/api/history` require a verified Firebase ID token.
- **User-scoped storage** — reads/writes go through `users/{uid}/reviews`, so no user can reach
  another's data (prevents IDOR).
- **No hardcoded secrets** — all project/config values come from environment variables.
- **Input limits + rate limiting** — oversize submissions are rejected; API routes are throttled.
- **Least stored data** — a SHA-256 hash of the code is stored, not the raw source.
- **Fail secure** — errors return generic messages; details (never tokens or code) stay in logs.
  HTTPS is provided by Cloud Run; Firestore encrypts data at rest.
- **XSS-safe rendering** — the frontend builds DOM nodes with `textContent`, never `innerHTML`.

## Notes

You still deploy this yourself, and you should read through it before the screen test — the
audition includes a 3-minute video and the studio rounds are live, so you'll need to explain
and extend this on camera. The two things worth demoing hard: the **`learned rule #N` badges**
(proof it applied the historical data) and the **growth trend** (proof it tracks improvement).
