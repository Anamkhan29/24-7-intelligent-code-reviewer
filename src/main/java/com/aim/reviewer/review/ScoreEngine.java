package com.aim.reviewer.review;

import com.aim.reviewer.model.Finding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic scoring engine for code reviews.
 *
 * Gemini identifies the problems.
 * ScoreEngine determines the final 1-10 score.
 *
 * This makes the final rating transparent and repeatable instead
 * of relying entirely on the model's subjective rating.
 */
public class ScoreEngine {

    /**
     * Calculate a deterministic score from the findings.
     *
     * Score starts at 10 and findings reduce it according to severity.
     */
    public ScoreResult calculate(List<Finding> findings) {

        if (findings == null || findings.isEmpty()) {
            return new ScoreResult(
                    10.0,
                    defaultCategoryScores(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    "No issues were identified."
            );
        }

        double penalty = 0.0;

        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        int info = 0;

        Map<String, Double> categoryPenalty = new LinkedHashMap<>();

        for (Finding finding : findings) {

            if (finding == null) {
                continue;
            }

            String severity = normalize(finding.severity);
            String category = normalize(finding.category);

            double severityPenalty = severityPenalty(severity);

            penalty += severityPenalty;

            switch (severity) {
                case "critical" -> critical++;
                case "high" -> high++;
                case "medium" -> medium++;
                case "low" -> low++;
                case "info" -> info++;
                default -> {
                    // Unknown severity receives a small penalty.
                }
            }

            if (!category.isBlank()) {
                categoryPenalty.merge(
                        category,
                        severityPenalty,
                        Double::sum
                );
            }
        }

        /*
         * Additional penalty when there are many findings.
         * This prevents a codebase with many issues from receiving
         * the same score as one with only a single issue.
         */
        int totalFindings = findings.size();

        if (totalFindings > 5) {
            penalty += Math.min(
                    1.5,
                    (totalFindings - 5) * 0.15
            );
        }

        double score = 10.0 - penalty;

        // Product requirement: score must always be between 1 and 10.
        score = Math.max(1.0, Math.min(10.0, score));

        // Round to one decimal place.
        score = Math.round(score * 10.0) / 10.0;

        Map<String, Double> categoryScores =
                calculateCategoryScores(categoryPenalty);

        String explanation = buildExplanation(
                critical,
                high,
                medium,
                low,
                info,
                totalFindings
        );

        return new ScoreResult(
                score,
                categoryScores,
                critical,
                high,
                medium,
                low,
                info,
                explanation
        );
    }

    /**
     * Severity impact on the overall score.
     *
     * Critical issues have the largest impact.
     */
    private double severityPenalty(String severity) {

        return switch (severity) {

            case "critical" -> 3.0;

            case "high" -> 1.8;

            case "medium" -> 0.8;

            case "low" -> 0.25;

            case "info" -> 0.0;

            default -> 0.3;
        };
    }

    /**
     * Calculate individual category scores.
     */
    private Map<String, Double> calculateCategoryScores(
            Map<String, Double> categoryPenalty) {

        Map<String, Double> scores = defaultCategoryScores();

        for (Map.Entry<String, Double> entry : categoryPenalty.entrySet()) {

            String category = entry.getKey();
            double penalty = entry.getValue();

            double categoryScore = 10.0 - penalty;

            categoryScore = Math.max(
                    1.0,
                    Math.min(10.0, categoryScore)
            );

            categoryScore =
                    Math.round(categoryScore * 10.0) / 10.0;

            scores.put(category, categoryScore);
        }

        return scores;
    }

    /**
     * Categories available to the UI.
     */
    private Map<String, Double> defaultCategoryScores() {

        Map<String, Double> scores = new LinkedHashMap<>();

        scores.put("security", 10.0);
        scores.put("bug", 10.0);
        scores.put("performance", 10.0);
        scores.put("architecture", 10.0);
        scores.put("optimization", 10.0);
        scores.put("formatting", 10.0);
        scores.put("style", 10.0);

        return scores;
    }

    /**
     * Human-readable explanation of why the score changed.
     */
    private String buildExplanation(
            int critical,
            int high,
            int medium,
            int low,
            int info,
            int total) {

        if (critical > 0) {
            return "The score is primarily reduced by "
                    + critical
                    + " critical issue"
                    + (critical == 1 ? "" : "s")
                    + ".";
        }

        if (high > 0) {
            return "The score is primarily reduced by "
                    + high
                    + " high-severity issue"
                    + (high == 1 ? "" : "s")
                    + ".";
        }

        if (medium > 0) {
            return "The code has "
                    + medium
                    + " medium-severity issue"
                    + (medium == 1 ? "" : "s")
                    + " affecting the overall score.";
        }

        if (low > 0) {
            return "Only low-severity improvements were identified.";
        }

        if (info > 0) {
            return "The review identified informational observations only.";
        }

        if (total == 0) {
            return "No issues were identified.";
        }

        return "The review identified code quality observations.";
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }

    /**
     * Result produced by the scoring engine.
     */
    public static class ScoreResult {

        public final double overallScore;

        public final Map<String, Double> categoryScores;

        public final int criticalCount;
        public final int highCount;
        public final int mediumCount;
        public final int lowCount;
        public final int infoCount;

        public final String explanation;

        public ScoreResult(
                double overallScore,
                Map<String, Double> categoryScores,
                int criticalCount,
                int highCount,
                int mediumCount,
                int lowCount,
                int infoCount,
                String explanation) {

            this.overallScore = overallScore;
            this.categoryScores = categoryScores;

            this.criticalCount = criticalCount;
            this.highCount = highCount;
            this.mediumCount = mediumCount;
            this.lowCount = lowCount;
            this.infoCount = infoCount;

            this.explanation = explanation;
        }
    }
}