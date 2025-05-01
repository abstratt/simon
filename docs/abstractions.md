# Abstractions Overview

This document provides an overview of the key abstractions in each module of the repository. It is intended to help new developers understand the codebase and overall design.

## annotation-dsl Module

### Meta Class

The `Meta` class in the `annotation-dsl` module is an annotation-based internal DSL for creating Java-based Simon metamodels. It provides various annotations to define types, packages, composites, contained elements, parent elements, references, attributes, and more.

## annotation-processor Module

### SimonDSLProcessor Class

The `SimonDSLProcessor` class in the `annotation-processor` module is an annotation processor that processes annotations defined in the `Meta` class. It generates resources based on the annotated elements and provides diagnostic messages during the processing.

## antlr-compiler Module

### SimonBuilder Class

The `SimonBuilder` class in the `antlr-compiler` module is responsible for building the Simon model from the parsed input. It handles the creation of objects, setting their properties, resolving references, and managing the scope of elements during the parsing process.

### SimonCompilerAntlrImpl Class

The `SimonCompilerAntlrImpl` class in the `antlr-compiler` module is the main implementation of the Simon compiler using ANTLR. It orchestrates the parsing, building, and resolving of the Simon model, and provides the overall compilation process.

## compiler-backend Module

### Backend Interface

The `Backend` interface in the `compiler-backend` module defines the contract for various backend operations in the Simon compiler. It includes methods for name setting, name querying, name resolution, instantiation, declaration, value setting, linking, parenting, and running operations.

## compiler-backend-ecore Module

### EcoreModelBuilder Class

The `EcoreModelBuilder` class in the `compiler-backend-ecore` module is responsible for building the Ecore-based model from the Simon model. It provides methods for creating objects, setting their properties, resolving references, and managing the scope of elements during the parsing process.

## compiler-source-ecore Module

### EcoreDynamicMetamodelSource Class

The `EcoreDynamicMetamodelSource` class in the `compiler-source-ecore` module is responsible for providing the Ecore-based metamodel source for the Simon compiler. It resolves type names, enumerates types, and provides built-in sources for the metamodel.

## compiler Module

### SimonCompiler Interface

The `SimonCompiler` interface in the `compiler` module defines the contract for the Simon compiler. It includes methods for compiling Simon programs from various input sources, such as strings, readers, input streams, URLs, and paths.

## metamodel-ecore Module

### EcoreMetamodel Interface

The `EcoreMetamodel` interface in the `metamodel-ecore` module defines the Ecore-based metamodel for the Simon compiler. It includes various classes and interfaces for representing types, features, slots, relationships, and more.

### EcoreType Class

The `EcoreType` class in the `metamodel-ecore` module is the base class for all Ecore-based types in the Simon metamodel. It provides methods for creating new model elements and determining if a type is a root type.

### EcoreObjectType Class

The `EcoreObjectType` class in the `metamodel-ecore` module represents an Ecore-based object type in the Simon metamodel. It includes methods for retrieving compositions, references, and features of the object type.

### EcoreRecordType Class

The `EcoreRecordType` class in the `metamodel-ecore` module represents an Ecore-based record type in the Simon metamodel. It includes methods for retrieving slots and features of the record type.

### EcoreSlot Class

The `EcoreSlot` class in the `metamodel-ecore` module represents an Ecore-based slot in the Simon metamodel. It includes methods for retrieving the type, name, and other properties of the slot.

### EcoreRelationship Class

The `EcoreRelationship` class in the `metamodel-ecore` module represents an Ecore-based relationship in the Simon metamodel. It includes methods for retrieving the type, name, and other properties of the relationship.

### EcoreFeature Class

The `EcoreFeature` class in the `metamodel-ecore` module represents an Ecore-based feature in the Simon metamodel. It includes methods for retrieving the type, name, and other properties of the feature.

### EcoreNamed Class

The `EcoreNamed` class in the `metamodel-ecore` module is the base class for all Ecore-based named elements in the Simon metamodel. It provides methods for retrieving the name of the element.

### EcoreSlotted Class

The `EcoreSlotted` class in the `metamodel-ecore` module is the base class for all Ecore-based slotted elements in the Simon metamodel. It provides methods for retrieving slots and creating new model elements.

### EcoreHelper Class

The `EcoreHelper` class in the `metamodel-ecore` module provides various utility methods for working with Ecore-based models in the Simon compiler. It includes methods for finding classifiers, setting names, and creating wrapped primitive values.

### MetaEcoreHelper Class

The `MetaEcoreHelper` class in the `metamodel-ecore` module provides various utility methods for working with Ecore-based metamodels in the Simon compiler. It includes methods for determining if a classifier is a root composite, a record, or a primitive.

## example-languages Module

### JavaExampleLanguages Class

The `JavaExampleLanguages` class in the `example-languages` module provides example languages implemented in Java for the Simon compiler. It includes various example programs and metamodels for testing and demonstration purposes.

## example-languages-kotlin Module

### KotlinExampleLanguages Class

The `KotlinExampleLanguages` class in the `example-languages-kotlin` module provides example languages implemented in Kotlin for the Simon compiler. It includes various example programs and metamodels for testing and demonstration purposes.
