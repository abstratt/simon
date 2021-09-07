package com.abstratt.simon.compiler.antlr;

import com.abstratt.simon.compiler.AbortCompilationException;
import com.abstratt.simon.compiler.CompilerException;
import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.backend.Linking;
import com.abstratt.simon.compiler.backend.Parenting;
import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.metamodel.Metamodel.BasicType;
import com.abstratt.simon.metamodel.Metamodel.Composition;
import com.abstratt.simon.metamodel.Metamodel.Enumerated;
import com.abstratt.simon.metamodel.Metamodel.Feature;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Primitive;
import com.abstratt.simon.metamodel.Metamodel.RecordType;
import com.abstratt.simon.metamodel.Metamodel.Reference;
import com.abstratt.simon.metamodel.Metamodel.Slot;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.metamodel.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.FeatureNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.LanguageDeclarationContext;
import com.abstratt.simon.parser.antlr.SimonParser.LinkContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.PropertiesContext;
import com.abstratt.simon.parser.antlr.SimonParser.RecordLiteralContext;
import com.abstratt.simon.parser.antlr.SimonParser.RootObjectContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

class SimonBuilder<T> extends SimonBaseListener {

    class ElementInfo {
        private String sourceName;
        private final T object;
        private final Slotted type;

        public ElementInfo(String sourceName, T object, Slotted type) {
            this.object = object;
            this.type = type;
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

        public ResolutionRequest(ParserRuleContext context, String source, T scope, String name, Resolver<T> resolver) {
            this.source = source;
            this.context = context;
            this.scope = scope;
            this.name = name;
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

    public SimonBuilder(
            Problem.Handler problemHandler,
            MetamodelSource metamodelSource,
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
    public void enterEveryRule(ParserRuleContext ctx) {
        assert sourceName != null;
    }

    public void startSource(String sourceName) {
        assert this.sourceName == null;
        this.sourceName = sourceName;
    }

    public void endSource(String sourceName) {
        assert sourceName.equals(this.sourceName);
        this.sourceName = null;
    }

    public List<T> build() {
    	assert currentScope.isEmpty();
    	var partialResult = built.stream().map(ElementInfo::getObject).collect(Collectors.toList());
    	built.clear();
		return partialResult;
    }

    void resolve() {
        for (ResolutionRequest request : resolutionRequests)
            resolveRequest(request);
    }

    private void resolveRequest(ResolutionRequest request) {
        String name = request.name;
        Resolver<T> resolver = request.resolver;
        T scope = request.scope;
        String[] nameComponents = name.contains(".") ? name.split("\\.") : new String[]{name};
        T resolved = modelHandling.nameResolution().resolve(scope, nameComponents);
        if (resolved != null) {
            resolver.resolve(resolved);
        } else {
            reportError(Problem.Severity.Error, request.getSource(), request.getLine(), request.getColumn(), "Unknown name: '" + name + "'");
        }
    }

    @Override
    public void exitLanguageDeclaration(LanguageDeclarationContext ctx) {
    	// TODO Auto-generated method stub
    	super.exitLanguageDeclaration(ctx);
    }
    
    @Override
    public void exitObjectHeader(ObjectHeaderContext ctx) {
        var object = ctx.objectClass();
        var typeName = getTypeName(object);
        var resolvedType = metamodelSource.resolveType(StringUtils.capitalize(typeName));
        var asObjectType = (ObjectType) resolvedType;
        if (asObjectType == null)
            throw new CompilerException("Unknown type: " + typeName + " on line " + object.start.getLine());
        if (!asObjectType.isInstantiable())
            throw new CompilerException("Type: " + typeName + " not instantiable on line " + object.start.getLine());
        var instantiation = modelHandling.instantiation();
        var created = (T) instantiation.createObject(asObjectType.isRoot(), asObjectType);
        var objectName = ctx.objectName();
        if (objectName != null)
            modelHandling.nameSetting().setName(created, getIdentifier(objectName));
        newScope(asObjectType, created);
    }
    
    @Override
    public void exitRootObject(RootObjectContext ctx) {
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
        resolutionRequests.add(new ResolutionRequest(context, sourceName, this.currentScope().get().getObject(), name, resolver));
    }

    @Override
    public void exitComponent(ComponentContext ctx) {
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
            reportError(Problem.Severity.Fatal, sourceName, ctx.featureName(), "No feature '" + getIdentifier(ctx.featureName()) +"' found on " + parentInfo.getType().name());
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
            reportError(Problem.Severity.Fatal, featureOwnerInfo.getSourceName(), featureNameCtx, "This type cannot have components: " + parentType.name());
        ObjectType parentTypeAsObjectType = (ObjectType) parentType;
        String featureName = getIdentifier(featureNameCtx);
        F feature = getter.apply(parentTypeAsObjectType, featureName);
        if (feature == null)
            reportError(Problem.Severity.Fatal, featureOwnerInfo.getSourceName(), featureNameCtx, "No feature '" + featureName + "' in " + parentType.name());
        return feature;
    }

    private Problem reportError(Problem.Severity severity, String sourceName, ParserRuleContext ctx, String message) {
        int line = ctx.getStart().getLine();
        int column = ctx.getStart().getCharPositionInLine();
        return reportError(severity, sourceName, line, column, message);
    }

    Problem reportError(Problem.Severity severity, String source, int line, int column, String message) {
        var problem = new Problem(source, line, column, message, severity);
        problemHandler.handleProblem(problem);
        if (severity == Problem.Severity.Fatal)
            throw new AbortCompilationException();
        return problem;
    }

    @Override
    public void exitRecordLiteral(RecordLiteralContext ctx) {
        // take the record out of the stack
        debug("Removing record", currentScope());
        dropScope();
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
            throw new CompilerException("Unknown property: " + propertyName + " in type " + asSlotted.name() + " - slots are: " + slotNames);
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
    public void exitSlot(SlotContext ctx) {
        ElementInfo info = currentScope().get();

        T target = info.getObject();
        parseSlotValue(ctx, info.getType(), (slot, value) -> modelHandling.valueSetting().setValue(slot, target, value));
    }

    private void parseSlotValue(SlotContext ctx, Slotted slotOwner, BiConsumer<Slot, Object> consumer) {
        String propertyName = getIdentifier(ctx.featureName());
        Slot slot = slotOwner.slotByName(propertyName);
        if (slot == null) {
            List<String> slotNames = slotOwner.slots().stream().map(Slot::name).collect(Collectors.toList());
            throw new CompilerException("Unknown property: " + propertyName + " in element " + slotOwner.name() + " - slots are: " + slotNames);
        }

        Object slotValue = buildValue(ctx, slot);
        consumer.accept(slot, slotValue);
    }

    private Object buildValue(SlotContext ctx, Slot slot) {
        Object slotValue;
        BasicType slotType = slot.type();
        if (slotType instanceof Primitive) {
            slotValue = parsePrimitiveLiteral((Primitive) slotType, ctx.slotValue().literal().getText());
        } else if (slotType instanceof Enumerated) {
            slotValue = parseEnumeratedLiteral((Enumerated) slotType, ctx.slotValue().literal().getText());
        } else if (slotType instanceof RecordType) {
            RecordType slotTypeAsRecordType = (RecordType) slotType;
            slotValue = parseRecordLiteral(slotTypeAsRecordType, ctx.slotValue().literal().recordLiteral());
        } else {
            throw new IllegalStateException("Unsupported basic type: " + slotType.name());
        }
        return slotValue;
    }

    private Object parseRecordLiteral(RecordType recordType, RecordLiteralContext recordLiteralContext) {
        T newRecord = modelHandling.instantiation().createObject(recordType.isRoot(), recordType);
        PropertiesContext properties = recordLiteralContext.properties();
        List<SlotContext> slots = properties.slot();
        for (SlotContext slotContext : slots) {
            parseSlotValue(slotContext, recordType, (Slot slot, Object newValue) -> modelHandling.valueSetting()
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
        ElementInfo newInfo = new ElementInfo(sourceName, created, type);
        String name = modelHandling.nameQuerying().getName(newInfo.getObject());
        currentScope.push(debug("Pushing (" + currentScope.size() + ")" + name, newInfo));
    }

    public void addImport(String importPath) {
    	imports.add(importPath);
    }
    
    public List<String> collectImports() {
        var result = new ArrayList<>(imports);
        imports.clear();
        return result;
	}
}
