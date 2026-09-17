package com.aim.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete code review result.
 *
 * The final rating is calculated by ScoreEngine rather than
 * relying solely on Gemini's rating.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReviewResult {

    public String language;

    /**
     * Final deterministic score from 1.0 to 10.0.
     */
    public double rating;

    public String summary;

    public List<com.aim.reviewer.model.Finding> findings = new ArrayList<>();

    /**
     * Explainable score information.
     */
    public String score_explanation;

    public int critical_count;

    public int high_count;

    public int medium_count;

    public int low_count;

    public int info_count;

    public Map<String, Double> category_scores =
            new LinkedHashMap<>();
}