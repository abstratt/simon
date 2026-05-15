package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta
import com.abstratt.simon.metamodel.dsl.Meta.Contained
import com.abstratt.simon.metamodel.dsl.Meta.Required
import org.apache.commons.text.WordUtils

@Meta.Package(builtIns = ["com.abstratt.simon.examples.im-primitives"])
interface IM {
    interface Primitive : BasicType {
        override fun name(): String {
            return WordUtils.uncapitalize(javaClass.getSimpleName())
        }
    }

    interface Named {
        @Meta.Name
        @Meta.Attribute
        fun name(): String?

        @Meta.Documentation
        @Meta.Attribute
        fun documentation(): String?
    }

    @Meta.Composite(root = true)
    interface Namespace : Named {
        @Contained
        @Meta.Typed(Entity::class)
        fun entities(): MutableCollection<Entity?>?

        @Contained
        @Meta.Typed(Primitive::class)
        fun primitives(): MutableCollection<Primitive?>?
    }

    interface Feature<T : Type?> : TypedElement<T?> {
        @Meta.Attribute
        fun multiple(): Boolean
    }

    interface Property : Feature<Type?>

    interface Relationship : Feature<Entity?> {
        @Meta.Reference
        fun opposite(): Relationship?
    }

    interface Operation : Named {
        enum class OperationKind {
            Action, Constructor, Event, Finder, Retriever
        }

        @Contained
        @Meta.Typed(Parameter::class)
        fun parameters(): MutableCollection<Parameter?>?

        @Meta.Attribute
        @Meta.Modifier
        fun kind(): OperationKind?

        @Meta.Attribute
        @Meta.Modifier
        fun public_(): Boolean
    }

    interface Parameter : TypedElement<Type?>

    interface TypedElement<T : Type?> : Named {
        @Meta.Reference
        @Required
        fun type(): T?
    }

    interface Entity : Type {
        @Contained
        @Meta.Typed(Property::class)
        fun properties(): MutableCollection<Property?>?

        @Meta.Reference(opposite = "subTypes")
        @Meta.Typed(Entity::class)
        fun superTypes(): MutableCollection<Entity?>?

        @Meta.Reference
        @Meta.Typed(Entity::class)
        fun subTypes(): MutableCollection<Entity?>?

        @Contained
        @Meta.Typed(Operation::class)
        fun operations(): MutableCollection<Operation?>?

        @Contained
        @Meta.Typed(Relationship::class)
        fun relationships(): MutableCollection<Relationship?>?

        @Meta.Reference(opposite = "entities")
        fun namespace(): Namespace?

        @Meta.Attribute
        @Meta.Modifier
        fun abstract_(): Boolean
    }

    interface Type : Named

    interface BasicType : Type

    interface TupleType : Type {
        @Contained
        @Meta.Typed(BasicType::class)
        fun componentTypes(): MutableList<BasicType?>?
    }
}