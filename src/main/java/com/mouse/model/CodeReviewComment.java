package com.mouse.model;

import com.mouse.enums.Category;
import com.mouse.enums.Severity;

public record CodeReviewComment(
        String filename,
        int lineNumber,
        Severity severity,
        Category category,
        String message,
        String suggestion) {

}
