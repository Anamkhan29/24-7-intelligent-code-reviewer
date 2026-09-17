package com.aim.reviewer;

import com.aim.reviewer.gemini.GeminiClient;
import com.aim.reviewer.review.Reviewer;
import com.aim.reviewer.review.RulesStore;
import com.aim.reviewer.review.ScoreEngine;
import com.aim.reviewer.store.HistoryStore;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Value("${gcp.project:}")
    private String project;

    @Value("${gcp.location:us-central1}")
    private String location;

    @Value("${firebase.project:}")
    private String firebaseProject;

    @Value("${review.model:gemini-3.5-flash-lite}")
    private String reviewModel;

    @Value("${embed.model:text-embedding-005}")
    private String embedModel;

    @Value("${app.topKRules:4}")
    private int topKRules;

    @Value("${app.rulesCsvPath:}")
    private String rulesCsvPath;

    /**
     * Firebase project is preferred for Firestore/Auth.
     * Falls back to the GCP project when Firebase project is not specified.
     */
    private String effectiveProject() {

        return (
                firebaseProject != null
                        && !firebaseProject.isBlank()
        )
                ? firebaseProject
                : project;
    }

    /**
     * Initialize Firebase Admin SDK.
     */
    @Bean
    public FirebaseApp firebaseApp()
            throws IOException {

        /*
         * Reuse existing Firebase initialization if one already exists.
         */
        if (!FirebaseApp.getApps().isEmpty()) {

            return FirebaseApp.getInstance();
        }

        FirebaseOptions.Builder builder =
                FirebaseOptions.builder()
                        .setCredentials(
                                GoogleCredentials
                                        .getApplicationDefault()
                        );

        String proj =
                effectiveProject();

        if (proj != null && !proj.isBlank()) {

            builder.setProjectId(proj);
        }

        return FirebaseApp.initializeApp(
                builder.build()
        );
    }

    /**
     * Firestore Admin client.
     */
    @Bean
    public Firestore firestore(
            FirebaseApp app
    ) {

        return FirestoreClient.getFirestore(
                app
        );
    }

    /**
     * Gemini Developer API client.
     *
     * IMPORTANT:
     * GEMINI_API_KEY is read from the environment.
     * It must never be placed in source code,
     * application.properties, or frontend JavaScript.
     */
    @Bean
    public GeminiClient geminiClient() {

        String apiKey =
                System.getenv(
                        "GEMINI_API_KEY"
                );

        if (
                apiKey == null
                        || apiKey.isBlank()
        ) {

            throw new IllegalStateException(
                    "GEMINI_API_KEY environment variable is not set"
            );
        }

        Client client =
                Client.builder()
                        .apiKey(apiKey)
                        .build();

        return new GeminiClient(
                client
        );
    }

    /**
     * Historical review rule store.
     */
    @Bean
    public RulesStore rulesStore(
            GeminiClient gemini
    ) {

        RulesStore store =
                new RulesStore(
                        gemini,
                        embedModel
                );

        store.load(
                rulesCsvPath
        );

        return store;
    }

    /**
     * Gemini review engine.
     */
    @Bean
    public Reviewer reviewer(
            GeminiClient gemini
    ) {

        return new Reviewer(
                gemini,
                reviewModel
        );
    }

    /**
     * Deterministic scoring engine.
     */
    @Bean
    public ScoreEngine scoreEngine() {

        return new ScoreEngine();
    }

    /**
     * Persistent review history.
     */
    @Bean
    public HistoryStore historyStore(
            Firestore firestore
    ) {

        return new HistoryStore(
                firestore
        );
    }
}