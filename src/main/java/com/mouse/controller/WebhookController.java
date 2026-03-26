package com.mouse.controller;


import com.mouse.component.DiffParser;
import com.mouse.model.ChangedFile;
import com.mouse.model.PullRequestEvent;
import com.mouse.service.AIAnalysisService;
import com.mouse.service.GithubApiService;
import com.mouse.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final GithubApiService githubApiService;
    private final AIAnalysisService aiService;
    private final DiffParser diffParser;


    @PostMapping("/github")
    public Mono<ResponseEntity<String>> handleGithubEvent(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {

        // 1. Security: Immediate HMAC Validation
        if (!webhookService.isValidSignature(payload, signature)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Signature"));
        }

        log.info("payload from github: {}", payload);

        // 2. Parse JSON to PullRequestEvent Record
        PullRequestEvent event = webhookService.parseEvent(payload);
        log.info("serialized pull request: {}", event);

        // 3. Filter for relevant PR actions (Opened or Synchronized/Updated)
        if (!isActionProcessable(event.action())) {
            return Mono.just(ResponseEntity.ok("Action ignored: " + event.action()));
        }

        log.info("Action not ignored");

        // 4. Reactive Orchestration Pipeline
        return githubApiService.getDiff(event)

                .map(diffParser::parseRawDiff) // Convert raw text to List<ChangedFile>
                .flatMap(files -> runConcurrentReview(event, files))
                .thenReturn(ResponseEntity.ok("AI Review initiated for PR #" + event.pullRequest().number()))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.internalServerError().body("Review failed: " + e.getMessage())
                ));
    }

    private Mono<Void> runConcurrentReview(PullRequestEvent event, List<ChangedFile> files) {

        log.info("Preparing to review {} files by AI", files.size());

        return Flux.fromIterable(files)
                .flatMap(file -> aiService.analyzeFile(file)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnSubscribe(sub ->
                                log.info("Analyzing file: {}", file.filename()))
                        .doOnSuccess(comments ->
                                log.info("AI result for file: {} -> {} comments",
                                        file.filename(),
                                        comments != null ? comments.size() : 0))
                        .doOnError(error ->
                                log.error("AI analysis failed for file: {}", file.filename(), error))
                )
                .collectList()

                .map(allComments -> {
                    log.info("Collected AI results for all files. Total file batches: {}", allComments.size());

                    var flat = allComments.stream()
                            .flatMap(Collection::stream)
                            .toList();

                    // 🔥 THIS IS WHAT YOU WANT (SEE AI OUTPUT)
                    log.info("Total AI comments generated: {}", flat.size());

                    flat.forEach(comment ->
                            log.info("AI Comment -> file: {}, line: {}, severity: {}, message: {}",
                                    comment.filename(),
                                    comment.lineNumber(),
                                    comment.severity(),
                                    comment.message())
                    );

                    return flat;
                })

                .flatMap(flatComments -> {
                    log.info("Sending {} comments to GitHub PR: {}",
                            flatComments.size(),
                            event.pullRequest().number());

                    return githubApiService.postReview(
                            event.repository().owner().login(),
                            event.repository().name(),
                            event.pullRequest().number(),
                            flatComments
                    );
                })

                .doOnSuccess(v ->
                        log.info("Review process completed successfully for PR: {}",
                                event.pullRequest().number()))

                .doOnError(error ->
                        log.error("Review process failed for PR: {}",
                                event.pullRequest().number(), error));
    }

    private boolean isActionProcessable(String action) {
        return "opened".equalsIgnoreCase(action) || "synchronize".equalsIgnoreCase(action);
    }
}
