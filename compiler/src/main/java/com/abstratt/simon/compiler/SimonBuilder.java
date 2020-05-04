package com.abstratt.simon.compiler;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;

import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Primitive;
import com.abstratt.simon.compiler.Metamodel.Slot;
import com.abstratt.simon.compiler.Metamodel.Slotted;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.parser.antlr.SimonBaseListener;
import com.abstratt.simon.parser.antlr.SimonParser;
import com.abstratt.simon.parser.antlr.SimonParser.ComponentContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectClassContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectHeaderContext;
import com.abstratt.simon.parser.antlr.SimonParser.ObjectNameContext;
import com.abstratt.simon.parser.antlr.SimonParser.SlotContext;

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
	private Deque<ElementInfo<T>> currentScope = new LinkedList<>();

	public SimonBuilder(Metamodel metamodel, Configuration.Provider<ObjectType, T> configurationProvider) {
		this.metamodel = metamodel;
		this.configurationProvider = configurationProvider;
	}

	public T build() {
		if (currentScope.size() != 1) {
			throw new IllegalStateException("Unexpected stack size: " + currentScope.size());
		}
		return pop().getObject();
	}

	
	@Override
	public void exitObjectHeader(ObjectHeaderContext ctx) {
		ObjectClassContext object = ctx.objectClass();
		String typeName = StringUtils.capitalize(getIdentifier(object));
		Type resolvedType = metamodel.resolveType(typeName);
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

	private String getIdentifier(ParserRuleContext object) {
		return object.getToken(SimonParser.IDENT, 0).getText();
	}
	
	@Override
	public void exitComponent(ComponentContext ctx) {
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
		ElementInfo<T> info = peekFirst().get();
		
		String propertyName = getIdentifier(ctx.slotName());
		Slotted asSlotted = (Slotted) info.type;
		Slot slot = asSlotted.slotByName(propertyName);
		if (slot == null) {
			throw new CompilerException("Unknown property: " + propertyName + " in type " + info.type.name());
		}
		ValueSetting<T, Slot> valueSetting = configurationProvider.valueSetting();
		
		String literalText = ctx.slotValue().literal().getText();
		Primitive asPrimitive = (Primitive) slot.type();
		Object slotValue;
		switch (asPrimitive.kind()) {
		case Integer:
			slotValue = Integer.parseInt(literalText);
			break;
		case String:
			slotValue = literalText.substring(1, literalText.length() - 1);
			break;
		default: throw new IllegalStateException();
		}
		valueSetting.setValue(slot, info.object, slotValue);
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
}
