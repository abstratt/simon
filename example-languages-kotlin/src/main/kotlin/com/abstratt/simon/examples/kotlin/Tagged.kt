package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta

@Meta.Package(builtIns = [])
interface Tagged {
    @Meta.Composite(root = true)
    interface Item {
        @Meta.Name
        @Meta.Attribute
        fun name(): String?

        @Meta.Typed(String::class)
        @Meta.Attribute
        fun tags(): Collection<String>?
    }
}
