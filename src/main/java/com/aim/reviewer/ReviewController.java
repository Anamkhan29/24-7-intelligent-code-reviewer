package com.aim.reviewer.web;

import com.aim.reviewer.model.ReviewRequest;
import com.aim.reviewer.model.ReviewResult;
import com.aim.reviewer.review.Reviewer;
import com.aim.reviewer.review.RulesStore;
import com.aim.reviewer.store.HistoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
public class ReviewController {

    private static final Logger log =
            Logger.getLogger(
                    ReviewController.class.getName()
            );

    private final Reviewer reviewer;

    private final RulesStore rulesStore;

    private final HistoryStore historyStore;

    @Value("${app.maxCodeChars:20000}")
    private int maxCodeChars;

    @Value("${app.topKRules:4}")
    private int topKRules;

    public ReviewController(
            Reviewer reviewer,
            RulesStore rulesStore,
            HistoryStore historyStore) {

        this.reviewer = reviewer;
        this.rulesStore = rulesStore;
        this.historyStore = historyStore;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "status",
                "ok"
        );

        response.put(
                "rules_loaded",
                rulesStore.count()
        );

        return response;
    }

    @PostMapping("/api/review")
    public ResponseEntity<?> review(
            @RequestBody ReviewRequest body,
            @RequestAttribute("uid") String uid) {

        String code =
                body.code == null
                        ? ""
                        : body.code.strip();

        if (code.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            detail(
                                    "No code submitted."
                            )
                    );
        }

        if (code.length() > maxCodeChars) {

            return ResponseEntity
                    .status(413)
                    .body(
                            detail(
                                    "Code too long (max "
                                            + maxCodeChars
                                            + " characters)."
                            )
                    );
        }

        String hash =
                sha256(code);

        try {

            /*
             * If the same user submits identical code again,
             * return the stored result instead of calling Gemini.
             */
            Map<String, Object> cached =
                    historyStore.findByHash(
                            uid,
                            hash
                    );

            if (cached != null) {

                cached.put(
                        "cached",
                        true
                );

                return ResponseEntity.ok(
                        cached
                );
            }

            /*
             * Retrieve relevant historical rules.
             */
            List<Map<String, Object>> rules =
                    rulesStore.retrieve(
                            code,
                            topKRules
                    );

            /*
             * Gemini identifies findings.
             *
             * Reviewer then passes those findings through
             * ScoreEngine to calculate the final score.
             */
            ReviewResult result =
                    reviewer.review(
                            code,
                            body.language,
                            rules
                    );

            /*
             * Persist the complete review result.
             */
            String id =
                    historyStore.save(
                            uid,
                            hash,
                            result
                    );

            /*
             * Build the API response.
             */
            Map<String, Object> payload =
                    new LinkedHashMap<>();

            payload.put(
                    "language",
                    result.language
            );

            payload.put(
                    "rating",
                    result.rating
            );

            payload.put(
                    "summary",
                    result.summary
            );

            payload.put(
                    "findings",
                    result.findings
            );

            /*
             * Explainable scoring information.
             */
            payload.put(
                    "score_explanation",
                    result.score_explanation
            );

            payload.put(
                    "critical_count",
                    result.critical_count
            );

            payload.put(
                    "high_count",
                    result.high_count
            );

            payload.put(
                    "medium_count",
                    result.medium_count
            );

            payload.put(
                    "low_count",
                    result.low_count
            );

            payload.put(
                    "info_count",
                    result.info_count
            );

            payload.put(
                    "category_scores",
                    result.category_scores
            );

            payload.put(
                    "id",
                    id
            );

            payload.put(
                    "cached",
                    false
            );

            return ResponseEntity.ok(
                    payload
            );

        } catch (Exception e) {

            /*
             * Detailed exception stays server-side.
             * The user receives only a safe generic message.
             */
            log.severe(
                    "Review generation failed: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .status(502)
                    .body(
                            detail(
                                    "The review engine is temporarily unavailable. Please retry."
                            )
                    );
        }
    }

    @GetMapping("/api/history")
    public ResponseEntity<?> history(
            @RequestAttribute("uid") String uid) {

        try {

            return ResponseEntity.ok(
                    historyStore.getTrends(uid)
            );

        } catch (Exception e) {

            log.severe(
                    "History load failed: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .status(500)
                    .body(
                            detail(
                                    "Could not load history."
                            )
                    );
        }
    }

    private static Map<String, Object> detail(
            String message) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "detail",
                message
        );

        return response;
    }

    /**
     * SHA-256 hash of the submitted source code.
     *
     * Raw source code is not used as the cache key.
     */
    private static String sha256(String s) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    md.digest(
                            s.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder sb =
                    new StringBuilder(
                            hash.length * 2
                    );

            for (byte b : hash) {

                sb.append(
                        Character.forDigit(
                                (b >> 4) & 0xF,
                                16
                        )
                );

                sb.append(
                        Character.forDigit(
                                b & 0xF,
                                16
                        )
                );
            }

            return sb.toString();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}