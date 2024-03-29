package com.abstratt.simon.compiler.antlr.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import com.abstratt.simon.compiler.AbortCompilationException;
import com.abstratt.simon.compiler.CompilerException;
import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Problem.Category;
import com.abstratt.simon.compiler.Problem.Severity;
import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.backend.Linking;
import com.abstratt.simon.compiler.backend.MetamodelException;
import com.abstratt.simon.compiler.backend.Parenting;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.metamodel.Metamodel.BasicType;
import com.abstratt.simon.metamodel.Metamodel.Composition;
import com.abstratt.simon.metamodel.Metamodel.Enumerated;
import com.abstratt.simon.metamodel.Metamodel.Feature;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Primitive;
import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;
import com.abstratt.simon.metamodel.Metamodel.RecordType;
import com.abstratt.simon.metamodel.Metamodel.Reference;
import com.abstratt.simon.metamodel.Metamodel.Slot;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.metamodel.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ChildObjectContext;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.FeatureNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.LanguageDeclarationContext;
import com.abstratt.simon.parser.antlr.SimonParser.LinkContext;
import com.abstratt.simon.parser.antlr.SimonParser.ModifierContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.PropertiesContext;
import com.abstratt.simon.parser.antlr.SimonParser.RecordLiteralContext;
import com.abstratt.simon.parser.antlr.SimonParser.RootObjectContext;
import com.abstratt.simon.parser.antlr.SimonParser.SimpleIdentifierContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotValueContext;

class SimonBuilder<T> extends SimonBaseListener {

    class ElementInfo {
        private String sourceName;
        /** The object this element represents. */
        private final T object;
        /** The type of the object. */
        private final Slotted type;
        private List<ModifierContext> modifiers;

        public ElementInfo(String sourceName, T object, Slotted type, List<ModifierContext> modifiers) {
            this.object = object;
            this.type = type;
            this.modifiers = modifiers;
        }

        public Slotted getType() {
            return type;
        }

        public T getObject() {
            return object;
        }

        public String getSourceName() {
            return sourceName;
        }

        @Override
        public String toString() {
            return modelHandling.nameQuerying().getName(object) + " : " + type.name();
        }

        public Slotted getSlotted() {
            return type;
        }
        public List<ModifierContext> getModifiers() {
			return modifiers;
		}
    }

    private final Problem.Handler problemHandler;
    private final Backend<ObjectType, Slotted, T> modelHandling;
    private final MetamodelSource<?> metamodelSource;
    /**
     * Scopes can be nested.
     */
    private final Deque<ElementInfo> currentScope = new LinkedList<>();
    private final List<ElementInfo> built = new LinkedList<>();
    private final List<ResolutionRequest> resolutionRequests = new LinkedList<>();
    private final List<String> imports = new ArrayList<>();
    private final List<ModifierContext> availableModifiers = new ArrayList<>();
    private Set<String> languages;
    private String sourceName;

    interface Resolver<R> {
        void resolve(R resolved);
    }

    class ResolutionRequest {
        private final ParserRuleContext context;
        private final T scope;
        private final String name;
        private final Resolver<T> resolver;
        private final String source;
        private Type expectedType;
        private final Set<String> languages;

        public ResolutionRequest(ParserRuleContext context, String source, T scope, String name, Set<String> languages,
                Resolver<T> resolver) {
            this.source = source;
            this.context = context;
            this.scope = scope;
            this.name = name;
            this.languages = languages;
            this.resolver = resolver;
        }

        public String getSource() {
            return source;
        }

        int getLine() {
            return context.start.getLine();
        }

        int getColumn() {
            return context.start.getCharPositionInLine();
        }
    }

    public SimonBuilder(Problem.Handler problemHandler, MetamodelSource metamodelSource,
            Backend<? extends ObjectType, ? extends Slotted, T> modelHandling) {
        this.problemHandler = problemHandler;
        this.metamodelSource = metamodelSource;
        this.modelHandling = (Backend<ObjectType, Slotted, T>) modelHandling;
    }

    @Override
    public void exitImportDeclaration(com.abstratt.simon.parser.antlr.SimonParser.ImportDeclarationContext ctx) {
        var source = getCharLiteral(ctx);
        addImport(source);
    }

    @Override
    public void exitLanguageDeclaration(LanguageDeclarationContext ctx) {
        var language = ctx.IDENT().getText();
        addLanguage(language);
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        assert sourceName != null;
    }

    public void startSource(String sourceName) {
        assert this.sourceName == null;
        this.currentScope.clear();
        this.sourceName = sourceName;
        this.languages = new LinkedHashSet<>();
    }

    public void endSource(String sourceName) {
        assert sourceName.equals(this.sourceName);
        this.sourceName = null;
        this.languages = null;
    }

    public List<T> buildUnit() {
        if (hasFatalError())
            return Collections.emptyList();
        assert currentScope.isEmpty();
        var partialResult = built.stream().map(ElementInfo::getObject).collect(Collectors.toList());
        built.clear();
        return partialResult;
    }

    public boolean hasFatalError() {
        return problemHandler.hasFatalError();
    }

    void resolve() {
        for (ResolutionRequest request : resolutionRequests)
            resolveRequest(request);
    }

    private void resolveRequest(ResolutionRequest request) {
        String name = request.name;
        Resolver<T> resolver = request.resolver;
        T scope = request.scope;
        String[] nameComponents = name.contains(".") ? name.split("\\.") : new String[] { name };
        T resolved = modelHandling.nameResolution().resolve(scope, nameComponents);
        if (resolved != null) {
            try {
                resolver.resolve(resolved);
            } catch (MetamodelException e) {
                reportError(Severity.Error, Category.TypeError, request.getSource(), request.getLine(),
                        request.getColumn(), e.getMessage());
            }
        } else {
            reportError(Severity.Error, Category.UnresolvedName, request.getSource(), request.getLine(),
                    request.getColumn(), "Unknown name: '" + name + "'");
        }
    }

    @Override
    public void enterRootObject(RootObjectContext ctx) {
        if (languages.isEmpty())
            reportError(Severity.Fatal, Category.MissingElement, sourceName, ctx, "No languages defined");
    }

    @Override
    public void exitObjectHeader(ObjectHeaderContext ctx) {
        var object = ctx.objectClass();
        var typeName = getTypeName(object);
        var resolvedType = metamodelSource.resolveType(StringUtils.capitalize(typeName), languages);
        var asObjectType = (ObjectType) resolvedType;
        if (asObjectType == null)
            reportError(Severity.Fatal, Category.UnknownElement, sourceName, ctx,
                    "Unknown language element: " + typeName);
        if (!asObjectType.isInstantiable())
            reportError(Severity.Fatal, Category.AbstractElement, sourceName, ctx,
                    "Language element not instantiable: " + typeName);
        var instantiation = modelHandling.instantiation();
        var created = (T) instantiation.createObject(asObjectType.isRoot(), asObjectType);
        var objectName = ctx.objectName();
        if (objectName != null)
            modelHandling.nameSetting().setName(created, getIdentifier(objectName));
        newScope(asObjectType, created);
    }

    @Override
    public void exitRootObject(RootObjectContext ctx) {
        if (hasFatalError())
            return;
        if (currentScope.isEmpty())
            return;
        assert currentScope.size() == 1;
        var lastScope = dropScope();
        built.add(lastScope);
    }

    private String getTypeName(ObjectClassContext object) {
        String text = object.getText();
        if (text.indexOf('.') < 0)
            return text;
        String[] components = text.split("\\.");
        String typeName = components[0] + '.' + components[1];
        return typeName;
    }

    private String getIdentifier(ParserRuleContext object) {
        TerminalNode token = object.getToken(SimonParser.IDENT, 0);
        return token.getText();
    }

    private String getCharLiteral(ParserRuleContext object) {
        TerminalNode token = object.getToken(SimonParser.CHAR_LITERAL, 0);
        String text = token.getText();
        return text.substring(1, text.length() - 1);
    }

    @Override
    public void exitLink(LinkContext ctx) {
        ElementInfo parentInfo = currentScope().get();
        FeatureNameContext featureNameCtx = ctx.featureName();
        Reference reference = getObjectFeature(parentInfo, featureNameCtx, ObjectType::referenceByName);
        Linking<T, Reference> linking = modelHandling.linking();
        String nameToResolve = ctx.objectNameRef().getText();
        requestResolution(nameToResolve, resolved -> linking.link(reference, parentInfo.object, resolved),
                ctx.objectNameRef());
    }

    private void requestResolution(String name, Resolver<T> resolver, ParserRuleContext context) {
        resolutionRequests.add(new ResolutionRequest(context, sourceName, this.currentScope().get().getObject(), name,
                languages, resolver));
    }

    @Override
    public void exitComponent(ComponentContext ctx) {
        if (hasFatalError())
            return;
        int childCount = ctx.childObjects().getChildCount();
        List<T> components = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            SimonBuilder<T>.ElementInfo last = dropScope();
            T child = last.getObject();
            debug("Collecting child to add: ", last);
            components.add(0, child);
        }
        ElementInfo parentInfo = currentScope().get();
        Parenting<T, Composition> parenting = modelHandling.parenting();
        Composition composition = getObjectFeature(parentInfo, ctx.featureName(), ObjectType::compositionByName);
        if (composition == null) {
            reportError(Severity.Fatal, Category.MissingFeature, sourceName, ctx.featureName(),
                    "No feature '" + getIdentifier(ctx.featureName()) + "' found on " + parentInfo.getType().name());
            return;
        }
        for (T child : components) {
            parenting.addChild(composition, parentInfo.getObject(), child);
        }
    }

    private <F extends Feature<ObjectType>> F getObjectFeature(ElementInfo featureOwnerInfo,
            ParserRuleContext featureNameCtx, BiFunction<ObjectType, String, F> getter) {
        Slotted parentType = featureOwnerInfo.getType();
        if (!(parentType instanceof ObjectType))
            reportError(Severity.Fatal, Category.ElementAdmitsNoFeatures, featureOwnerInfo.getSourceName(),
                    featureNameCtx, "This type cannot have components: " + parentType.name());
        ObjectType parentTypeAsObjectType = (ObjectType) parentType;
        String featureName = getIdentifier(featureNameCtx);
        F feature = getter.apply(parentTypeAsObjectType, featureName);
        if (feature == null)
            reportError(Severity.Fatal, Category.MissingFeature, featureOwnerInfo.getSourceName(), featureNameCtx,
                    "No feature '" + featureName + "' in " + parentType.name());
        return feature;
    }

    private Problem reportError(Severity severity, Category category, String sourceName, ParserRuleContext ctx,
            String message) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return reportError(severity, category, sourceName, line, column, message);
    }

    Problem reportError(Severity severity, Category category, String source, int line, int column, String message) {
        var problem = new Problem(source, line, column, message, severity, category);
        problemHandler.handleProblem(problem);
        if (severity == Severity.Fatal) {
            throw new AbortCompilationException();
        }
        return problem;
    }

    @Override
    public void exitRecordLiteral(RecordLiteralContext ctx) {
        // take the record out of the stack
        debug("Removing record", currentScope());
        dropScope();
    }
    
    private List<ModifierContext> consumeModifiers() {
    	var snapshot = new ArrayList<>(this.availableModifiers);
    	availableModifiers.clear();
    	System.out.println("Modifiers: " + snapshot);
    	return snapshot; 
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    	currentScope().ifPresent(info -> applyModifiers(ctx, info));
    }

	private void applyModifiers(ParserRuleContext ctx, SimonBuilder<T>.ElementInfo info) {
		T target = info.getObject();
		var modifiers = info.getModifiers();
		modifiers.forEach(it -> applyModifier(ctx, info, it));
	}
	
	private void applyModifier(ParserRuleContext ctx, SimonBuilder<T>.ElementInfo info, ModifierContext modifier) {
		FeatureNameProvider<ModifierContext, SimpleIdentifierContext> featureNameProvider = ModifierContext::simpleIdentifier;
		SlotValueBuilder<ModifierContext> valueBuilder = this::buildModifierValue;
    	parseSlotValue(modifier, info.getType(), featureNameProvider, valueBuilder, (Slot slot, Object value) -> modelHandling.valueSetting().setValue(slot, info.getObject(), value));
	}


	@Override
    public void enterRecordLiteral(RecordLiteralContext ctx) {
        SlotContext slotContext = findParent(ctx, SlotContext.class);
        ElementInfo info = currentScope().get();

        String propertyName = getIdentifier(slotContext.featureName());
        Slotted asSlotted = info.type;
        Slot slot = asSlotted.slotByName(propertyName);
        if (slot == null) {
            List<String> slotNames = asSlotted.slots().stream().map(Slot::name).collect(Collectors.toList());
            throw new CompilerException("Unknown property: " + propertyName + " in type " + asSlotted.name()
                    + " - slots are: " + slotNames);
        }
        if (!(slot.type() instanceof RecordType)) {
            // no instance required
            return;
        }
        RecordType asRecordType = (RecordType) slot.type();
        T created = modelHandling.instantiation().createObject(asRecordType.isRoot(), asRecordType);
        newScope(asRecordType, created);
    }

    <PRC extends ParserRuleContext> PRC findParent(ParserRuleContext current, Class<? extends PRC> parentType) {
        ParserRuleContext candidate = current.getParent();
        while (candidate != null && !parentType.isInstance(candidate)) {
            candidate = candidate.getParent();
        }
        return (PRC) candidate;
    }
    
    @Override
    public void exitModifier(ModifierContext ctx) {
    	availableModifiers.add(ctx);
    }

    @Override
    public void exitSlot(SlotContext ctx) {
        var info = currentScope().get();
        T target = info.getObject();
        parseSlotValue(ctx, info.getType(), SlotContext::featureName, this::buildSlotValue, (slot, value) -> modelHandling.valueSetting().setValue(slot, target, value));
    }

    private <CTX extends ParserRuleContext, FCTX extends ParserRuleContext> void parseSlotValue(CTX ctx, Slotted slotOwner, FeatureNameProvider<CTX, FCTX> featureName, SlotValueBuilder<CTX> valueBuilder, BiConsumer<Slot, Object> valueConsumer) {
		var slot = getSlotByFeatureName(slotOwner, featureName.get(ctx));
        Object slotValue = valueBuilder.build((CTX) ctx, slot.type());
        valueConsumer.accept(slot, slotValue);
    }
    
    interface SlotValueBuilder<CTX extends ParserRuleContext> {
    	Object build(CTX context, BasicType slotType);
    }
    
    interface FeatureNameProvider<CTX extends ParserRuleContext, FCTX extends ParserRuleContext> {
    	FCTX get(CTX context);
    }

	private Slot getSlotByFeatureName(Slotted slotOwner, ParserRuleContext slotName) {
		var propertyName = getIdentifier(slotName);
        var slot = getSlotByName(slotOwner, propertyName);
		return slot;
	}

	private Slot getSlotByName(Slotted slotOwner, String propertyName) {
		var slot = slotOwner.slotByName(propertyName);
        if (slot == null) {
            List<String> slotNames = slotOwner.slots().stream().map(Slot::name).collect(Collectors.toList());
            throw new CompilerException("Unknown property: " + propertyName + " in element " + slotOwner.name()
                    + " - slots are: " + slotNames);
        }
		return slot;
	}

	private Object buildSlotValue(SlotContext ctx, BasicType slotType) {
		Object slotValue;
        SlotValueContext slotValueContext = ctx.slotValue();
		if (slotType instanceof Primitive) {
            slotValue = parsePrimitiveLiteral((Primitive) slotType, slotValueContext.literal().getText());
        } else if (slotType instanceof Enumerated) {
            slotValue = parseEnumeratedLiteral((Enumerated) slotType, slotValueContext.literal().getText());
        } else if (slotType instanceof RecordType) {
            RecordType slotTypeAsRecordType = (RecordType) slotType;
            slotValue = parseRecordLiteral(slotTypeAsRecordType, slotValueContext.literal().recordLiteral());
        } else {
            throw new IllegalStateException("Unsupported basic type: " + slotType.name());
        }
        return slotValue;
	}
	
	private Object buildModifierValue(ModifierContext ctx, BasicType slotType) {
		Object slotValue;
		if (slotType instanceof Primitive && ((Primitive) slotType).kind() == PrimitiveKind.Boolean) {
			slotValue = true;
        } else if (slotType instanceof Enumerated) {
            slotValue = parseEnumeratedLiteral((Enumerated) slotType, ctx.simpleIdentifier().getText());
        } else {
        	throw new IllegalStateException("Unsupported basic type for a modifier: " + slotType.name() + " (only booleans/enums allowed)");
        }
        return slotValue;
	}
	
    private Object parseRecordLiteral(RecordType recordType, RecordLiteralContext recordLiteralContext) {
        T newRecord = modelHandling.instantiation().createObject(recordType.isRoot(), recordType);
        PropertiesContext properties = recordLiteralContext.properties();
        List<SlotContext> slots = properties.slot();
        for (SlotContext slotContext : slots) {
            parseSlotValue(slotContext, recordType, SlotContext::featureName, this::buildSlotValue, (Slot slot, Object newValue) -> modelHandling.valueSetting()
                    .setValue(slot, newRecord, (T) newValue));
        }
        return newRecord;
    }

    private Object parseEnumeratedLiteral(Enumerated type, String valueName) {
        return type.valueForName(valueName);
    }

    private Object parsePrimitiveLiteral(Primitive slotType, String literalText) {
        return switch (slotType.kind()) {
        case Integer -> Integer.parseInt(literalText);
        case Decimal -> Double.parseDouble(literalText);
        case Boolean -> Boolean.parseBoolean(literalText);
        case String, Other -> literalText.substring(1, literalText.length() - 1);
        default -> throw new IllegalStateException();
        };
    }

    private ElementInfo dropScope() {
        return debug("pop (" + currentScope.size() + ")" + currentScope, currentScope.removeFirst());
    }

    private <Z> Z debug(String tag, Z toDebug) {
        System.out.println("\n" + tag + ": " + toDebug);
        return toDebug;
    }

    private Optional<ElementInfo> currentScope() {
        return debug("peekFirst (" + currentScope.size() + ")", Optional.ofNullable(currentScope.peekFirst()));
    }

    private void newScope(Slotted type, T created) {
        ElementInfo newInfo = new ElementInfo(sourceName, created, type, consumeModifiers());
        String name = modelHandling.nameQuerying().getName(newInfo.getObject());
        currentScope.push(debug("Pushing (" + currentScope.size() + ")" + name, newInfo));
    }

    public void addImport(String importPath) {
        imports.add(importPath);
    }

    public void addLanguage(String language) {
        languages.add(language);
    }

    public List<String> collectImports() {
        var result = new ArrayList<>(imports);
        imports.clear();
        return result;
    }

    public Problem.Handler getProblemHandler() {
        return problemHandler;
    }
}
