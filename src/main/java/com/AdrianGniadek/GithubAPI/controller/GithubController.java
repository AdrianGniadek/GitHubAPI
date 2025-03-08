package com.AdrianGniadek.GithubAPI.controller;

import com.AdrianGniadek.GithubAPI.records.ErrorResponse;
import com.AdrianGniadek.GithubAPI.records.RepositoryInfo;
import com.AdrianGniadek.GithubAPI.service.GithubService;
import io.smallrye.mutiny.Uni;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class GithubController {
    private final GithubService githubService;

    public GithubController(GithubService githubService) {
        this.githubService = githubService;
    }

    @GetMapping(value = "/repositories/{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Uni<List<RepositoryInfo>> getUserRepositories(@PathVariable String username) {
        return githubService.getUserRepositories(username);
    }

    @ExceptionHandler(GithubService.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(GithubService.UserNotFoundException ex) {
        var errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
