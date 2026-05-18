# Contributing to Simon

## Logging

Simon's library modules log through [SLF4J](https://www.slf4j.org/). No backend is shipped with the library jars — consumers wire in their own (Logback, Log4j 2, slf4j-simple, etc.) and control verbosity through that backend's configuration.

### During tests

The test suite uses `slf4j-simple` and is configured for quiet output: `tests/src/test/resources/simplelogger.properties` sets `defaultLogLevel=warn`, so Simon's `debug` / `trace` / `info` lines are suppressed by default. Genuine warnings and errors still surface.

To re-enable Simon's logs while running tests, pass a system property on the command line:

```bash
# Everything from Simon at DEBUG (loud)
mvn -pl tests test -Dorg.slf4j.simpleLogger.log.com.abstratt.simon=debug

# Only one sub-package
mvn -pl tests test -Dorg.slf4j.simpleLogger.log.com.abstratt.simon.compiler.source.annotated=debug

# All loggers at DEBUG (also surfaces EMF/ClassGraph chatter)
mvn -pl tests test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug

# Specific test class with debug
mvn -pl tests test -Dtest=AnnotatedJava2EcoreMapperTest \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dorg.slf4j.simpleLogger.log.com.abstratt.simon=debug
```

Levels accepted: `trace`, `debug`, `info`, `warn`, `error`, `off`. See the [slf4j-simple docs](https://www.slf4j.org/api/org/slf4j/simple/SimpleLogger.html) for the full list of configuration keys.

### In code

Add a private static logger at the top of each class and use parameterised messages so the format string is built only when the level is enabled:

```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);

log.debug("Building if needed: {}", clazz.getName());
```

Use `trace` for high-frequency events (per-classifier progress), `debug` for normal diagnostics, `info` sparingly for one-off lifecycle events, and `warn`/`error` for real problems. Do not use `System.out.println` for diagnostics.

### In downstream code

Consumers embedding Simon as a library control Simon's log level through their own SLF4J backend. With Logback:

```xml
<logger name="com.abstratt.simon" level="DEBUG"/>
```

With Log4j 2:

```xml
<Logger name="com.abstratt.simon" level="debug"/>
```

With slf4j-simple:

```
-Dorg.slf4j.simpleLogger.log.com.abstratt.simon=debug
```
