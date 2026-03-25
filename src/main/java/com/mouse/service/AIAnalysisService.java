package com.mouse.service;

import com.mouse.model.ChangedFile;
import com.mouse.model.CodeReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIAnalysisService {

    private final ChatClient chatClient;

    private static final String REVIEW_PROMPT = """
    You are an expert Senior Software Engineer.
    Review the code diff for {language} in file {filename}.
    
    For each issue, you MUST provide:
    - severity: Choose one from [INFO, WARNING, ERROR]
    - category: Choose one from [SECURITY, PERFORMANCE, STYLE, CORRECTNESS]
    
    Diff Content:
    {patch}
    """;

    private static final String SUMMARY_PROMPT = """
        Provide a brief, high-level 2-sentence summary of the changes in these files: {filenames}.
        Mention if you see any major red flags.
        """;

    public AIAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Structured Analysis for a single file.
     */
    public Mono<List<CodeReviewComment>> analyzeFile(ChangedFile file) {

        log.info("Starting AI analysis for file: {} (language: {})",
                file.filename(), file.language());

        long startTime = System.currentTimeMillis();

        return Mono.fromCallable(() ->
                        chatClient.prompt()
                                .user(u -> u.text(REVIEW_PROMPT)
                                        .params(Map.of(
                                                "language", file.language(),
                                                "filename", file.filename(),
                                                "patch", file.patch(),
                                                "validLines", file.validLines()
                                        )))
                                .call()
                                .entity(new ParameterizedTypeReference<List<CodeReviewComment>>() {})
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("AI analysis completed for file: {} | comments: {} | duration: {} ms",
                            file.filename(),
                            result != null ? result.size() : 0,
                            duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("AI analysis failed for file: {} | duration: {} ms",
                            file.filename(), duration, error);
                });
    }

    /**
     * Streaming Summary
     */
    public Flux<String> streamSummary(List<ChangedFile> files) {

        String filenames = files.stream()
                .map(ChangedFile::filename)
                .collect(Collectors.joining(", "));

        log.info("Starting AI summary stream for files: {}", filenames);

        long startTime = System.currentTimeMillis();

        return chatClient.prompt()
                .user(u -> u.text(SUMMARY_PROMPT)
                        .param("filenames", filenames))
                .stream()
                .content()
                .doOnSubscribe(sub ->
                        log.info("AI summary stream subscribed"))
                .doOnNext(chunk ->
                        log.debug("Streaming chunk received"))
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("AI summary stream completed | duration: {} ms", duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("AI summary stream failed | duration: {} ms", duration, error);
                });
    }
}