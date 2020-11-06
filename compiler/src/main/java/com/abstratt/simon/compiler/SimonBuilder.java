package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.Enumerated;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Primitive;
import com.abstratt.simon.compiler.Metamodel.Slot;
import com.abstratt.simon.compiler.Metamodel.Slotted;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.LanguageNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.ProgramContext;
import com.abstratt.simon.parser.antlr.SimonParser.ScopedRootObjectsContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;
import com.google.common.base.Strings;

public class SimonBuilder<T> extends SimonBaseListener {

	static class ElementInfo<T> {
		private T object;
		private ObjectType type;
		public ElementInfo(T object, ObjectType type) {
			this.object = object;
			this.type = type;
		}
		
		public ObjectType getType() {
			return type;
		}
		public T getObject() {
			return object;
		}
		@Override
		public String toString() {
			return object + " : " + type.name();
		}
	}

	private Configuration.Provider<ObjectType, T> configurationProvider;
	private Metamodel metamodel;
	private TypeSource typeSource;
	private Deque<ElementInfo<T>> currentScope = new LinkedList<>();
	private List<Problem> problems = new LinkedList<>();

	public SimonBuilder(Metamodel metamodel, TypeSource typeSource, Configuration.Provider<? extends ObjectType, T> configurationProvider) {
		this.metamodel = metamodel;
		this.typeSource = typeSource;
		this.configurationProvider = (Provider<ObjectType, T>) configurationProvider;
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
		T created = instantiation.<T>createObject(asObjectType);
		if (metamodel.isNamedObject(asObjectType)) {
			ObjectNameContext objectName = ctx.objectName();
			if (objectName != null) {
				configurationProvider.naming().setName(created, getIdentifier(objectName));
			}
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
		String compositionName = getIdentifier(ctx.compositionName());
		Composition composition = parentInfo.getType().compositionByName(compositionName);
		Parenting<T, Composition> parenting = configurationProvider.parenting();
		for (T child : components) {
			parenting.addChild(composition, parent, child);
		}
		super.exitComponent(ctx);
	}
	
	@Override
	public void exitSlot(SlotContext ctx) {
		if (abort()) {
			return;
		}
		ElementInfo<T> info = peekFirst().get();
		
		String propertyName = getIdentifier(ctx.slotName());
		Slotted asSlotted = (Slotted) info.type;
		Slot slot = asSlotted.slotByName(propertyName);
		if (slot == null) {
			throw new CompilerException("Unknown property: " + propertyName + " in type " + info.type.name());
		}
		ValueSetting<T, Slot> valueSetting = configurationProvider.valueSetting();
		
		String literalText = ctx.slotValue().literal().getText();
		Object slotValue;
		if (slot.type() instanceof Primitive) {
			slotValue = parsePrimitiveLiteral((Primitive) slot.type(), literalText);
		} else if (slot.type() instanceof Enumerated) {
			slotValue = parseEnumeratedLiteral((Enumerated) slot.type(), literalText);
		} else {
			throw new IllegalStateException("Unsupported basic type: " + slot.type().name());
		}
		valueSetting.setValue(slot, info.object, slotValue);
	}

	private Object parseEnumeratedLiteral(Enumerated type, String literalText) {
		// TODO Auto-generated method stub
		return null;
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

	
	private Optional<ElementInfo<T>> peekLast() {
		return debug("peekLast", Optional.ofNullable(currentScope.peekLast()));
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
	
	private void push(ObjectType asObjectType, T created) {
		ElementInfo<T> newInfo = new ElementInfo<>(created, asObjectType);
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
