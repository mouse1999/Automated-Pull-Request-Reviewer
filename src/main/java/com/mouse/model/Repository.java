package com.mouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Repository(
        String name,              // e.g., "nexus-agentic-commerce"
        Owner owner               // <--- This is where the owner field lives
) {}
