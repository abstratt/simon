package com.abstratt.simon.compiler;

public class Problem {

	public enum Severity {
		Warning,
		Error,
		Fatal;
	}

	private final Severity severity;
	private String source;
	private int line;
	private int column;
	private String message;

	public Problem(String source, int line, int column, String message, Severity severity) {
		this.source = source;
		this.line = line;
		this.column = column;
		this.message = message;
		this.severity = severity;
	}

	public Severity category() {
		return severity;
	}

	public int column() {
		return column;
	}

	int line() {
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
		return message + " in " + source + " at " + (line + "," + column);
	}

	public interface Handler {
		void handleProblem(Problem toHandle);
	}
}
