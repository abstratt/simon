package com.abstratt.simon.tooling;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@SupportedAnnotationTypes("com.abstratt.simon.metamodel.dsl.Meta.*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class SimonDSLProcessor extends AbstractProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimonDSLProcessor.class);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Initializing: " + processingEnv.getOptions());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Source version: " + processingEnv.getSourceVersion());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        printMessage(Diagnostic.Kind.NOTE, "Processing " + annotations, null);
        roundEnv.getRootElements().forEach(e -> log.debug("Root: {} : {}", e.getSimpleName(), e.getKind()));
        annotations.forEach(a -> {
            printMessage(Diagnostic.Kind.NOTE, "Processing annotation " + a.getSimpleName(), null);
            roundEnv.getElementsAnnotatedWith(a).forEach(e ->
                    {
                        printMessage(Diagnostic.Kind.NOTE, "Processing Element ", e);
                        try {
                            var created = processingEnv.getFiler().createResource(
                                    StandardLocation.SOURCE_OUTPUT, "",
                                    getQualifiedName(e) + "." + a.getSimpleName() + ".txt", e);
                            try (var writer = new BufferedWriter(created.openWriter())) {
                                writer.write("Generated from " + getQualifiedName(e) + " for " + getQualifiedName(a));
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
            );
        });

        return true;
    }

    protected void printMessage(Diagnostic.Kind note, String message, Element e) {
        processingEnv.getMessager().printMessage(note, message, e);
    }

    @VisibleForTesting
    CharSequence getQualifiedName(Element e) {
        var parent = Optional.ofNullable(e.getEnclosingElement());
        return parent
                .filter(it -> !it.getSimpleName().isEmpty())
                .map(it ->  {log.debug("e: {}", it); return it; })
                .map(this::getQualifiedName)
                .map(it ->  {log.debug("qn: {}", it); return it; })
                .map(it -> it + ".")
                .map(it -> (CharSequence) (it + e.getSimpleName()))
                .orElseGet(e::getSimpleName);
    }
}
