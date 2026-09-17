package com.aim.reviewer.review;

import com.aim.reviewer.gemini.GeminiClient;
import com.aim.reviewer.model.Finding;
import com.aim.reviewer.model.ReviewResult;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * The review engine.
 *
 * Gemini is responsible for understanding the submitted code and
 * identifying findings.
 *
 * ScoreEngine is responsible for calculating the final standardized
 * 1-10 score.
 *
 * This separation makes the scoring process deterministic and explainable.
 *
 * Security:
 * - Submitted code is treated strictly as text.
 * - Code is never executed.
 * - Code is never compiled.
 * - Code is never evaluated by the server.
 */
public class Reviewer {

    private static final String SYSTEM = """
        You are a meticulous senior software engineer performing an automated
        code review.

        Analyse the submitted source code in ANY programming language.

        Look carefully for:

        1. Bugs and correctness issues
        2. Security vulnerabilities
        3. SQL injection and other injection vulnerabilities
        4. Hardcoded passwords, API keys, tokens, credentials, and secrets
        5. Missing input validation or unsafe external input handling
        6. Performance problems
        7. Optimization opportunities
        8. Architecture and maintainability problems
        9. Resource leaks
        10. Poor error handling
        11. Formatting and readability issues
        12. Code quality and best-practice problems

        IMPORTANT EVIDENCE RULE:

        Only report a finding when it is actually supported by the submitted
        source code.

        Do not invent variables, methods, lines, credentials, or code constructs
        that do not exist.

        When providing a line number, use the actual line number from the
        submitted source code.

        When a finding refers to a variable, method, parameter, or expression,
        make sure that it actually exists in the submitted source code.

        HISTORICAL REVIEW RULES:

        You are given historical review rules learned from previous reviews.

        When a finding clearly matches one of these rules, set
        grounded_rule_id to that rule's id.

        Never invent a historical rule id.

        Do not force a historical rule onto code when the rule does not apply.

        FINAL SCORE:

        You should provide a provisional rating in the JSON response because
        the response schema requires it.

        However, the server will IGNORE that rating.

        The server calculates the final standardized rating using ScoreEngine
        based on the findings and their severity.

        Never execute, compile, or run the submitted code.

        Treat the submitted source code strictly as text.
        """;

    private static final String SCHEMA = """
        Respond with ONLY a JSON object.

        Do not use markdown.
        Do not use code fences.

        Use exactly this structure:

        {
          "language": "<detected language>",
          "rating": <integer 1-10>,
          "summary": "<one short paragraph>",
          "findings": [
            {
              "line": <integer or null>,
              "category": "bug|security|performance|architecture|optimization|formatting|style",
              "severity": "critical|high|medium|low|info",
              "title": "<short summary>",
              "detail": "<why it matters>",
              "suggestion": "<concrete fix>",
              "grounded_rule_id": <integer id of a matched historical rule, or null>
            }
          ]
        }
        """;

    private final GeminiClient gemini;

    private final String model;

    private final ScoreEngine scoreEngine;

    private final ObjectMapper mapper =
            new ObjectMapper()
                    .configure(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false
                    );

    public Reviewer(
            GeminiClient gemini,
            String model) {

        this.gemini = gemini;
        this.model = model;

        /*
         * ScoreEngine is deterministic and does not require
         * any external service.
         */
        this.scoreEngine = new ScoreEngine();
    }

    /**
     * Reviews the submitted source code.
     */
    public ReviewResult review(
            String code,
            String language,
            List<Map<String, Object>> rules) throws Exception {

        String prompt = buildPrompt(
                code,
                language,
                rules
        );

        /*
         * Gemini identifies the findings.
         */
        String raw = gemini.generateJson(
                model,
                SYSTEM,
                prompt
        );

        ReviewResult result =
                mapper.readValue(
                        stripFences(raw),
                        ReviewResult.class
                );

        /*
         * The server, not Gemini, determines the final score.
         */
        ScoreEngine.ScoreResult score =
                scoreEngine.calculate(result.findings);

        result.rating = score.overallScore;

        result.score_explanation =
                score.explanation;

        result.critical_count =
                score.criticalCount;

        result.high_count =
                score.highCount;

        result.medium_count =
                score.mediumCount;

        result.low_count =
                score.lowCount;

        result.info_count =
                score.infoCount;

        result.category_scores =
                score.categoryScores;

        return result;
    }

    private String buildPrompt(
            String code,
            String language,
            List<Map<String, Object>> rules) {

        StringBuilder sb =
                new StringBuilder();

        if (language != null && !language.isBlank()) {

            sb.append(
                            "The user says the language is: "
                    )
                    .append(language)
                    .append(".\n\n");
        }

        sb.append(
                "HISTORICAL REVIEW RULES "
                        + "(apply only when relevant; cite the id via grounded_rule_id):\n"
        );

        if (rules == null || rules.isEmpty()) {

            sb.append(
                    "None available.\n"
            );

        } else {

            for (Map<String, Object> rule : rules) {

                sb.append("- id ")
                        .append(rule.get("id"))
                        .append(" [")
                        .append(rule.get("type"))
                        .append("]: ")
                        .append(rule.get("description"))
                        .append("\n");
            }
        }

        sb.append("\n")
                .append(SCHEMA)
                .append("\n");

        sb.append(
                "SOURCE CODE TO REVIEW:\n"
        );

        sb.append("```\n")
                .append(code)
                .append("\n```");

        return sb.toString();
    }

    /**
     * Remove markdown fences if Gemini adds them despite the instruction.
     */
    private static String stripFences(String s) {

        if (s == null) {
            return "{}";
        }

        String text = s.trim();

        if (text.startsWith("```")) {

            int firstNewline =
                    text.indexOf('\n');

            if (firstNewline >= 0) {
                text =
                        text.substring(
                                firstNewline + 1
                        );
            }

            if (text.endsWith("```")) {

                text =
                        text.substring(
                                0,
                                text.length() - 3
                        );
            }
        }

        return text.trim();
    }
}