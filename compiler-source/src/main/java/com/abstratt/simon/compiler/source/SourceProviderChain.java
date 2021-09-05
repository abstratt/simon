package com.abstratt.simon.compiler.source;

import java.util.List;
import java.util.Objects;

public class SourceProviderChain implements SourceProvider {
    private final List<SourceProvider> chain;

    public SourceProviderChain(List<SourceProvider> chain) {
        this.chain = chain;
    }

    @Override
    public ContentProvider access(String sourceName) {
        return chain.stream().map(provider -> provider.access(sourceName)).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
