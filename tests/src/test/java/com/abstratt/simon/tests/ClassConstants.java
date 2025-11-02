package com.abstratt.simon.tests;

import com.abstratt.simon.examples.JavaExampleLanguages;

public class ClassConstants {
    public Class<?> IM_BASIC_TYPE_CLASS = getClass("IM.BasicType");
    public Class<?> IM_ENTITY_CLASS = getClass("IM.Entity");
    public Class<?> IM_NAMED_CLASS = getClass("IM.Named");
    public Class<?> IM_NAMESPACE_CLASS = getClass("IM.Namespace");
    public Class<?> IM_PRIMITIVE_CLASS = getClass("IM.Primitive");
    public Class<?> RELATIONSHIP_CLASS = getClass("IM.Relationship");
    public Class<?> IM_TYPE_CLASS = getClass("IM.Type");
    public Class<?> IM_CLASS = getClass("IM");
    public Class<?> UI_APPLICATION_CLASS = getClass("UI.Application");
    public Class<?> UI_BUTTON_CLASS = getClass("UI.Button");
    public Class<?> UI_COLOR_CLASS = getClass("UI.Color");
    public Class<?> UI_COMPONENT_CLASS = getClass("UI.Component");
    public Class<?> UI_CONTAINER_CLASS = getClass("UI.Container");
    public Class<?> UI_LINK_CLASS = getClass("UI.Link");
    public Class<?> UI_NAMED_CLASS = getClass("UI.Named");
    public Class<?> UI_PANEL_LAYOUT_CLASS = getClass("UI.PanelLayout");
    public Class<?> UI_SCREEN_CLASS = getClass("UI.Screen");
    public Class<?> UI2_FORM_CLASS = getClass("UI2.Form");
    public Class<?> UI2_CLASS = getClass("UI2");
    public Class<?> UI3_PROTOTYPE_CLASS = getClass("UI3.IPrototype");
    public Class<?> UI3_CLASS = getClass("UI3");
    public Class<?> UI_CLASS = getClass("UI");

    protected Class<?> getClass(String name) {
        try {
            String packageName = getLanguagePackage();
            String javaClassName = packageName + "." + name.replace(".", "$");
            Class<?> aClass = Class.forName(javaClassName);
            System.out.println("Class for " + name + ": " + aClass);
            return aClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getLanguagePackage() {
        return JavaExampleLanguages.class.getPackageName();
    }
}
