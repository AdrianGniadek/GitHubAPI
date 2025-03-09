package com.AdrianGniadek.GithubAPI.controller;

import com.AdrianGniadek.GithubAPI.records.BranchInfo;
import com.AdrianGniadek.GithubAPI.records.RepositoryInfo;
import com.AdrianGniadek.GithubAPI.service.GithubService;
import io.smallrye.mutiny.Uni;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GithubControllerIntegrationTest {

    private MockWebServer mockWebServer;
    private GithubController githubController;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        GithubService githubService = new GithubService(baseUrl);

        githubController = new GithubController(githubService);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getUserRepositories_shouldReturnFilteredRepositoriesWithBranchInfo() throws Exception {
        String username = "testuser";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("[" +
                        "  {" +
                        "    \"name\": \"repo1\"," +
                        "    \"owner\": {" +
                        "      \"login\": \"testuser\"" +
                        "    }," +
                        "    \"fork\": false" +
                        "  }," +
                        "  {" +
                        "    \"name\": \"forked-repo\"," +
                        "    \"owner\": {" +
                        "      \"login\": \"testuser\"" +
                        "    }," +
                        "    \"fork\": true" +
                        "  }" +
                        "]"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("[" +
                        "  {" +
                        "    \"name\": \"main\"," +
                        "    \"commit\": {" +
                        "      \"sha\": \"abc123def456\"" +
                        "    }" +
                        "  }," +
                        "  {" +
                        "    \"name\": \"develop\"," +
                        "    \"commit\": {" +
                        "      \"sha\": \"789xyz\"" +
                        "    }" +
                        "  }" +
                        "]"));

        Uni<List<RepositoryInfo>> repoInfoUni = githubController.getUserRepositories(username);
        List<RepositoryInfo> result = repoInfoUni.await().atMost(Duration.ofSeconds(5));

        assertEquals(1, result.size());

        RepositoryInfo repo = result.get(0);
        assertEquals("repo1", repo.name());
        assertEquals("testuser", repo.ownerLogin());

        List<BranchInfo> branches = repo.branches();
        assertEquals(2, branches.size());

        BranchInfo mainBranch = branches.stream()
                .filter(b -> b.name().equals("main"))
                .findFirst()
                .orElseThrow();
        assertEquals("abc123def456", mainBranch.lastCommitSha());

        BranchInfo devBranch = branches.stream()
                .filter(b -> b.name().equals("develop"))
                .findFirst()
                .orElseThrow();
        assertEquals("789xyz", devBranch.lastCommitSha());
    }
}