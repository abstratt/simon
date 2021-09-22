package com.abstratt.simon.examples;

import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = {})
public interface UI2 {
    class Form extends UI.Container {
        private UI.Link helpLink;

        @Meta.Contained
        public UI.Link helpLink() {
            return helpLink;
        }
    }
}
