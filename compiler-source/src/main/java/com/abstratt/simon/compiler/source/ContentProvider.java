package com.abstratt.simon.compiler.source;

import java.io.IOException;
import java.io.Reader;

/**
 * A content provider can supply a reader to the the contents on demand.
 *
 * Since reading contents can potentially fail, a content provider is
 * essentially a provider for a reader that can fail.
 */
public interface ContentProvider {
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
