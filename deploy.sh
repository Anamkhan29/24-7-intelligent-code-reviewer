#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# EDIT THESE THREE LINES, then run:  bash deploy.sh
# ============================================================
PROJECT_ID="your-gcp-project-id"     # your Google Cloud / Code Kitchen sandbox project
REGION="us-central1"                 # a Vertex AI-supported region
SERVICE="code-reviewer"
# ============================================================

gcloud config set project "$PROJECT_ID"

# 1. Enable the Google Cloud APIs this app uses.
gcloud services enable \
  run.googleapis.com \
  aiplatform.googleapis.com \
  firestore.googleapis.com \
  cloudbuild.googleapis.com \
  identitytoolkit.googleapis.com

# 2. Create the Firestore database in Native mode (safe to skip if it already exists).
gcloud firestore databases create --location="$REGION" 2>/dev/null || \
  echo "Firestore database already exists (or creation skipped) — continuing."

# 3. Build the container from the Dockerfile and deploy to Cloud Run.
#    --allow-unauthenticated makes the SITE reachable; the app still requires a valid
#    Firebase login on every /api route.
gcloud run deploy "$SERVICE" \
  --source . \
  --region "$REGION" \
  --allow-unauthenticated \
  --memory 512Mi \
  --set-env-vars "GCP_PROJECT=${PROJECT_ID},GCP_LOCATION=${REGION},FIREBASE_PROJECT=${PROJECT_ID}"

echo ""
echo "Done. Open the Service URL printed above, and add that URL to"
echo "Firebase Authentication > Settings > Authorized domains."
