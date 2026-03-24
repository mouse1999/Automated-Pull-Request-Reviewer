package com.mouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestEvent(
        String action,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {}

