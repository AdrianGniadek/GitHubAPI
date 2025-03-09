package com.AdrianGniadek.GithubAPI.records;

public record BranchInfo(
        String name,
        String lastCommitSha
) {}