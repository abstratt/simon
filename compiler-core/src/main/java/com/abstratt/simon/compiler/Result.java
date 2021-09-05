package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of compiling a compilation unit.
 */
public class Result<T> {
    private final String source;
    private final T rootObject;
    private final List<Problem> problems;

    public Result(String source, T rootObject, List<Problem> problems) {
        this.source = source;
        this.rootObject = rootObject;
        this.problems = new ArrayList<>(problems);
    }

    public static Result failure(String source) {
        return new Result(source, null, new ArrayList<>());
    }

    public T getRootObject() {
        return rootObject;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public String getSource() {
        return source;
    }
}
