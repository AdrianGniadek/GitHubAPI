package com.AdrianGniadek.GithubAPI.records;

import java.util.List;

public record RepositoryInfo(
        String name,
        String ownerLogin,
        List<BranchInfo> branches
) {}