package com.AdrianGniadek.GithubAPI.service;

import com.AdrianGniadek.GithubAPI.records.Repository;
import com.AdrianGniadek.GithubAPI.records.RepositoryInfo;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GithubService {
    private final WebClient webClient;

    public GithubService(@Value("${github.api.url:https://api.github.com}") String apiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    public Uni<List<RepositoryInfo>> getUserRepositories(String username)  {
        Mono<List<RepositoryInfo>> mono = getRepositoriesForUser(username)
                .filter(repo -> !repo.fork())
                .map(repo -> new RepositoryInfo(repo.name(), repo.owner().login()))
                .collectList()
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    throw new UserNotFoundException("User not found");
                });

        return Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
    }
    private Flux<Repository> getRepositoriesForUser(String username) {
        return webClient.get()
                .uri("/users/{username}/repos", username)
                .retrieve()
                .bodyToFlux(Repository.class);
    }

    // Using sealed class pattern for better error handling
    public static sealed class ApiException extends RuntimeException permits UserNotFoundException {
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
