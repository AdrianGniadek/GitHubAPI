package com.AdrianGniadek.GithubAPI.records;

public record ErrorResponse(
        int status,
        String message
) {}
