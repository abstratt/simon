package com.abstratt.simon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Base class for Simon compilers.
 * 
 * This base class is agnostic to actual compiler implementation.
 * 
 * Compilation in general operates on a set of inputs. Symbol resolution can
 * take the multiple inputs into account in order to resolve symbol references.
 * 
 * Compiling a single input is a special case, where the set of inputs size 1.
 * 
 * @param <T> the type of model being built
 */
public interface SimonCompiler<T> {

	/**
	 * The result of compiling a compilation unit.
	 */
	class Result<T> {
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

	/**
	 * A content provider can supply a reader to the the contents on demand.
	 * 
	 * Since reading contents can potentially fail, a content provider is
	 * essentially a provider for a reader that can fail.
	 */
	interface ContentProvider {
		/**
		 * Opens a reader to the contents. The caller is responsible for closing the
		 * reader.
		 */
		Reader getContents() throws IOException;
		
		/**
		 * Adapts an already created reader as a content provider.
		 * 
		 * @param reader
		 * @return
		 */
		static ContentProvider provideContents(Reader reader) {
			return () -> reader;
		}

	}
	
	interface SourceProvider {
		SourceProvider NULL = source -> null;

		/**
		 * Requests access to a source with the given name.
		 * 
		 * @param sourceName
		 * @return a content provider for accessing such source, or null if one could not be found
		 */
		ContentProvider access(String sourceName);
	}

	List<Result<T>> compile(List<String> entryPoints, SourceProvider sources);

	default Result<T> compile(Reader contents) {
		return compile(ContentProvider.provideContents(contents));
	}

	/**
	 * Compiles a single input as provided by the given provider.
	 * 
	 * @param input
	 * @return
	 */
	default Result<T> compile(String name, ContentProvider input) {
		return compile(Arrays.asList(name), new ContentProviderSourceProvider(Collections.singletonMap(name, input))).get(0);
	}

	default Result<T> compile(ContentProvider input) {
		return compile("input", input);
	}

	default Result<T> compile(URI uri) {
		return compile(() -> new InputStreamReader(uri.toURL().openStream()));
	}

	default Result<T> compile(URL url) {
		try (InputStream contents = url.openStream()) {
			return compile(contents);
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}

	default Result<T> compile(InputStream contents) {
		return compile(() -> new InputStreamReader(contents, StandardCharsets.UTF_8));
	}

	default Result<T> compile(Path toParse) {
		return compile(() -> Files.newBufferedReader(toParse));
	}

	default Result<T> compile(String toParse) {
		return compile(ContentProvider.provideContents(new StringReader(toParse)));
	}
}
