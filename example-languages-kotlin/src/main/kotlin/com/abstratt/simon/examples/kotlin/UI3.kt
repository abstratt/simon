package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta

@Meta.Package(builtIns = [])
interface UI3 {
    interface IPrototype {
        @Meta.Reference
        @Meta.Typed(UI.Application::class)
        fun applications(): List<UI.Application?>?
    }
}