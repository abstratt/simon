package com.abstratt.simon.tests;

import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.findByFeature;
import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.findChildByAttributeValue;
import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.findStructuralFeature;
import static com.abstratt.simon.metamodel.ecore.impl.EcoreHelper.getValue;
import static com.abstratt.simon.tests.TestHelper.IM_PACKAGE;
import static com.abstratt.simon.tests.TestHelper.UI2_PACKAGE;
import static com.abstratt.simon.tests.TestHelper.UI_PACKAGE;
import static com.abstratt.simon.tests.TestHelper.buildMetamodelSourceFactory;
import static com.abstratt.simon.tests.TestHelper.buildSourceProvider;
import static com.abstratt.simon.tests.TestHelper.compileProject;
import static com.abstratt.simon.tests.TestHelper.compileUsingIM;
import static com.abstratt.simon.tests.TestHelper.compileUsingUI;
import static com.abstratt.simon.tests.TestHelper.ensureSuccess;
import static com.abstratt.simon.tests.TestHelper.getPrimitiveValue;
import static com.abstratt.simon.tests.TestHelper.root;
import static com.abstratt.simon.tests.TestHelper.uiClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.examples.IM;
import com.abstratt.simon.examples.UI;

public class CompilerTests {
    private static final EClass namespaceClass = TestHelper.imClassFor(IM.Namespace.class);
    private static final EClass applicationClass = uiClassFor(UI.Application.class);
    private static final EClass buttonClass = uiClassFor(UI.Button.class);
    private static final EClass namedClass = uiClassFor(UI.Named.class);
    private static final EClass containerClass = uiClassFor(UI.Container.class);
    private static final EClass screenClass = uiClassFor(UI.Screen.class);

    @Test
    void emptyApplication() {
        var application = emptyApplication("@language UI Application myApplication {}");
        assertNotNull(application.eResource());
        assertEquals("myApplication", getPrimitiveValue(application, "name"));
    }

    @Test
    void emptyApplications() {
        String[] toParse = { "@language UI Application myApplication1 {}",
                "@language UI Application myApplication2 {}" };
        List<Result<EObject>> results = ensureSuccess(compileProject(Arrays.asList(UI_PACKAGE), toParse));
        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getRootObjects().size());
        assertEquals(1, results.get(1).getRootObjects().size());
        EObject application1 = results.get(0).getRootObject();
        EObject application2 = results.get(1).getRootObject();
        assertEquals("myApplication1", getPrimitiveValue(application1, "name"));
        assertEquals("myApplication2", getPrimitiveValue(application2, "name"));
        assertNotNull(application1.eResource());
        assertSame(application1.eResource(), application2.eResource());
    }

    @Test
    void multipleApplications() {
        String[] toParse = { """
                  @language UI
                  Application myApplication1 {}
                  Application myApplication2 {}
                """ };
        List<Result<EObject>> results = ensureSuccess(compileProject(Arrays.asList(UI_PACKAGE), toParse));
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getRootObjects().size());
        EObject application1 = results.get(0).getRootObjects().get(0);
        EObject application2 = results.get(0).getRootObjects().get(1);
        assertEquals("myApplication1", getPrimitiveValue(application1, "name"));
        assertEquals("myApplication2", getPrimitiveValue(application2, "name"));
        assertNotNull(application1.eResource());
        assertSame(application1.eResource(), application2.eResource());
    }

    @Test
    void typeReference() {
        var source = """
                @language IM
                Namespace customers {
                  entities {
                    entity Customer
                    entity Order {
                      relationships {
                        relationship { type: Customer }
                      }
                    }
                  }
                }
                """;
        String[] toParse = { source };
        EObject namespace = root(ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE), toParse)));
        List<EObject> entities = getValue(namespace, "entities");
        assertEquals("Customer", getPrimitiveValue(entities.get(0), "name"));
        assertEquals("Order", getPrimitiveValue(entities.get(1), "name"));
    }

    @Test
    void crossFileReference() {
        doCrossFileReference(0, 1);
    }

    @Test
    void crossFileReferenceReverseOrder() {
        doCrossFileReference(1, 0);
    }

    void doCrossFileReference(int... order) {
        String customersSource = """
                @language IM
                namespace customers {
                        entities {
                            entity Customer
                        }
                }
                """;
        String ordersSource = """
                @language IM
                namespace orders {
                        entities {
                                entity Order {
                                        relationships {
                                                relationship customer { type: customers.Customer }
                                        }
                                }
                        }
                }
                """;
        String[] sources = { customersSource, ordersSource };
        String[] toParse = { sources[order[0]], sources[order[1]] };
        var results = ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE), toParse));
        var customersNamespace = results.get(order[0]).getRootObject();
        var ordersNamespace = results.get(order[1]).getRootObject();
        assertEquals("customers", getPrimitiveValue(customersNamespace, "name"));
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
        var orderEntity = findChildByAttributeValue(ordersNamespace, "name", "Order");
        var customerRelationship = findChildByAttributeValue(orderEntity, "name", "customer");
        assertNotNull(customerRelationship);
        var customerEntity = findChildByAttributeValue(customersNamespace, "name", "Customer");
        assertSame(customerEntity, getValue(customerRelationship, "type"));
    }

    @Test
    void unresolvedReference() {
        var results = compileProject(IM_PACKAGE, """
                    @language IM
                    namespace orders {
                        entities {
                            entity Order {
                                relationships {
                                    relationship customer { type: FOOBAR }
                                }
                            }
                        }
                    }
                """);

        var problems = results.get(0).getProblems();
        assertEquals(1, problems.size());
        assertEquals(Problem.Category.UnresolvedName, problems.get(0).category(), problems.get(0)::toString);

        var ordersNamespace = results.get(0).getRootObject();
        var orderEntity = findChildByAttributeValue(ordersNamespace, "name", "Order");
        var customerRelationship = findChildByAttributeValue(orderEntity, "name", "customer");
        assertNull(getValue(customerRelationship, "type"));
    }

    @Test
    void imports() {
        var customers = """
                @language IM
                namespace customers
                """;
        var orders = """
                @language IM
                @import 'customers'
                namespace orders
                """;

        var allSources = new HashMap<String, String>();
        allSources.put("customers", customers);
        allSources.put("orders", orders);
        var results = ensureSuccess(compileProject(Collections.singletonList("orders"),
                buildMetamodelSourceFactory(Arrays.asList(IM_PACKAGE)), buildSourceProvider(allSources)));
        assertEquals(2, results.size());
        var ordersNamespace = results.get(0).getRootObject();
        var customersNamespace = results.get(1).getRootObject();
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
        assertNotNull(ordersNamespace.eResource());
        assertSame(ordersNamespace.eResource(), customersNamespace.eResource());
    }

    @Test
    void importBuiltIn() {
        var orders = """
                @language IM
                @import 'im'
                namespace orders
                """;

        var results = ensureSuccess(compileProject(Collections.singletonList("orders"),
                buildMetamodelSourceFactory(Arrays.asList(IM_PACKAGE)),
                buildSourceProvider(Collections.singletonMap("orders", orders))));
        assertEquals(2, results.size());
        var namespace = results.get(0).getRootObject();
        assertEquals("orders", getPrimitiveValue(namespace, "name"));
        assertNotNull(results.get(0).getRootObject().eResource());
        assertSame(results.get(0).getRootObject().eResource(), results.get(1).getRootObject().eResource());
    }

    @Test
    void crossFileReferenceViaImport() {
        var customers = """
                @language IM
                namespace customers {
                        entities {
                            entity Customer
                        }
                }
                """;
        var orders = """
                @language IM
                @import 'customers'
                namespace orders {
                        entities {
                                entity Order {
                                        relationships {
                                                relationship { type: customers.Customer }
                                        }
                                }
                        }
                }
                """;

        var allSources = new HashMap<String, String>();
        allSources.put("customers", customers);
        allSources.put("orders", orders);
        var results = ensureSuccess(compileProject(Arrays.asList("orders"),
                buildMetamodelSourceFactory(Arrays.asList(IM_PACKAGE)), buildSourceProvider(allSources)));
        assertEquals(2, results.size());
        var ordersNamespace = results.get(0).getRootObject();
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
    }

    @Test
    void incompatibleReference() {
        var source = """
                    @language UI
                    application {
                      screens {
                        screen (layout : Vertical) {
                            children {
                                button btn1 (label : 'Ok')
                                link(label: 'To screen 2') {
                                  targetScreen: btn1
                                }
                            }
                        }
                      }
                    }
                """;
        var results = compileProject(UI_PACKAGE, source);
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).getProblems().size());
        var problem = results.get(0).getProblems().get(0);
        assertEquals(Problem.Category.TypeError, problem.category(), problem::toString);
        assertEquals(Problem.Severity.Error, problem.severity(), problem::toString);
        assertEquals(8, problem.line());
        assertEquals(32, problem.column());
    }

    @Test
    void emptyApplication_metaclassCapitalization() {
        emptyApplication("@language UI application myApplication {}");
    }

    @Test
    void emptyApplicationWithNameAsProperty() {
        emptyApplication("@language UI Application (name = 'myApplication')");
    }

    @Test
    void numericalSlot() {
        EObject button = compileUsingUI("Button (index = 3)");
        int buttonIndex = getPrimitiveValue(button, "index");
        assertEquals(3, buttonIndex);
    }

    @Test
    void namespaceWithTwoEntities() {
        String[] toParse = { """
                @language IM
                namespace myapp {
                        entities {
                                Entity Customer
                                Entity Order
                        }
                }
                """ };
        var namespace1 = root(ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE), toParse)));
        List<EObject> entities = getValue(namespace1, "entities");
        assertEquals("Customer", getPrimitiveValue(entities.get(0), "name"));
        assertEquals("Order", getPrimitiveValue(entities.get(1), "name"));
    }

    @Test
    // issue https://github.com/abstratt/simon/issues/3
    void primitiveTypes() {
        var toParse = """
                @language IM
                @import 'im'
                namespace {
                    entities {
                        Entity Product {
                            properties {
                                Property description { type = im.StringValue }
                            }
                        }
                    }
                }""";
        var namespace = compileUsingIM(toParse);
        List<EObject> entities = getValue(namespace, "entities");
        assertEquals(1, entities.size());
        EObject productEntity = entities.get(0);
        assertNotNull(productEntity);
        assertEquals("Product", getPrimitiveValue(productEntity, "name"));
        Collection<EObject> properties = getValue(productEntity, "properties");
        var descriptionProperty = findByFeature(properties, "name", "description");
        assertNotNull(descriptionProperty);
        var typeReference = findStructuralFeature(descriptionProperty, "type");
        EObject descriptionPropertyType = getValue(descriptionProperty, "type");
        assertEquals("Primitive", descriptionPropertyType.eClass().getName());
        assertEquals("StringValue", getPrimitiveValue(descriptionPropertyType, "name"));
    }
    
    @Test
    void annotations() {
        var toParse = """
                @language IM
                @import 'im'
                namespace {
                    entities {
                        [abstract] entity Product
                    }
                }""";
        var namespace = compileUsingIM(toParse);
        List<EObject> entities = getValue(namespace, "entities");
        assertEquals(1, entities.size());
        EObject productEntity = entities.get(0);
        assertNotNull(productEntity);
        assertEquals("Product", getPrimitiveValue(productEntity, "name"));
        var abstract_ = getValue(productEntity, "abstract");
        assertNotNull(abstract_);
        assertTrue((boolean) getPrimitiveValue(productEntity, "abstract")); 
    }


    @Test
    void recordSlot() {
        var rootObject = compileUsingUI("Button (backgroundColor = #(red = 100 blue = 50))");
        var backgroundColor = (EObject) getValue(rootObject, "backgroundColor");
        assertNotNull(backgroundColor);
        assertEquals(100, (int) getPrimitiveValue(backgroundColor, "red"));
        assertEquals(50, (int) getPrimitiveValue(backgroundColor, "blue"));
        assertEquals(0, (int) getPrimitiveValue(backgroundColor, "green"));

    }

    private EObject emptyApplication(String toParse) {
        EObject application = compileUsingUI(toParse);
        assertEquals("myApplication", getPrimitiveValue(application, "name"));
        return application;
    }

    @Test
    void applicationWithScreens() {
        var myApplication = compileUsingUI("""
                application myApplication {
                        screens {
                            Screen screen1 {}
                            Screen screen2 {}
                            Screen screen3 {}
                        }
                }
                            """);
        assertNotNull(myApplication);
        assertSame(applicationClass, myApplication.eClass());
        List<EObject> screens = getValue(myApplication, "screens");
        assertEquals(3, screens.size());
        for (int i = 0; i < screens.size(); i++) {
            assertEquals("screen" + (i + 1), getPrimitiveValue(screens.get(i), "name"));
        }
    }

    @Test
    void fullyQualifiedReferences() {
        var application = compileUsingUI("""
                @language UI
                application myApplication {
                    screens {
                      screen screen1 {
                        children {
                          link {
                              targetScreen: myApplication.screen2
                          }
                        }
                      }
                      screen screen2 {}
                    }
                }
                  """);
        List<EObject> screens = getValue(application, "screens");
        assertEquals(2, screens.size());
        EObject firstScreen = screens.get(0);
        List<EObject> screenComponents = getValue(firstScreen, "children");
        assertEquals(1, screenComponents.size());
        EObject link = screenComponents.get(0);
        assertEquals(screens.get(1), getValue(link, "targetScreen"));
    }

    @Test
    void multipleLanguages() {
        String[] toParse = { """
                  @language UI
                  @language IM
                  application myApplication
                  namespace myNamespace
                """ };
        var results = ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE, UI_PACKAGE), toParse));
        assertEquals(1, results.size());
        var roots = results.get(0).getRootObjects();
        assertEquals(2, roots.size());
        assertEquals("myApplication", getPrimitiveValue(roots.get(0), "name"));
        assertEquals("myNamespace", getPrimitiveValue(roots.get(1), "name"));
        assertSame(applicationClass, roots.get(0).eClass());
        assertSame(namespaceClass, roots.get(1).eClass());
    }

    @Test
    void multiplePackagesEmptySource() {
        String[] toParse = { "" };
        var roots = ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE, UI_PACKAGE), toParse));
        assertEquals(1, roots.size());
        assertNull(roots.get(0).getRootObject());
        assertEquals(0, roots.get(0).getProblems().size());
    }

    @Test
    void derivedLanguages() {
        String toParse = """
                  @language UI2
                  @language UI
                  application myApplication {
                      screens {
                          screen main {
                              children {
                                  form MyForm {
                                        children {
                                          link(label: 'To screen 2') {
                                              targetScreen: helpScreen
                                          }
                                      }
                                  }
                              }
                          }
                          screen helpScreen
                      }
                  }
                """;
        var application = ensureSuccess(compileProject(UI2_PACKAGE.eResource(), toParse)).get(0).getRootObject();
        List<EObject> screens = getValue(application, "screens");
        assertEquals(2, screens.size());
        EObject mainScreen = screens.get(0);
        EObject helpScreen = screens.get(1);
        EObject form = ((List<EObject>) getValue(mainScreen, "children")).get(0);
        EObject link = ((List<EObject>) getValue(form, "children")).get(0);
        EObject linkTarget = getValue(link, "targetScreen");
        assertNotNull(linkTarget);
        assertSame(helpScreen, linkTarget);
    }
}
