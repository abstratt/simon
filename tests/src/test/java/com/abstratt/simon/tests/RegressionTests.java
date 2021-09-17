package com.abstratt.simon.tests;

import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.findByFeature;
import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.getValue;
import static com.abstratt.simon.testing.TestHelper.compileResourceToEObject;
import static com.abstratt.simon.testing.TestHelper.getPrimitiveValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.examples.im.IM;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.testing.TestHelper;

public class RegressionTests {


    @Test
    void uiProgram() throws Exception {
        EObject application = compileResourceToEObject(UI.class, "/ui-sample.simon");
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
        EObject namespace = compileResourceToEObject(IM.class,"/im-sample.simon");
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
