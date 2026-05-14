package com.abstratt.simon.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.stream.Collectors;

import com.abstratt.simon.compiler.source.annotated.AnnotatedJava2EcoreMapper;
import org.eclipse.emf.ecore.ENamedElement;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Tests {@link AnnotatedJava2EcoreMapper} — the {@code @Meta}-annotated
 * Java reflection path. Inherits the shared mapper-shape tests from
 * {@link AbstractSource2EcoreMapperTest}; adds the two Java-reflection-only
 * checks that have no Simon-source analog.
 */
public class AnnotatedJava2EcoreMapperTest extends AbstractSource2EcoreMapperTest {

    @Override
    protected <E extends ENamedElement> E build(Class<?> clazz) {
        E built = new AnnotatedJava2EcoreMapper().map(clazz);
        ensureValid(built);
        return built;
    }

    /**
     * The annotation-driven mapper builds upstream packages lazily as
     * referenced, so the resulting resource is minimal: only the packages
     * actually pulled in by the build under test. The Simon-source mapper
     * loads every {@code .simon} package up front into one shared resource,
     * so this resource-shape check is Java-specific.
     */
    @Test
    void dependentPackageViaInheritance_resourceShape() {
        var ui2Package = build(classConstants.UI2_CLASS);
        assertEquals(2, ((org.eclipse.emf.ecore.EPackage) ui2Package).eResource().getContents().size());

        var uiPackage = ((org.eclipse.emf.ecore.EPackage) ui2Package).eResource().getContents().get(0);
        assertEquals(1, filter(uiPackage.eResource().getAllContents(),
                e -> e instanceof org.eclipse.emf.ecore.EClass
                        && ((org.eclipse.emf.ecore.EClass) e).getName().equals("Named")).size());
    }

    @Test
    void objectTypeFromAccessor() throws Exception {
        var type = MetaEcoreHelper.getType(classConstants.UI_SCREEN_CLASS.getMethod("application"));
        assertEquals(classConstants.UI_APPLICATION_CLASS, type);
    }

    @Test
    void isObjectType() throws Exception {
        var type = classConstants.UI_APPLICATION_CLASS;
        var annotations = MetaEcoreHelper.getTypeAnnotations(type).collect(Collectors.toList());
        assertEquals(Collections.emptyList(), annotations);
        assertFalse(MetaEcoreHelper.isExplicitlyObjectType(type));
        assertTrue(MetaEcoreHelper.isImplicitlyObjectType(type));
        assertTrue(MetaEcoreHelper.isObjectType(type));
    }
}
