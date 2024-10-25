package com.abstratt.simon.tests;

import com.abstratt.simon.examples.kotlin.KotlinExampleLanguages;
import org.jetbrains.annotations.NotNull;

public class Kotlin2EcoreTest extends Java2EcoreTest {
    @NotNull
    @Override
    protected ClassConstants getClassConstants() {
        return new ClassConstants() {
            @Override
            protected String getLanguagePackage() {
                return KotlinExampleLanguages.class.getPackageName();
            }
        };
    }
}
