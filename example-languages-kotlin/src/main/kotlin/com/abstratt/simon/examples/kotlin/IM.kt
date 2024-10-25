package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta
import org.apache.commons.text.WordUtils

@Meta.Package(builtIns = ["im"])
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
    }

    @Meta.Composite(root = true)
    interface Namespace : Named {
        @Meta.Contained
        @Meta.Typed(Entity::class)
        fun entities(): Collection<Entity>

        @Meta.Contained
        @Meta.Typed(Primitive::class)
        fun primitives(): Collection<Primitive>
    }

    interface Feature<T : Type> : TypedElement<T> {
        @Meta.Attribute
        fun multiple(): Boolean
    }

    interface Property : Feature<Type>
    interface Relationship : Feature<Entity> {
        @Meta.Reference
        fun opposite(): Relationship?
    }

    interface Operation : Named {
        enum class OperationKind {
            Action,
            Constructor,
            Event,
            Finder,
            Retriever
        }

        @Meta.Contained
        @Meta.Typed(Parameter::class)
        fun parameters(): Collection<Parameter>

        @Meta.Attribute
        fun kind(): OperationKind

        @Meta.Attribute
        fun public_(): Boolean
    }

    interface Parameter : TypedElement<Type>
    interface TypedElement<T : Type> : Named {
        @Meta.Reference
        @Meta.Required
        fun type(): T
    }

    interface Entity : Type {
        @Meta.Contained
        @Meta.Typed(Property::class)
        fun properties(): Collection<Property>

        @Meta.Reference(opposite = "subTypes")
        @Meta.Typed(
            Entity::class
        )
        fun superTypes(): Collection<Entity>

        @Meta.Reference
        @Meta.Typed(Entity::class)
        fun subTypes(): Collection<Entity>

        @Meta.Contained
        @Meta.Typed(Operation::class)
        fun operations(): Collection<Operation>

        @Meta.Contained
        @Meta.Typed(Relationship::class)
        fun relationships(): Collection<Relationship>

        @Meta.Reference(opposite = "entities")
        fun namespace(): Namespace

        @Meta.Attribute
        fun abstract_(): Boolean
    }

    interface Type : Named
    interface BasicType : Type
    interface TupleType : Type {
        @Meta.Contained
        @Meta.Typed(BasicType::class)
        fun componentTypes(): List<BasicType>
    }
}