package com.aim.reviewer.store;

import com.aim.reviewer.model.Finding;
import com.aim.reviewer.model.ReviewResult;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Persistence and growth tracking on Firestore.
 *
 * Every review is stored under the authenticated user's own subcollection:
 *
 * users/{uid}/reviews
 *
 * This ensures that reads and writes are scoped to the authenticated user.
 * We store a SHA-256 hash of the code instead of the raw source code.
 */
public class HistoryStore {

    private final Firestore db;

    public HistoryStore(Firestore db) {
        this.db = db;
    }

    private CollectionReference reviews(String uid) {
        return db.collection("users")
                .document(uid)
                .collection("reviews");
    }

    /**
     * Save a review for the authenticated user.
     */
    public String save(String uid, String codeHash, ReviewResult r) {
        String id = UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", id);
        doc.put("created_at", Instant.now().toString());
        doc.put("code_hash", codeHash);
        doc.put("language", r.language);
        doc.put("rating", r.rating);
        doc.put("summary", r.summary);
        doc.put("findings", findingsToMaps(r.findings));
        doc.put("findings_count", r.findings.size());

        try {
            reviews(uid)
                    .document(id)
                    .set(doc)
                    .get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Failed to save review: request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Failed to save review: Firestore error - " + getCauseMessage(e),
                    e
            );
        }

        return id;
    }

    /**
     * Return a previously stored review for identical code, or null.
     */
    public Map<String, Object> findByHash(String uid, String codeHash) {
        try {
            QuerySnapshot snap = reviews(uid)
                    .whereEqualTo("code_hash", codeHash)
                    .limit(1)
                    .get()
                    .get();

            for (QueryDocumentSnapshot d : snap.getDocuments()) {
                return new LinkedHashMap<>(d.getData());
            }

            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Failed to look up review: request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Failed to look up review: Firestore error - " + getCauseMessage(e),
                    e
            );
        }
    }

    /**
     * Aggregate this user's history into the growth-dashboard payload.
     */
    public Map<String, Object> getTrends(String uid) {
        List<QueryDocumentSnapshot> docs;

        try {
            docs = reviews(uid)
                    .orderBy("created_at", Query.Direction.ASCENDING)
                    .get()
                    .get()
                    .getDocuments();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Failed to load history: request interrupted", e
            );

        } catch (ExecutionException e) {
            throw new RuntimeException(
                    "Failed to load history: Firestore error - " + getCauseMessage(e),
                    e
            );
        }

        List<Map<String, Object>> ratingsOverTime = new ArrayList<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        long ratingSum = 0;

        for (QueryDocumentSnapshot d : docs) {

            int rating = asInt(d.get("rating"));
            ratingSum += rating;

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("created_at", d.getString("created_at"));
            point.put("rating", rating);

            ratingsOverTime.add(point);

            Object findings = d.get("findings");

            if (findings instanceof List<?> list) {

                for (Object f : list) {

                    if (f instanceof Map<?, ?> fm) {

                        Object cat = fm.get("category");

                        String key = cat == null
                                ? "other"
                                : cat.toString();

                        categoryCounts.merge(
                                key,
                                1,
                                Integer::sum
                        );
                    }
                }
            }
        }

        double average = docs.isEmpty()
                ? 0.0
                : Math.round(
                (ratingSum * 100.0) / docs.size()
        ) / 100.0;

        List<Map<String, Object>> topCategories =
                categoryCounts.entrySet()
                        .stream()
                        .sorted(
                                (a, b) ->
                                        Integer.compare(
                                                b.getValue(),
                                                a.getValue()
                                        )
                        )
                        .limit(6)
                        .map(e -> {

                            Map<String, Object> m =
                                    new LinkedHashMap<>();

                            m.put("category", e.getKey());
                            m.put("count", e.getValue());

                            return m;
                        })
                        .toList();

        /*
         * History newest-first.
         */
        List<Map<String, Object>> history =
                new ArrayList<>();

        for (int i = docs.size() - 1; i >= 0; i--) {

            QueryDocumentSnapshot d = docs.get(i);

            Map<String, Object> h =
                    new LinkedHashMap<>();

            h.put("id", d.getString("id"));
            h.put("created_at", d.getString("created_at"));
            h.put("language", d.getString("language"));
            h.put("rating", asInt(d.get("rating")));
            h.put("summary", d.getString("summary"));
            h.put(
                    "findings_count",
                    asInt(d.get("findings_count"))
            );

            history.add(h);
        }

        Map<String, Object> out =
                new LinkedHashMap<>();

        out.put("count", docs.size());
        out.put("average_rating", average);
        out.put("ratings_over_time", ratingsOverTime);
        out.put("top_categories", topCategories);
        out.put("history", history);

        return out;
    }

    /**
     * Convert Finding objects into Firestore-compatible maps.
     */
    private static List<Map<String, Object>> findingsToMaps(
            List<Finding> findings) {

        List<Map<String, Object>> out =
                new ArrayList<>();

        for (Finding f : findings) {

            Map<String, Object> m =
                    new LinkedHashMap<>();

            m.put("line", f.line);
            m.put("category", f.category);
            m.put("severity", f.severity);
            m.put("title", f.title);
            m.put("detail", f.detail);
            m.put("suggestion", f.suggestion);
            m.put("grounded_rule_id", f.groundedRuleId);

            out.add(m);
        }

        return out;
    }

    /**
     * Extract the most useful message from a Firestore exception.
     */
    private static String getCauseMessage(ExecutionException e) {

        Throwable cause = e.getCause();

        if (cause == null) {
            return e.getMessage();
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return cause.toString();
        }

        return message;
    }

    private static int asInt(Object o) {
        return (o instanceof Number n)
                ? n.intValue()
                : 0;
    }
}