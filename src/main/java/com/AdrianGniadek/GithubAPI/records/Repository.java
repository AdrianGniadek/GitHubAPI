package com.AdrianGniadek.GithubAPI.records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Repository(
        String name,
        @JsonProperty("owner") Owner owner,
        boolean fork
) {}
