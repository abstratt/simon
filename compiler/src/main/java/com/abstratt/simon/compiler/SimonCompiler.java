package com.abstratt.simon.compiler;

import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.source.ContentProvider;
import com.abstratt.simon.compiler.source.ContentProviderSourceProvider;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Slotted;

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
	
	interface Factory {
		<T> SimonCompiler<T> create(MetamodelSource.Factory<?> typeSourceFactory,
				Backend<? extends ObjectType, ? extends Slotted, T> configurationProvider);
	}
}
