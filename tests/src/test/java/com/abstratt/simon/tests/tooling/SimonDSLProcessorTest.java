package com.abstratt.simon.tests.tooling;

import com.abstratt.simon.tooling.SimonDSLProcessor;
import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimonDSLProcessorTest {
    @Test
    void sanity() {
        var compiled = Compiler.javac().compile(JavaFileObjects.forSourceLines("foo.bar.MyClass", """
                package foo.bar;
                public class MyClass {
                }
        """));
        assertEquals(Compilation.Status.SUCCESS,compiled.status());
        ImmutableList<JavaFileObject> generated = compiled.generatedFiles().asList();
        assertEquals(1, generated.size());
        assertEquals( "/"+ StandardLocation.CLASS_OUTPUT + "/foo/bar/MyClass.class", generated.get(0).getName());
    }

    @Test
    void getQualifiedName() {
        var compiled = Compiler.javac().withProcessors(new SimonDSLProcessor()).compile(JavaFileObjects.forResource("com/abstratt/simon/examples/DAUI.java"));
        assertFalse(compiled.generatedFiles().isEmpty());
        assertEquals(Compilation.Status.SUCCESS, compiled.status());
        var generated = compiled.generatedFile(StandardLocation.CLASS_OUTPUT, "com.abstratt.simon.examples", "DAUI.class").get();
        assertEquals(JavaFileObject.Kind.CLASS, generated.getKind());
        var dslRelated = compiled.generatedFiles().stream().filter(it -> it.getName().endsWith("examples.DAUI.IEntityComponent.entity.Typed.txt")).findFirst();
        assertTrue(dslRelated.isPresent());
        assertTrue(dslRelated.get().getName().endsWith("examples.DAUI.IEntityComponent.entity.Typed.txt"));
    }
}
