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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
	public static class Result<T> {
		private final T rootObject;
		private final List<Problem> problems;

		public Result(T rootObject, List<Problem> problems) {
			this.rootObject = rootObject;
			this.problems = problems;
		}

		public T getRootObject() {
			return rootObject;
		}

		public List<Problem> getProblems() {
			return problems;
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
		public static ContentProvider provideContents(Reader reader) {
			return () -> reader;
		}

	}

	/**
	 * Compiles a stream of inputs provided by the given providers.
	 * 
	 * @param inputs
	 * @return one result per input
	 */
	public List<Result<T>> compile(Stream<ContentProvider> inputs);

	public default Result<T> compile(Reader contents) {
		return compile(ContentProvider.provideContents(contents));
	}

	/**
	 * Compiles a single input as provided by the given provider.
	 * 
	 * @param contentProvider
	 * @return
	 */

	public default Result<T> compile(ContentProvider input) {
		return compile(Stream.of(input)).get(0);
	}

	public default Result<T> compile(URI uri) {
		return compile(() -> new InputStreamReader(uri.toURL().openStream()));
	}

	public default Result<T> compile(URL url) {
		try (InputStream contents = url.openStream()) {
			return compile(contents);
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}

	public default Result<T> compile(InputStream contents) {
		return compile(() -> new InputStreamReader(contents, StandardCharsets.UTF_8));
	}

	public default Result<T> compile(Path toParse) {
		return compile(() -> Files.newBufferedReader(toParse));
	}

	public default Result<T> compile(String toParse) {
		return compile(ContentProvider.provideContents(new StringReader(toParse)));
	}

	public default List<Result<T>> compile(String... toParse) {
		Stream<String> stream = Arrays.stream(toParse);
		Stream<StringReader> readers = stream.map(StringReader::new);
		Stream<ContentProvider> contentProviders = readers.map(ContentProvider::provideContents);
		return compile(contentProviders);
	}

}
