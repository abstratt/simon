package com.abstratt.simon.examples;

import java.util.List;

import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = {})
public interface UI {

    @Meta.ObjectType(instantiable = false)
    interface Named {
        @Meta.Required(false)
        @Meta.Name
        @Meta.Attribute
        String name();
    }

    interface Labeled {
        @Meta.Attribute
        String label();
    }

    abstract class BaseNamed implements Named {
        private String name;

        @Override
        public String name() {
            return name;
        }
    }

    interface IComponent extends Named {
        @Meta.Reference(opposite = "children")
        @Meta.Required(false)
        public Container parent();

        @Meta.Attribute
        public int index();
    }

    abstract class Component extends BaseNamed implements IComponent {
        private Container parent;
        private int index;

        @Override
        public Container parent() {
            return parent;
        }

        @Override
        public int index() {
            return index;
        }
    }

    @Meta.Composite
    abstract class Container extends Component {
        private List<IComponent> children;
        private PanelLayout layout;

        @Meta.Contained
        @Meta.Typed(IComponent.class)
        public List<IComponent> children() {
            return children;
        }

        @Meta.Attribute
        public PanelLayout getLayout() {
            return layout;
        }
    }

    abstract class LabeledComponent extends Component implements Labeled {
        private String label;

        @Override
        public String label() {
            return label;
        }
    }

    class Link extends LabeledComponent {
        private Screen targetScreen;

        @Meta.Reference
        public Screen targetScreen() {
            return targetScreen;
        }

    }

    class Screen extends Container {
        private String screenName;
        private Application application;

        @Meta.Attribute
        public String screenName() {
            return screenName;
        }

        @Meta.Reference(opposite = "screens")
        public Application application() {
            return application;
        }
    }

    @Meta.Composite(root = true)
    class Application extends BaseNamed {
        private List<Screen> screens;

        @Meta.Contained
        @Meta.Typed(Screen.class)
        public List<Screen> screens() {
            return screens;
        }
    }

    class Button extends LabeledComponent {
        private Color backgroundColor;
        private Color foregroundColor;

        @Meta.Attribute
        public Color getBackgroundColor() {
            return backgroundColor;
        }

        @Meta.Attribute
        public Color getForegroundColor() {
            return foregroundColor;
        }
    }

    enum PanelLayout {
        Vertical, Horizontal
    }

    @Meta.RecordType
    class Color {
        private final int red;
        private final int green;
        private final int blue;

        public Color(int red, int green, int blue) {
            super();
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        @Meta.Attribute
        public int red() {
            return red;
        }

        @Meta.Attribute
        public int green() {
            return green;
        }

        @Meta.Attribute
        public int blue() {
            return blue;
        }

    }
}
