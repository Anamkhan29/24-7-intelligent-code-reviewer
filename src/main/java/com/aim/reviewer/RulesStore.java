package com.aim.reviewer.review;

import com.aim.reviewer.gemini.GeminiClient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Historical learning. Loads review rules (schema: id,type,description), embeds them once
 * with Vertex AI, and at review time retrieves the most relevant rules for the submitted
 * code via cosine similarity — kept in-process, no separate vector DB (cheap and simple).
 *
 * If embeddings are unavailable for any reason, it degrades gracefully to sending the first
 * K rules so grounding still works.
 */
public class RulesStore {

    private static final Logger log = Logger.getLogger(RulesStore.class.getName());
    private static final String CLASSPATH_CSV = "/historical_reviews.csv";

    private final GeminiClient gemini;
    private final String embedModel;
    private final List<Map<String, Object>> rules = new ArrayList<>();
    private final List<float[]> vectors = new ArrayList<>();

    public RulesStore(GeminiClient gemini, String embedModel) {
        this.gemini = gemini;
        this.embedModel = embedModel;
    }

    public int count() {
        return rules.size();
    }

    public void load(String csvPath) {
        for (String[] row : readRows(csvPath)) {
            try {
                int id = Integer.parseInt(row[0].trim());
                String type = row.length > 1 ? row[1].trim() : "";
                String desc = row.length > 2 ? row[2].trim() : "";
                if (desc.isEmpty()) continue;
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("id", id);
                rule.put("type", type);
                rule.put("description", desc);
                rules.add(rule);
            } catch (NumberFormatException ignore) {
                // skip malformed rows (e.g. the header)
            }
        }

        if (!rules.isEmpty()) {
            try {
                List<String> texts = new ArrayList<>();
                for (Map<String, Object> r : rules) {
                    texts.add(r.get("type") + ": " + r.get("description"));
                }
                vectors.addAll(gemini.embed(embedModel, texts));
            } catch (Exception e) {
                log.warning("Embedding rules failed — falling back to non-semantic grounding: " + e.getMessage());
                vectors.clear();
            }
        }
        log.info("Loaded " + rules.size() + " historical rules");
    }

    /** Return the top-K most relevant rules for this code. */
    public List<Map<String, Object>> retrieve(String code, int topK) {
        if (rules.isEmpty()) return List.of();
        if (vectors.size() != rules.size()) return capped(topK); // embeddings unavailable

        float[] query;
        try {
            String snippet = code.length() > 4000 ? code.substring(0, 4000) : code;
            query = gemini.embed(embedModel, List.of(snippet)).get(0);
        } catch (Exception e) {
            log.warning("Query embedding failed — using non-semantic grounding: " + e.getMessage());
            return capped(topK);
        }

        double[] scores = new double[rules.size()];
        Integer[] order = new Integer[rules.size()];
        for (int i = 0; i < rules.size(); i++) {
            scores[i] = cosine(query, vectors.get(i));
            order[i] = i;
        }
        java.util.Arrays.sort(order, (a, b) -> Double.compare(scores[b], scores[a]));

        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, order.length); i++) {
            out.add(rules.get(order[i]));
        }
        return out;
    }

    private List<Map<String, Object>> capped(int topK) {
        return rules.subList(0, Math.min(topK, rules.size()));
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /** Read CSV rows as [id, type, description]. Splits on the first two commas only, so
     *  a description may itself contain commas. Reads an external file if the path is set and
     *  exists, otherwise the bundled classpath rules. */
    private List<String[]> readRows(String csvPath) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = openReader(csvPath)) {
            if (reader == null) {
                log.warning("No rules CSV found — running without grounding");
                return rows;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(line.split(",", 3));
            }
        } catch (Exception e) {
            log.warning("Failed to read rules CSV: " + e.getMessage());
        }
        return rows;
    }

    private BufferedReader openReader(String csvPath) throws Exception {
        if (csvPath != null && !csvPath.isBlank() && Files.exists(Path.of(csvPath))) {
            return Files.newBufferedReader(Path.of(csvPath), StandardCharsets.UTF_8);
        }
        InputStream in = getClass().getResourceAsStream(CLASSPATH_CSV);
        return in == null ? null : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}
