package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Linking;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.Enumerated;
import com.abstratt.simon.compiler.Metamodel.Feature;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Primitive;
import com.abstratt.simon.compiler.Metamodel.RecordType;
import com.abstratt.simon.compiler.Metamodel.Reference;
import com.abstratt.simon.compiler.Metamodel.Slot;
import com.abstratt.simon.compiler.Metamodel.Slotted;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.FeatureNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.LinkContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.PropertiesContext;
import com.abstratt.simon.parser.antlr.SimonParser.RecordLiteralContext;
import com.abstratt.simon.parser.antlr.SimonParser.ScopedRootObjectsContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;

public class SimonBuilder<T> extends SimonBaseListener {

	class ElementInfo {
		private T object;
		private Slotted type;
		public ElementInfo(T object, Slotted type) {
			this.object = object;
			this.type = type;
		}
		
		public Slotted getType() {
			return type;
		}
		
		public T getObject() {
			return object;
		}
		@Override
		public String toString() {
			return configurationProvider.nameQuerying().getName(object) + " : " + type.name();
		}

		public Slotted getSlotted() {
			return type;
		}
	}

	private Configuration.Provider<ObjectType, Slotted, T> configurationProvider;
	private TypeSource<?> typeSource;
	private Deque<ElementInfo> currentScope = new LinkedList<>();
	private List<Problem> problems = new LinkedList<>();
	private List<ResolutionRequest> resolutionRequests = new LinkedList<>();
	
	interface Resolver<R> {
		void resolve(R resolved);
	}
	
	class ResolutionRequest {
		private final ParserRuleContext context;
		private final T scope;
		private final String name;
		private final Resolver<T> resolver;
		private Type expectedType;
		public ResolutionRequest(ParserRuleContext context, T scope, String name, Resolver<T> resolver) {
			this.context = context;
			this.scope = scope;
			this.name = name;
			this.resolver = resolver;
		}
		int getLine() {
			return context.start.getLine();
		}
		int getColumn() {
			return context.start.getCharPositionInLine();
		}
	}

	public SimonBuilder(TypeSource typeSource, Configuration.Provider<? extends ObjectType,? extends Slotted, T> configurationProvider) {
		this.typeSource = typeSource;
		this.configurationProvider = (Provider<ObjectType, Slotted, T>) configurationProvider;
	}

	public T build() {
		checkAbort();
		resolve();
		return currentScope.getFirst().getObject();
	}
	
	private void resolve() {
		for (ResolutionRequest request : resolutionRequests) {
			String name = request.name;
			Resolver<T> resolver = request.resolver;
			T scope = request.scope;
			T resolved = configurationProvider.nameResolution().resolve(scope, name);
			if (resolved != null) {
				resolver.resolve(resolved);
			} else {
				reportError(false, request.getLine(), request.getColumn(), "Unknown name: '" + name + "'");
			}
		}
	}

	@Override
	public void exitScopedRootObjects(ScopedRootObjectsContext ctx) {
		String packageName = getIdentifier(ctx.languageName());
		typeSource.use(packageName);
	}
	
	boolean abort() {
		return !problems.isEmpty();
	}

	@Override
	public void exitObjectHeader(ObjectHeaderContext ctx) {
		ObjectClassContext object = ctx.objectClass();
		String typeName = getTypeName(object);
		Type resolvedType = typeSource.resolveType(typeName);
		ObjectType asObjectType = (ObjectType) resolvedType;
		if (asObjectType == null) {
			throw new CompilerException("Unknown type: " + typeName);
		}
		if (!asObjectType.isRoot()) {
			throw new CompilerException("Not a root type: " + typeName);
		}
		Instantiation<ObjectType> instantiation = configurationProvider.instantiation();
		T created = instantiation.createObject(asObjectType);
		ObjectNameContext objectName = ctx.objectName();
		if (objectName != null) {
			configurationProvider.nameSetting().setName(created, getIdentifier(objectName));
		}
		push(asObjectType, created);
	}
	
	private String getTypeName(ObjectClassContext object) {
		String text = object.getText();
		if (text.indexOf('.') < 0) return text;
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
		ElementInfo parentInfo = peek().get();
		FeatureNameContext featureNameCtx = ctx.featureName();
		Reference reference = getObjectFeature(parentInfo, featureNameCtx, ObjectType::referenceByName);
		Linking<T, Reference> linking = configurationProvider.linking();
		String nameToResolve = ctx.objectNameRef().getText();
		addResolver(nameToResolve, resolved -> linking.link(reference, parentInfo.object, resolved), ctx.objectNameRef());
	}
	
	private void addResolver(String name, Resolver<T> resolver, ParserRuleContext context) {
		resolutionRequests.add(new ResolutionRequest(context, currentScope.peek().getObject(), name, resolver));
	}

	@Override
	public void exitComponent(ComponentContext ctx) {
		int childCount = ctx.childObjects().getChildCount();
		List<T> components = new ArrayList<>(childCount);
		for (int i = 0; i < childCount; i++) {
			SimonBuilder<T>.ElementInfo last = pop();
			T child = last.getObject();
			debug("Collecting child to add: ", last);
			components.add(0, child);
		}
		ElementInfo parentInfo = peek().get();
		Parenting<T, Composition> parenting = configurationProvider.parenting();
		Composition composition = getObjectFeature(parentInfo, ctx.featureName(), ObjectType::compositionByName);
		for (T child : components) {
			parenting.addChild(composition, parentInfo.getObject(), child);
		}
	}

	private <F extends Feature<ObjectType>> F getObjectFeature(ElementInfo featureOwnerInfo, ParserRuleContext featureNameCtx, BiFunction<ObjectType, String, F> getter) {
		Slotted parentType = featureOwnerInfo.getType();
		if (!(parentType instanceof ObjectType)) {
			reportFatalError(featureNameCtx, "This type cannot have components: " + parentType.name());
		}
		ObjectType parentTypeAsObjectType = (ObjectType) parentType;
		String featureName = getIdentifier(featureNameCtx);
		F feature = getter.apply(parentTypeAsObjectType, featureName);
		if (feature == null) {
			reportFatalError(featureNameCtx, "No feature '" + featureName + "' in " + parentType.name());
		}
		return feature;
	}
	
	private void reportFatalError(ParserRuleContext ctx, String message) {
		reportError(true, ctx, message);
	}

	private void reportError(boolean fatal, ParserRuleContext ctx, String message) {
		int line = ctx.getStart().getLine();
		int column = ctx.getStart().getCharPositionInLine();
		reportError(fatal, line, column, message);
	}

	void reportError(boolean fatal, int line, int column, String message) {
		addProblem(new Problem(line, column, message));
		if (fatal) {
			throw new AbortCompilationException();
		}
	}

	@Override
	public void exitRecordLiteral(RecordLiteralContext ctx) {
		// take the record out of the stack
		debug("Removing record", peek());
		pop();
	}
	
	@Override
	public void enterRecordLiteral(RecordLiteralContext ctx) {
		SlotContext slotContext = findParent(ctx, SlotContext.class);
		ElementInfo info = peek().get();
		
		String propertyName = getIdentifier(slotContext.featureName());
		Slotted asSlotted = (Slotted) info.type;
		Slot slot = asSlotted.slotByName(propertyName);
		if (slot == null) {
			throw new CompilerException("Unknown property: " + propertyName + " in type " + info.type.name());
		}
		if (!(slot.type() instanceof RecordType)) {
			// no instance required
			return;
		}
		RecordType asRecordType = (RecordType) slot.type(); 
		T created = configurationProvider.instantiation().<T>createObject(asRecordType);
		push(asRecordType, created);		
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
		ElementInfo info = peek().get();
		
		T target = info.getObject();
		parseSlotValue(ctx, info.getType(), (slot, value) -> {
			configurationProvider.valueSetting().setValue(slot, target, value);
		});
	}

	private void parseSlotValue(SlotContext ctx, Slotted slotOwner, BiConsumer<Slot, Object> consumer) {
		String propertyName = getIdentifier(ctx.featureName());
		Slot slot = slotOwner.slotByName(propertyName);
		if (slot == null) {
			throw new CompilerException("Unknown property: " + propertyName + " in type " + slotOwner.name());
		}
		
		Object slotValue = buildValue(ctx, slot);
		consumer.accept(slot, slotValue);
	}

	private Object buildValue(SlotContext ctx, Slot slot) {
		Object slotValue;
		if (slot.type() instanceof Primitive) {
			slotValue = parsePrimitiveLiteral((Primitive) slot.type(), ctx.slotValue().literal().getText());
		} else if (slot.type() instanceof Enumerated) {
			slotValue = parseEnumeratedLiteral((Enumerated) slot.type(), ctx.slotValue().literal().getText());
		} else if (slot.type() instanceof RecordType) {
			RecordType slotTypeAsRecordType = (RecordType) slot.type();
			slotValue = parseRecordLiteral(slotTypeAsRecordType, ctx.slotValue().literal().recordLiteral());
		} else {
			throw new IllegalStateException("Unsupported basic type: " + slot.type().name());
		}
		return slotValue;
	}

	private Object parseRecordLiteral(RecordType recordType, RecordLiteralContext recordLiteralContext) {
		T newRecord = configurationProvider.instantiation().createObject(recordType);
		PropertiesContext properties = recordLiteralContext.properties();
		List<SlotContext> slots = properties.slot();
		for (SlotContext slotContext : slots) {
			parseSlotValue(slotContext, recordType, (Slot slot, Object newValue) -> 
				configurationProvider.valueSetting().setValue(slot, newRecord, (T) newValue)
			);
		}
		return newRecord;
	}

	private Object parseEnumeratedLiteral(Enumerated type, String valueName) {
		return type.valueForName(valueName);
	}

	private Object parsePrimitiveLiteral(Primitive slotType, String literalText) {
		Object slotValue;
		switch (slotType.kind()) {
		case Integer:
			slotValue = Integer.parseInt(literalText);
			break;
		case String:
			slotValue = literalText.substring(1, literalText.length() - 1);
			break;
		case Boolean:
			slotValue = Boolean.parseBoolean(literalText);
			break;
		default: throw new IllegalStateException();
		}
		return slotValue;
	}

	
	private ElementInfo pop() {
		return debug("pop (" + currentScope.size() +")" + currentScope, currentScope.removeFirst());
	}

	private <Z> Z debug(String tag, Z toDebug) {
		System.out.println("\n" + tag +": " + toDebug);
		return toDebug;
	}

	private Optional<ElementInfo> peek() {
		return debug("peekFirst (" + currentScope.size() +")", Optional.ofNullable(currentScope.peekFirst()));
	}
	
	private void push(Slotted type, T created) {
		ElementInfo newInfo = new ElementInfo(created, type);
		String name = configurationProvider.nameQuerying().getName(newInfo.getObject());
		currentScope.push(debug("Pushing (" + currentScope.size() +")" + currentScope, newInfo));
	}

	public void addProblem(Problem problem) {
		problems.add(problem);
	}

	public boolean hasProblems() {
		return !problems.isEmpty();
	}

	public List<Problem> getProblems() {
		return problems;
	}

	public void checkAbort() {
		if (abort()) {
			throw new AbortCompilationException();
		}
	}
}
