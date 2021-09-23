package com.abstratt.simon.tests;

import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.findByFeature;
import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.getValue;
import static com.abstratt.simon.tests.TestHelper.compileResource;
import static com.abstratt.simon.tests.TestHelper.ensureSuccess;
import static com.abstratt.simon.tests.TestHelper.getPrimitiveValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.examples.IM;
import com.abstratt.simon.examples.UI;

public class RegressionTests {

    @Test
    void uiProgram() throws Exception {
        List<Result<EObject>> results = ensureSuccess(compileResource(UI.class, "/ui-sample.simon"));
        EObject application = results.get(0).getRootObject();
        List<EObject> screens = getValue(application, "screens");
        assertEquals(3, screens.size());
        EObject firstScreen = screens.get(0);
        EEnumLiteral layout = getValue(firstScreen, "layout");
        assertEquals(UI.PanelLayout.Vertical.name(), layout.getLiteral());
        List<EObject> screenComponents = getValue(firstScreen, "children");
        assertEquals(3, screenComponents.size());
        EObject firstButton = screenComponents.get(0);
        assertEquals("Ok", TestHelper.getPrimitiveValue(firstButton, "label"));
        EObject secondButton = screenComponents.get(1);
        assertEquals("Cancel", TestHelper.getPrimitiveValue(secondButton, "label"));
        EObject backgroundColor = getValue(secondButton, "backgroundColor");
        assertNotNull(backgroundColor);
        assertEquals(100, (int) TestHelper.getPrimitiveValue(backgroundColor, "red"));

        EObject link = screenComponents.get(2);
        assertEquals(screens.get(1), getValue(link, "targetScreen"));
    }

    @Test
    void imProgram() throws Exception {
        List<Result<EObject>> results = ensureSuccess(compileResource(IM.class, "/im-sample.simon"));
        EObject namespace = results.get(0).getRootObject();
        List<EObject> entities = getValue(namespace, "entities");
        assertEquals(5, entities.size());
        EObject memberEntity = findByFeature(entities, "name", "Member");
        assertNotNull(memberEntity);
        EObject memberEntityNameProperty = findByFeature(getValue(memberEntity, "properties"), "name", "name");
        EObject namePropertyType = getValue(memberEntityNameProperty, "type");
        assertEquals("Primitive", namePropertyType.eClass().getName());
        assertEquals("StringValue", getPrimitiveValue(namePropertyType, "name"));

    }
}
