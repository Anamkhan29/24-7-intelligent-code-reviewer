package com.aim.reviewer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A single review finding. Public fields (auto-mapped by Jackson) keep this concise. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Finding {
    public Integer line;         // 1-based line number, or null
    public String category;      // bug | security | performance | architecture | optimization | formatting | style
    public String severity;      // critical | high | medium | low | info
    public String title;
    public String detail;
    public String suggestion;

    @JsonProperty("grounded_rule_id")
    public Integer groundedRuleId; // id of the matched historical rule, or null
}
