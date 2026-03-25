package com.mouse.service;

import com.mouse.model.CodeReviewComment;
import com.mouse.model.PullRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubApiService {

    private final WebClient.Builder webClientBuilder;

    @Value("${github.api.token}")
    private String githubToken;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        log.info("Initializing GitHub WebClient");

        this.webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "*/*")
                .build();

        log.info("GitHub WebClient initialized successfully");
    }

    public Mono<String> getDiff(PullRequestEvent event) {
        // 1. Get the patch URL from the metadata you shared
        String patchUrl = event.pullRequest().patchUrl();

        return webClient.get()
                .uri(patchUrl)
                // 2. This header tells GitHub to return the RAW DIFF text, not JSON
                .header("Accept", "*/*")
                .retrieve()
                .bodyToMono(String.class) // This is the actual "rawDiff"
                .doOnSuccess(diff -> log.info("Successfully fetched diff for PR: {}, diff payload: {}", event.pullRequest().number(), diff))
                .doOnError(error -> log.error("Error fetching diff for PR: {}", event.pullRequest().number(), error));
    }

    public Mono<Void> postReview(String owner, String repo, int prNumber, List<CodeReviewComment> comments) {

        log.info("Posting review for PR: {} with {} comments", prNumber, comments.size());

        var githubComments = comments.stream().map(c -> Map.of(
                "path", c.filename(),
                "line", c.lineNumber(),
                "body", String.format("**[%s] %s**\n%s\n\n*Suggestion:* `%s`",
                        c.severity(), c.category(), c.message(), c.suggestion())
        )).toList();

        var body = Map.of(
                "event", "COMMENT",
                "comments", githubComments
        );

        return webClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews", owner, repo, prNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Review posted successfully for PR: {}", prNumber))
                .doOnError(error -> log.error("Error posting review for PR: {}", prNumber, error))
                .then();
    }
}