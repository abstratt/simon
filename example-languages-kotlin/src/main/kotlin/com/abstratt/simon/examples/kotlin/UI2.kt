package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta

@Meta.Package(builtIns = [])
interface UI2 {
    class Form : UI.Container() {
        private val helpLink: UI.Link? = null
        @Meta.Contained
        fun helpLink(): UI.Link? {
            return helpLink
        }
    }
}