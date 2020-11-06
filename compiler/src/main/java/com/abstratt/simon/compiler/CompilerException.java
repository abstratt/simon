package com.abstratt.simon.compiler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CompilerException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private List<Problem> problems = Collections.emptyList();

	public CompilerException(List<Problem> problems) {
		this.problems = problems;
	}
	
	public CompilerException() {
		super();
	}

	public CompilerException(String message, Throwable cause) {
		super(message, cause);
	}

	public CompilerException(String message) {
		super(message);
	}

	public CompilerException(Throwable cause) {
		super(cause);
	}
	
	@Override
	public String getMessage() {
		return problems.isEmpty() ? super.getMessage() : problems.toString();
	}
}
