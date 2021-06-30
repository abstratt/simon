package com.abstratt.simon.compiler;

public class Problem {
	private int line;
	private int column;
	private String message;

	public Problem(int line, int column, String message) {
		this.line = line;
		this.column = column;
		this.message = message;
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

	@Override
	public String toString() {
		return message + " - " + (line + "," + column);
	}
}
