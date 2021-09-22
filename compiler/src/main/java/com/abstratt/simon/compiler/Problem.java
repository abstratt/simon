package com.abstratt.simon.compiler;

public class Problem {

    public enum Severity {
        Warning, Error, Fatal
    }

    public enum Category {
        Unspecified, Internal, MissingElement, SyntaxError, UnresolvedName, UnknownElement, AbstractElement,
        MissingFeature, ElementAdmitsNoFeatures, TypeError
    }

    private final Category category;
    private final Severity severity;
    private final String source;
    private final int line;
    private final int column;
    private final String message;

    public Problem(String source, int line, int column, String message, Severity severity, Category category) {
        this.source = source;
        this.line = line;
        this.column = column;
        this.message = message;
        this.severity = severity;
        this.category = category;
    }

    public Problem(String source, int line, int column, String message, Severity severity) {
        this(source, line, column, message, severity, Category.Unspecified);
    }

    public Problem(String source, String message, Severity severity) {
        this(source, -1, -1, message, severity);
    }

    public Severity severity() {
        return severity;
    }

    public Category category() {
        return category;
    }

    public int column() {
        return column;
    }

    public int line() {
        return line;
    }

    String message() {
        return message;
    }

    public String source() {
        return source;
    }

    @Override
    public String toString() {
        return message + " in " + source + " at " + ("[" + line + "," + column + "] (" + category + ")");
    }

    public interface Handler {
        void handleProblem(Problem toHandle);

        boolean hasFatalError();
    }
}
