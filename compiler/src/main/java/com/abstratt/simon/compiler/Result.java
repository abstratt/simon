package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.abstratt.simon.compiler.Problem.Severity;

/**
 * The result of compiling a compilation unit.
 */
public class Result<T> {
    private final String source;
    private final List<T> rootObjects;
    private final List<Problem> problems;

    public Result(String source, List<T> rootObjects, List<Problem> problems) {
        this.source = source;
        this.rootObjects = new ArrayList<>(rootObjects);
        this.problems = new ArrayList<>(problems);
    }

    public static <T> Result<T> failure(String source, Problem problem) {
        return new Result<>(source, Collections.emptyList(), Arrays.asList(problem));
    }

    public T getRootObject() {
        return rootObjects.isEmpty() ? null : rootObjects.get(0);
    }
    
    public List<T> getRootObjects() {
        return rootObjects;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public String getSource() {
        return source;
    }
}
