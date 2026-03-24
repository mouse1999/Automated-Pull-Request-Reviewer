package com.mouse.model;

import java.util.List;

/**
 * Represents a file modified in a PR, including the specific
 * lines that are eligible for review comments.
 * * @param filename The full path to the file (e.g., "README.md")
 * @param patch    The raw hunk of the diff
 * @param validLines A list of line numbers (in the new file) that
 * were added or modified and are safe to comment on.
 */
public record ChangedFile(
        String filename,
        String patch,
        List<Integer> validLines,
        String language
) {}
