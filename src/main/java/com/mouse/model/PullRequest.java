package com.mouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequest(
        int number,
        String title,
        String body,
        @JsonProperty("head") Head head,
        @JsonProperty("patch_url") String patchUrl
) {}
