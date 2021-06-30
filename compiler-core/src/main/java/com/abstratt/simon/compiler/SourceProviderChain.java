package com.abstratt.simon.compiler;

import java.util.List;
import java.util.Objects;

public class SourceProviderChain implements SimonCompiler.SourceProvider {
    private List<SimonCompiler.SourceProvider> chain;

    public SourceProviderChain(List<SimonCompiler.SourceProvider> chain) {
        this.chain = chain;
    }

    @Override
    public SimonCompiler.ContentProvider access(String sourceName) {
        return chain.stream().map(provider -> provider.access(sourceName)).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
