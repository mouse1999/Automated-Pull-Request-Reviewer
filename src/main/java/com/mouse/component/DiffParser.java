package com.mouse.component;

import com.mouse.model.ChangedFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DiffParser {

    public List<ChangedFile> parseRawDiff(String rawPatch) {
        List<ChangedFile> changedFiles = new ArrayList<>();

        // Split by "diff --git ", skipping the email/date headers at the top
        String[] fileSections = rawPatch.split("diff --git ");

        for (int i = 1; i < fileSections.length; i++) {
            String section = fileSections[i];

            String filename = extractFilename(section);
            String patch = extractPatch(section);
            // New: Detect language based on the filename extension
            String language = detectLanguage(filename);

            if (filename != null && patch != null) {
                List<Integer> validLines = calculateChangedLines(patch);
                // Updated: Passing the language to the ChangedFile constructor
                changedFiles.add(new ChangedFile(filename, patch, validLines, language));
            }
        }
        return changedFiles;
    }

    private String extractFilename(String section) {
        Pattern pattern = Pattern.compile("b/(\\S+)");
        Matcher matcher = pattern.matcher(section);
        return matcher.find() ? matcher.group(1) : "unknown";
    }

    private String extractPatch(String section) {
        int index = section.indexOf("@@");
        return (index != -1) ? section.substring(index) : null;
    }

    private List<Integer> calculateChangedLines(String patch) {
        List<Integer> lines = new ArrayList<>();
        String[] parts = patch.split("\n");
        int currentLineInNewFile = 0;

        for (String line : parts) {
            if (line.startsWith("@@")) {
                Pattern headerPattern = Pattern.compile("\\+(\\d+)");
                Matcher matcher = headerPattern.matcher(line);
                if (matcher.find()) {
                    currentLineInNewFile = Integer.parseInt(matcher.group(1)) - 1;
                }
            } else if (line.startsWith("+")) {
                currentLineInNewFile++;
                lines.add(currentLineInNewFile);
            } else if (line.startsWith("-")) {
                continue;
            } else {
                currentLineInNewFile++;
            }
        }
        return lines;
    }

    private String detectLanguage(String filename) {
        if (filename == null) return "text";
        String lowerFile = filename.toLowerCase();

        if (lowerFile.endsWith(".java")) return "java";
        if (lowerFile.endsWith(".js") || lowerFile.endsWith(".ts")) return "javascript/typescript";
        if (lowerFile.endsWith(".py")) return "python";
        if (lowerFile.endsWith(".go")) return "go";
        if (lowerFile.endsWith(".md")) return "markdown"; // Added for your README changes
        if (lowerFile.endsWith(".xml") || lowerFile.endsWith(".yml") || lowerFile.endsWith(".yaml")) return "config";

        return "text";
    }
}