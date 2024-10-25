package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta

@Meta.Package(builtIns = [])
interface UI {
    @Meta.ObjectType(instantiable = false)
    interface Named {
        @Meta.Required(false)
        @Meta.Name
        @Meta.Attribute
        fun name(): String?
    }

    interface Labeled {
        @Meta.Attribute
        fun label(): String?
    }

    abstract class BaseNamed : Named {
        private val name: String? = null
        override fun name(): String {
            return name!!
        }
    }

    interface IComponent : Named {
        @Meta.Reference(opposite = "children")
        @Meta.Required(false)
        fun parent(): Container?

        @Meta.Attribute
        fun index(): Int
    }

    abstract class Component : BaseNamed(), IComponent {
        private val parent: Container? = null
        private val index = 0
        override fun parent(): Container {
            return parent!!
        }

        override fun index(): Int {
            return index
        }
    }

    @Meta.Composite
    abstract class Container : Component() {
        private val children: List<IComponent>? = null

        @get:Meta.Attribute
        val layout: PanelLayout? = null
        @Meta.Contained
        @Meta.Typed(IComponent::class)
        fun children(): List<IComponent>? {
            return children
        }
    }

    abstract class LabeledComponent : Component(), Labeled {
        private val label: String? = null
        override fun label(): String {
            return label!!
        }
    }

    class Link : LabeledComponent() {
        private val targetScreen: Screen? = null
        @Meta.Reference
        fun targetScreen(): Screen? {
            return targetScreen
        }
    }

    open class Screen : Container() {
        private val screenName: String? = null
        private val application: Application? = null
        @Meta.Attribute
        fun screenName(): String? {
            return screenName
        }

        @Meta.Reference(opposite = "screens")
        fun application(): Application? {
            return application
        }
    }

    @Meta.Composite(root = true)
    class Application : BaseNamed() {
        private val screens: List<Screen>? = null
        @Meta.Contained
        @Meta.Typed(Screen::class)
        fun screens(): List<Screen>? {
            return screens
        }
    }

    class Button : LabeledComponent() {
        @get:Meta.Attribute
        val backgroundColor: Color? = null

        @get:Meta.Attribute
        val foregroundColor: Color? = null
    }

    enum class PanelLayout {
        Vertical,
        Horizontal
    }

    @Meta.RecordType
    class Color(private val red: Int, private val green: Int, private val blue: Int) {
        @Meta.Attribute
        fun red(): Int {
            return red
        }

        @Meta.Attribute
        fun green(): Int {
            return green
        }

        @Meta.Attribute
        fun blue(): Int {
            return blue
        }
    }
}