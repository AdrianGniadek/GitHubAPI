package com.AdrianGniadek.GithubAPI.service;

import com.AdrianGniadek.GithubAPI.records.*;
import io.smallrye.mutiny.Uni;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;

@Service
public class GithubService {
    private final WebClient webClient;

    public GithubService(@Value("${github.api.url:https://api.github.com}") String apiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    public Uni<List<RepositoryInfo>> getUserRepositories(String username) {
        Flux<Repository> repositoriesFlux = webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .bodyToFlux(Repository.class);

        Mono<List<Repository>> nonForkReposMono = repositoriesFlux
                .filter(repo -> !repo.fork())
                .collectList();

        Mono<List<RepositoryInfo>> resultMono = nonForkReposMono.flatMap(repos -> {
            // If empty list, just return it
            if (repos.isEmpty()) {
                return Mono.just(List.of());
            }

            List<Mono<RepositoryInfo>> repoInfoMonos = repos.stream()
                    .map(repo -> {
                        String repoName = repo.name();
                        String ownerLogin = repo.owner().login();

                        return webClient.get()
                                .uri("/repos/{username}/{repo}/branches", username, repoName)
                                .retrieve()
                                .bodyToFlux(Branch.class)
                                .collectList()
                                .map(branches -> {
                                    List<BranchInfo> branchInfos = branches.stream()
                                            .map(branch -> new BranchInfo(branch.name(), branch.commit().sha()))
                                            .collect(Collectors.toList());

                                    return new RepositoryInfo(repoName, ownerLogin, branchInfos);
                                });
                    })
                    .collect(Collectors.toList());

            return Mono.zip(repoInfoMonos, results -> {
                List<RepositoryInfo> resultList = new java.util.ArrayList<>(results.length);
                for (Object result : results) {
                    resultList.add((RepositoryInfo) result);
                }
                return resultList;
            });
        });

        Mono<List<RepositoryInfo>> errorHandledMono = resultMono
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    throw new UserNotFoundException("User not found: " + username);
                });

        return Uni.createFrom().converter(UniReactorConverters.fromMono(), errorHandledMono);
    }

    public static sealed class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }

    public static final class UserNotFoundException extends ApiException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}