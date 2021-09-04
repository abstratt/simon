# simon - {Sim}ple {O}bject {N}otation

Simon is a framework that supports implementation of object-based DSLs via parser combinators.

## Motivation

Imagine there was a conventional way of textually representing object models?

### Simon can represent graphs, not only trees

JSON and YAML work fine for object trees. However, object models are more diverse than trees. Simon supports associations between objects.

### Simon is polyglot

Simon can be implemented on almost any language.

The first reference implementation happens to be implemented in Java.

### Simon's metamodel

You can read more about Simon's metamodel by checking out the documentation generated from  the reference implementation ([javadoc](https://abstratt.github.io/simon/com/abstratt/simon/metamodel/package-summary.html)).  

### The general syntax for Simon-based programs


#### Language declaration

Every Simon program starts with a language declaration (`@<language-name>`). 

```
@language <language-name>
...
```

#### Program elements

Every program element is declared by specifying the kind of element, an optional name, and an optional body.

These are all valid Simon programs:

```
@language UI
application myApplication { }
```


```
@language UI
application myApplication

```

```
@language UI
application {}
```

#### Element properties

Any element of a kind that contains attributes, properties can be specified as:


```
<kind> [<name>] ([<attribute-name> : <attribute-value>[ ...]])
```

Example: 

```
button btn1a (label : 'Ok')
```

#### Child elements

For any element of a kind that admits child elements, children can be specified like this:


```
screen screen1 (layout : Vertical) {
    children {
        button btn1a (label : 'Ok')
        button btn1b (
            label : 'Cancel' 
            backgroundColor : #(red : 100 green : 50)
    	)
    	link(label: 'To screen 2') {
    	  targetScreen: screen2
    	}
    }
}
```
    
  
### A simple Simon-encoded program

See an example Simon program describing a user-interface:

```
@language UI
application myApplication { 
  screens { 
    screen screen1 (layout : Vertical) {
        children {
            button btn1a (label : 'Ok')
            button btn1b (
                label : 'Cancel' 
                backgroundColor : #(red : 100 green : 50)
            )
            link(label: 'To screen 2') {
              targetScreen: screen2
            }
        }
    } 
    screen screen2 {
        children {
            button btn2a (label : 'Ok')
        }
    } 
    screen screen3 {} 
  } 
}
```

## Java/Ecore-based implementation

#### Decisions

1. primitive values are stored as EObjects that contain one `value` feature

### Module structure

* gen-utils - utilities used by Simon but not tied to the Simon API
* metamodel-core - defines the metamodel behind Simon
* compiler-core - the basic API for the Java-based Simon compiler
* parser-antlr - the ANTLR-based generic parser for all Simon-based DSLs
* compiler-antlr - the ANTLR-based generic compiler
* compiler-ecore - a backend for the Simon compiler which produces EMF-based models
* annotation-dsl - an annotation-based DSL for defining a Simon metamodel
* metamodel-ecore - an implementation of the Simon metamodel on top of Ecore metamodels and pure Java-based metamodels   
* tests - all tests live here 
