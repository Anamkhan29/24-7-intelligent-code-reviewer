package com.aim.reviewer.model;

/** Incoming review request body. */
public class ReviewRequest {
    public String code;
    public String language; // optional hint; the model auto-detects otherwise
}
