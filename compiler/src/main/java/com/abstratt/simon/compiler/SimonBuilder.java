package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.BasicEMap;
import org.eclipse.emf.common.util.EMap;

import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.Metamodel.BasicType;
import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.Enumerated;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Primitive;
import com.abstratt.simon.compiler.Metamodel.RecordType;
import com.abstratt.simon.compiler.Metamodel.Slot;
import com.abstratt.simon.compiler.Metamodel.Slotted;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.RecordLiteralContext;
import com.abstratt.simon.parser.antlr.SimonParser.ScopedRootObjectsContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotNameContext;

public class SimonBuilder<T> extends SimonBaseListener {

	static class ElementInfo<T> {
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
			return object + " : " + type.name();
		}

		public Slotted getSlotted() {
			return type;
		}
	}

	private Configuration.Provider<ObjectType, Slotted, T> configurationProvider;
	private TypeSource<?> typeSource;
	private Deque<ElementInfo<T>> currentScope = new LinkedList<>();
	private List<Problem> problems = new LinkedList<>();

	public SimonBuilder(TypeSource typeSource, Configuration.Provider<? extends ObjectType,? extends Slotted, T> configurationProvider) {
		this.typeSource = typeSource;
		this.configurationProvider = (Provider<ObjectType, Slotted, T>) configurationProvider;
	}

	public T build() {
		if (currentScope.size() != 1) {
			throw new IllegalStateException("Unexpected stack size: " + currentScope.size());
		}
		return pop().getObject();
	}
	
	@Override
	public void exitScopedRootObjects(ScopedRootObjectsContext ctx) {
		if (abort()) {
			return;
		}
		String packageName = getIdentifier(ctx.languageName());
		typeSource.use(packageName);
	}
	
	private boolean abort() {
		return !problems.isEmpty();
	}

	@Override
	public void exitObjectHeader(ObjectHeaderContext ctx) {
		if (abort()) {
			return;
		}
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
			configurationProvider.naming().setName(created, getIdentifier(objectName));
		}
		push(asObjectType, created);
	}

	private String getTypeName(ObjectClassContext object) {
		String text = object.getText();
		if (text.indexOf('.') < 0) return StringUtils.capitalize(text);
		String[] components = text.split("\\.");
		String typeName = components[0] + '.' + StringUtils.capitalize(components[1]);
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
	public void exitComponent(ComponentContext ctx) {
		if (abort()) {
			return;
		}
		int childCount = ctx.childObjects().getChildCount();
		List<T> components = new ArrayList<>(childCount);
		for (int i = 0; i < childCount; i++) {
			components.add(0, pop().getObject());
		}
		ElementInfo<T> parentInfo = peekFirst().get();
		T parent = parentInfo.getObject();
		Slotted parentType = parentInfo.getType();
		if (!(parentType instanceof ObjectType)) {
			throw new CompilerException("This type cannot have components: " + parentType.name());
		}
		ObjectType parentTypeAsObjectType = (ObjectType) parentType;
		String compositionName = getIdentifier(ctx.compositionName());
		Composition composition = parentTypeAsObjectType.compositionByName(compositionName);
		Parenting<T, Composition> parenting = configurationProvider.parenting();
		for (T child : components) {
			parenting.addChild(composition, parent, child);
		}
		super.exitComponent(ctx);
	}
	
	@Override
	public void exitRecordLiteral(RecordLiteralContext ctx) {
		if (abort()) {
			return;
		}
		// take the record out of the stack
		pop();
	}
	
	@Override
	public void enterRecordLiteral(RecordLiteralContext ctx) {
		SlotContext slotContext = findParent(ctx, SlotContext.class);
		if (abort()) {
			return;
		}
		ElementInfo<T> info = peekFirst().get();
		
		String propertyName = getIdentifier(slotContext.slotName());
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
	public void exitSlotName(SlotNameContext ctx) {

	}
	
	@Override
	public void exitSlot(SlotContext ctx) {
		if (abort()) {
			return;
		}
		ElementInfo<T> info = peekFirst().get();
		
		T target = info.getObject();
		parseSlotValue(ctx, info.getType(), (slot, value) -> {
			configurationProvider.valueSetting().setValue(slot, target, value);
		});
	}

	private void parseSlotValue(SlotContext ctx, Slotted slotOwner, BiConsumer<Slot, Object> consumer) {
		String propertyName = getIdentifier(ctx.slotName());
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
		List<SlotContext> slots = recordLiteralContext.properties().slot();
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
		default: throw new IllegalStateException();
		}
		return slotValue;
	}

	
	private ElementInfo<T> pop() {
		return debug("pop", currentScope.removeFirst());
	}

	private <Z> Z debug(String tag, Z toDebug) {
		System.out.println(tag +": " + toDebug);
		return toDebug;
	}

	private Optional<ElementInfo<T>> peekFirst() {
		return debug("peekFirst", Optional.ofNullable(currentScope.peekFirst()));
	}
	
	private void push(Slotted type, T created) {
		ElementInfo<T> newInfo = new ElementInfo<>(created, type);
		currentScope.push(debug("Pushing", newInfo));
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
}
