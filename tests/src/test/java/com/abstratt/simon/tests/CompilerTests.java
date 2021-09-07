package com.abstratt.simon.tests;

import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.antlr.SimonCompilerAntlrImpl;
import com.abstratt.simon.compiler.backend.ecore.EcoreModelBuilder;
import com.abstratt.simon.compiler.source.MetamodelSource.Factory;
import com.abstratt.simon.compiler.source.MetamodelSourceChain;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.examples.kirra.Kirra;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.testing.TestHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

import static com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper.*;
import static com.abstratt.simon.testing.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

public class CompilerTests {
    private static final EClass namespaceClass = TestHelper.kirraClassFor(Kirra.Namespace.class);
    private static final EClass applicationClass = uiClassFor(UI.Application.class);
    private static final EClass buttonClass = uiClassFor(UI.Button.class);
    private static final EClass namedClass = uiClassFor(UI.Named.class);
    private static final EClass containerClass = uiClassFor(UI.Container.class);
    private static final EClass screenClass = uiClassFor(UI.Screen.class);

    @Test
    void metamodelResolveType() {
        var metamodel = new EPackageMetamodelSource.Factory(UI_PACKAGE).build();
        var resolved = metamodel.resolveType("Application");
        assertNotNull(resolved);
    }

    @Test
    void emptyApplication() {
        var application = emptyApplication("Application myApplication {}");
        assertNotNull(application.eResource());
    }

    @Test
    void emptyApplications() {
        List<Result<EObject>> results = compileValidProject(UI_PACKAGE, "Application myApplication1 {}", "Application myApplication2 {}");
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
        List<Result<EObject>> results = compileValidProject(UI_PACKAGE, """
                  		Application myApplication1 {}
                  		Application myApplication2 {}
                """);
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
                Namespace customers {
                  entities {
                    Entity Customer
                    Entity Order {
                      relationships {
                        relationship { type: Customer }
                      }
                    }
                  }
                }
                """;
        EObject namespace = compile(KIRRA_PACKAGE, source);
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
                namespace customers {
                        entities {
                            entity Customer
                        }
                }
                """;
        String ordersSource = """
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
        String[] sources = {customersSource, ordersSource};
        var results = compileValidProject(KIRRA_PACKAGE,
                sources[order[0]],
                sources[order[1]]
        );
        var customersNamespace = results.get(order[0]).getRootObject();
        var ordersNamespace = results.get(order[1]).getRootObject();
        assertEquals("customers", getPrimitiveValue(customersNamespace, "name"));
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
        var orderEntity = findChildByAttributeValue(ordersNamespace, "name", "Order");
        var customerRelationship = findChildByAttributeValue(orderEntity, "name", "customer");
        assertNotNull(customerRelationship);
    }

    @Test
    void unresolvedReference() {
        var results = compileProject(KIRRA_PACKAGE,
                Collections.singletonList("orders"),
                Collections.singletonMap("orders",
                        """
                                Namespace orders {
                                        entities { 
                                                Entity Order { 
                                                        relationships { 
                                                                relationship { type: customers.Customer } 
                                                        } 
                                                }
                                        }
                                }
                                """)
        );

        var problems = results.get(0).getProblems();
        assertNotEquals(0, problems.size());
    }

    @Test
    void imports() {
        var customers =
                """
                        namespace customers
                        """;
        var orders =
                """
                        @import 'customers'
                        namespace orders
                        """;

        var allSources = new HashMap<String, String>();
        allSources.put("customers", customers);
        allSources.put("orders", orders);
        var results = compileValidProject(
                KIRRA_PACKAGE,
                Collections.singletonList("orders"),
                allSources
        );
        assertEquals(2, results.size());
        var ordersNamespace = results.get(0).getRootObject();
        var customersNamespace = results.get(1).getRootObject();
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
        assertNotNull(ordersNamespace.eResource());
        assertSame(ordersNamespace.eResource(), customersNamespace.eResource());
    }

    @Test
    void importBuiltIn() {
        var orders =
                """
                        @import 'kirra'
                        namespace orders
                        """;

        var results = compileValidProject(
                KIRRA_PACKAGE,
                Collections.singletonList("orders"),
                Collections.singletonMap("orders", orders)
        );
        assertEquals(2, results.size());
        var namespace = results.get(0).getRootObject();
        assertEquals("orders", getPrimitiveValue(namespace, "name"));
        assertNotNull(results.get(0).getRootObject().eResource());
        assertSame(results.get(0).getRootObject().eResource(), results.get(1).getRootObject().eResource());
    }


    @Test
    void crossFileReferenceViaImport() {
        var customers =
                """
                        namespace customers {
                                entities {
                                    entity Customer
                                }
                        }
                        """;
        var orders =
                """
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
        var results = compileValidProject(
                KIRRA_PACKAGE,
                Collections.singletonList("orders"),
                allSources
        );
        assertEquals(2, results.size());
        var ordersNamespace = results.get(0).getRootObject();
        assertEquals("orders", getPrimitiveValue(ordersNamespace, "name"));
    }


    @Test
    void emptyApplication_metaclassCapitalization() {
        emptyApplication("application myApplication {}");
    }

    @Test
    void emptyApplicationWithNameAsProperty() {
        emptyApplication("Application (name = 'myApplication')");
    }

    @Test
    void numericalSlot() {
        EObject button = compileUI("Button (index = 3)");
        int buttonIndex = getPrimitiveValue(button, "index");
        assertEquals(3, buttonIndex);
    }

    @Test
    void namespaceWithTwoEntities() {
        var namespace1 = compile(KIRRA_PACKAGE,
                """
                        Namespace myapp { 
                                entities { 
                                        Entity Customer 
                                        Entity Order 
                                } 
                        }
                        """
        );
        List<EObject> entities = getValue(namespace1, "entities");
        assertEquals("Customer", getPrimitiveValue(entities.get(0), "name"));
        assertEquals("Order", getPrimitiveValue(entities.get(1), "name"));
    }

    @Test
        // issue https://github.com/abstratt/simon/issues/3
    void primitiveTypes()
    {
        var toParse = """
                @import 'kirra'
                namespace { 
                    entities { 
                        Entity Product { 
                            properties { 
                                Property description { type = kirra.StringValue } 
                            } 
                        } 
                    } 
                }""";
        var namespace = compileKirra(toParse);
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
    void recordSlot() {
        var rootObject = compileUI("Button (backgroundColor = #(red = 100 blue = 50))");
        var backgroundColor = (EObject) getValue(rootObject, "backgroundColor");
        assertNotNull(backgroundColor);
        assertEquals(100, (int) getPrimitiveValue(backgroundColor, "red"));
        assertEquals(50, (int) getPrimitiveValue(backgroundColor, "blue"));
        assertEquals(0, (int) getPrimitiveValue(backgroundColor, "green"));

    }

    private EObject emptyApplication(String toParse) {
        EObject application = compileUI(toParse);
        assertEquals("myApplication", getPrimitiveValue(application, "name"));
        return application;
    }

    private static EObject compileUI(String toParse) {
        return compile(UI_PACKAGE, toParse);
    }

    private static EObject compileKirra(String toParse) {
        return compile(KIRRA_PACKAGE, toParse);
    }

    private static EObject compile(EPackage package_, String toParse) {
        return compile(Arrays.asList(package_), toParse);
    }

    private static EObject compile(List<EPackage> asList, String toParse) {
        var models = compileValidProject(asList, toParse);
        assertNotNull(models.get(0));
        return models.get(0).getRootObject();
    }

    private static List<Result<EObject>> compileValidProject(EPackage package_, String... toParse) {
        return compileValidProject(Arrays.asList(package_), toParse);
    }

    private static List<Result<EObject>> compileValidProject(List<EPackage> packages, String... toParse) {
        int[] index = {0};
        var allSources = Arrays.stream(toParse).collect(Collectors.toMap(it -> "source" + index[0]++, it -> it));
        return compileValidProject(packages, new ArrayList(allSources.keySet()), allSources);
    }

    private static List<Result<EObject>> compileValidProject(EPackage package_, List<String> entryPoints, Map<String, String> toParse) {
        return compileValidProject(Arrays.asList(package_), entryPoints, toParse);
    }

    private static List<Result<EObject>> compileValidProject(List<EPackage> packages, List<String> entryPoints,
            Map<String, String> toParse)
    {
        var results = compileProject(packages, entryPoints, toParse);
        results.forEach(CompilerTests::ensureSuccess);
        return results;
    }

    private static List<Result<EObject>> compileProject(EPackage package_, List<String> entryPoints, Map<String, String> toParse) {
        return compileProject(Arrays.asList(package_), entryPoints, toParse);
    }

    private static List<Result<EObject>> compileProject(List<EPackage> packages, List<String> entryPoints,
            Map<String, String> toParse)
    {
        List<Factory<EcoreType<? extends EClassifier>>> sourceFactories = packages.stream().map(p -> new EPackageMetamodelSource.Factory(p)).collect(Collectors.toList());
        // TODO-RC this should be supported in the compiler itself, which should take
        // multiple type sources (one per language) and then decide which ones to enable
        // based on the languages declared in the file using the @language processing instruction
        var typeSourceFactory = new MetamodelSourceChain.Factory<EcoreType<? extends EClassifier>>(sourceFactories);
        var modelBuilder = new EcoreModelBuilder();
        var compiler = new SimonCompilerAntlrImpl<>(typeSourceFactory, modelBuilder);
        return compiler.compile(entryPoints, new SimpleSourceProvider(toParse));
    }

    private static void ensureSuccess(Result<?> result) {
        assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
    }

    @Test
    void applicationWithScreens() {
        var myApplication = compileUI(
                """
                            Application myApplication {
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
        var application = compileUI("""
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
        var results = compileValidProject(Arrays.asList(KIRRA_PACKAGE, UI_PACKAGE), """
                  @language UI
                  @language Kirra
                  application myApplication
                  namespace myNamespace
                """);
        assertEquals(1, results.size());
        var roots = results.get(0).getRootObjects();
        assertEquals(2, roots.size());
        assertEquals("myApplication", getPrimitiveValue(roots.get(0), "name"));
        assertEquals("myNamespace", getPrimitiveValue(roots.get(1), "name"));
        assertSame(applicationClass, roots.get(0).eClass());
        assertSame(namespaceClass, roots.get(1).eClass());
    }

    @Test
    void multiplePackages() {
        var roots = compileValidProject(Arrays.asList(KIRRA_PACKAGE, UI_PACKAGE), "");
        assertEquals(1, roots.size());
        assertNull(roots.get(0).getRootObject());
        assertEquals(0, roots.get(0).getProblems().size());
    }
}
