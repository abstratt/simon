package com.abstratt.simon.examples.kotlin

import com.abstratt.simon.metamodel.dsl.Meta

@Meta.Package(builtIns = [])
interface DAUI {
    interface IEntityComponent : UI.IComponent {
        @Meta.Reference
        @Meta.Typed(IM.Entity::class)
        fun entity(): IM.Entity?
    }

    class EntityScreen : UI.Screen(), IEntityComponent {
        private val entity: IM.Entity? = null
        override fun entity(): IM.Entity {
            return entity!!
        }
    }
}