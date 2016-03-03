/*
 * Copyright (C) 2008-2015 Citeck LLC.
 *
 * This file is part of Citeck EcoS
 *
 * Citeck EcoS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Citeck EcoS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Citeck EcoS. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.citeck.ecos.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

public class DictionaryUtils {

    public static List<ClassDefinition> getClasses(Collection<QName> classNames, DictionaryService dictionaryService) {
        List<ClassDefinition> classes = new ArrayList<ClassDefinition>(classNames.size());
        for(QName className : classNames) {
            ClassDefinition classDef = dictionaryService.getClass(className);
            if(classDef == null) continue;
            classes.add(classDef);
        }
        return classes;
    }
    
    public static List<QName> getClassNames(Collection<? extends ClassDefinition> classes) {
        List<QName> classNames = new ArrayList<QName>(classes.size());
        for(ClassDefinition classDef : classes) {
            if(classDef == null) continue;
            classNames.add(classDef.getName());
        }
        return classNames;
    }
    
    /**
     * Return the ordered list of all class names for the specified node.
     * The list is ordered by the depth of classes in hierarchy: base classes first.
     * 
     * @param nodeRef reference to node to retrieve all classes
     * @param nodeService
     * @param dictionaryService
     * @return ordered list 
     */
    public static List<QName> getAllNodeClassNames(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        return getClassNames(getAllNodeClasses(nodeRef, nodeService, dictionaryService));
    }

    /**
     * Return class name of specified node and class names of aspects.
     *
     * @param nodeRef reference to node to retrieve classes
     * @param nodeService
     * @return list of class names
     */
    public static List<QName> getNodeClassNames(NodeRef nodeRef, NodeService nodeService) {
        List<QName> classNames = new ArrayList<QName>();
        classNames.add(nodeService.getType(nodeRef));
        classNames.addAll(nodeService.getAspects(nodeRef));
        return classNames;
    }

    /**
     * Return class definition of specified node and class definitions of aspects.
     *
     * @param nodeRef reference to node to retrieve classes
     * @param nodeService
     * @return list of class definitions
     */
    public static List<ClassDefinition> getNodeClasses(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        List<ClassDefinition> classes = new LinkedList<ClassDefinition>();
        QName typeName = nodeService.getType(nodeRef);
        classes.add(dictionaryService.getClass(typeName));
        classes.addAll(getClasses(nodeService.getAspects(nodeRef), dictionaryService));
        return classes;
    }
    
    public static List<ClassDefinition> getAllNodeClasses(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        List<ClassDefinition> classes = new LinkedList<ClassDefinition>();
        QName typeName = nodeService.getType(nodeRef);
        TypeDefinition typeDef = dictionaryService.getType(typeName);
        expandClasses(typeDef, classes, dictionaryService);
        List<ClassDefinition> aspects = getClasses(nodeService.getAspects(nodeRef), dictionaryService);
        for(ClassDefinition aspectDef : aspects) {
            expandClasses(aspectDef, classes, dictionaryService);
        }
        return classes;
    }
    
    public static Collection<QName> getChildClassNames(QName className, boolean recursive, DictionaryService dictionaryService) {
        ClassDefinition classDef = dictionaryService.getClass(className);
        if(classDef == null) {
            throw new IllegalArgumentException("Class is not registered: " + className);
        } else if(classDef.isAspect()) {
            return dictionaryService.getSubAspects(className, recursive);
        } else {
            return dictionaryService.getSubTypes(className, recursive);
        }
    }
    
    public static Collection<ClassDefinition> getParentClasses(ClassDefinition classDef, DictionaryService dictionaryService) {
        Collection<ClassDefinition> parentClasses = new ArrayList<>();
        ClassDefinition parent = classDef.getParentClassDefinition();
        while(parent != null) {
            parentClasses.add(parent);
            parent = parent.getParentClassDefinition();
        }
        return parentClasses;
    }
    
    public static Collection<QName> getParentClassNames(QName className, DictionaryService dictionaryService) {
        return getClassNames(getParentClasses(dictionaryService.getClass(className), dictionaryService));
    }

    /**
     * Expand class name to list of class names, which contains all parents and aspects.
     * The most basic classes appear first in the list.
     */
    public static List<QName> expandClassName(QName className, DictionaryService dictionaryService) {
        return expandClassNames(Collections.singletonList(className), dictionaryService);
    }

    /**
     * Expand list of class names to another list of class names, which contains all parents and aspects.
     * The most basic classes appear first in the list.
     * 
     * @param classNames source class names
     * @param dictionaryService
     * @return expanded list of class names
     */
    public static List<QName> expandClassNames(Collection<QName> classNames, DictionaryService dictionaryService) {
        return getClassNames(expandClasses(getClasses(classNames, dictionaryService), dictionaryService));
    }
    
    public static List<ClassDefinition> expandClassNamesToDefs(Collection<QName> classNames, DictionaryService dictionaryService) {
        return expandClasses(getClasses(classNames, dictionaryService), dictionaryService);
    }
    
    public static List<ClassDefinition> expandClasses(Collection<ClassDefinition> classes, DictionaryService dictionaryService) {
        List<ClassDefinition> expandedClasses = new LinkedList<ClassDefinition>();
        for(ClassDefinition classDef : classes) {
            expandClasses(classDef, expandedClasses, dictionaryService);
        }
        return expandedClasses;
    }
    
    private static void expandClasses(ClassDefinition classDef, List<ClassDefinition> classes, DictionaryService dictionaryService) {
        if(classes.contains(classDef)) return;
        // first parent class:
        if(classDef.getParentName() != null) {
            expandClasses(classDef.getParentClassDefinition(), classes, dictionaryService);
        }
        // next default aspects:
        for(AspectDefinition aspectDef : classDef.getDefaultAspects()) {
            expandClasses(aspectDef, classes, dictionaryService);
        }
        // finally class itself:
        classes.add(classDef);
    }
    
    /**
     * Get all properties, defined by classes.
     * 
     * @param classNames
     * @param dictionaryService
     * @return
     */
    public static Set<QName> getAllPropertyNames(Collection<QName> classNames, DictionaryService dictionaryService) {
        Set<QName> properties = new HashSet<>();
        for(QName className : expandClassNames(classNames, dictionaryService)) {
            ClassDefinition classDef = dictionaryService.getClass(className);
            properties.addAll(classDef.getProperties().keySet());
        }
        return properties;
    }

    public static Set<PropertyDefinition> getAllProperties(Collection<ClassDefinition> classes, DictionaryService dictionaryService) {
        Set<PropertyDefinition> properties = new HashSet<>();
        for(ClassDefinition classDef : expandClasses(classes, dictionaryService)) {
            properties.addAll(classDef.getProperties().values());
        }
        return properties;
    }

    private static Set<QName> getAllAssociationNames(
            Collection<QName> classNames, boolean needChild,
            DictionaryService dictionaryService) {
        Set<QName> associations = new HashSet<>();
        for(QName className : expandClassNames(classNames, dictionaryService)) {
            ClassDefinition classDef = dictionaryService.getClass(className);
            for(AssociationDefinition assocDef : classDef.getAssociations().values()) {
                if(assocDef.isChild() == needChild) {
                    associations.add(assocDef.getName());
                }
            }
        }
        return associations;
    }
    
    private static Set<AssociationDefinition> getAllAssociations(
            Collection<ClassDefinition> classes, boolean needChild,
            DictionaryService dictionaryService) {
        Set<AssociationDefinition> associations = new HashSet<>();
        for(ClassDefinition classDef : expandClasses(classes, dictionaryService)) {
            for(AssociationDefinition assocDef : classDef.getAssociations().values()) {
                if(assocDef.isChild() == needChild) {
                    associations.add(assocDef);
                }
            }
        }
        return associations;
    }
    
    /**
     * Get all associations, defined by classes.
     * 
     * @param classNames
     * @param dictionaryService
     * @return
     */
    public static Set<QName> getAllAssociationNames(Collection<QName> classNames, DictionaryService dictionaryService) {
        return getAllAssociationNames(classNames, false, dictionaryService);
    }

    public static Set<AssociationDefinition> getAllAssociations(Collection<ClassDefinition> classes, DictionaryService dictionaryService) {
        return getAllAssociations(classes, false, dictionaryService);
    }

    public static Set<QName> getAllChildAssociationNames(Collection<QName> classNames, DictionaryService dictionaryService) {
        return getAllAssociationNames(classNames, true, dictionaryService);
    }
    
    public static Set<AssociationDefinition> getAllChildAssociations(Collection<ClassDefinition> classes, DictionaryService dictionaryService) {
        return getAllAssociations(classes, true, dictionaryService);
    }
    
    //
    // Defined Attributes Methods
    //
    
    private static Map<QName, PropertyDefinition> getDefinedPropertiesMap(
            ClassDefinition classDef, DictionaryService dictionaryService) {
        Map<QName, PropertyDefinition> definedProps = classDef.getProperties();
        Map<QName, PropertyDefinition> inheritedProps = Collections.emptyMap();
        if(classDef.getParentClassDefinition() != null) {
            inheritedProps = classDef.getParentClassDefinition().getProperties();
        }
        Map<QName, PropertyDefinition> result = new HashMap<>();
        for(Map.Entry<QName, PropertyDefinition> entry : definedProps.entrySet()) {
            if(!inheritedProps.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    
    public static Set<PropertyDefinition> getDefinedProperties(ClassDefinition classDef, DictionaryService dictionaryService) {
        return new HashSet<>(getDefinedPropertiesMap(classDef, dictionaryService).values());
    }
    
    public static Set<PropertyDefinition> getDefinedProperties(QName className, DictionaryService dictionaryService) {
        return getDefinedProperties(dictionaryService.getClass(className), dictionaryService);
    }
    
    public static Set<QName> getDefinedPropertyNames(ClassDefinition classDef, DictionaryService dictionaryService) {
        return getDefinedPropertiesMap(classDef, dictionaryService).keySet();
    }
    
    public static Set<QName> getDefinedPropertyNames(QName className, DictionaryService dictionaryService) {
        return getDefinedPropertyNames(dictionaryService.getClass(className), dictionaryService);
    }
    
    
    private static Map<QName, AssociationDefinition> getDefinedAssociationsMap(
            ClassDefinition classDef, boolean needChild,
            DictionaryService dictionaryService) {
        Map<QName, AssociationDefinition> definedAssocs = classDef.getAssociations();
        Map<QName, AssociationDefinition> inheritedAssocs = Collections.emptyMap();
        if(classDef.getParentClassDefinition() != null) {
            inheritedAssocs = classDef.getParentClassDefinition().getAssociations();
        }
        Map<QName, AssociationDefinition> result = new HashMap<>();
        for(Map.Entry<QName, AssociationDefinition> entry : definedAssocs.entrySet()) {
            if(entry.getValue().isChild() == needChild && !inheritedAssocs.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
    
    public static Set<AssociationDefinition> getDefinedAssociations(ClassDefinition classDef, DictionaryService dictionaryService) {
        return new HashSet<>(getDefinedAssociationsMap(classDef, false, dictionaryService).values());
    }
    
    public static Set<AssociationDefinition> getDefinedAssociations(QName className, DictionaryService dictionaryService) {
        return getDefinedAssociations(dictionaryService.getClass(className), dictionaryService);
    }
    
    public static Set<QName> getDefinedAssociationNames(ClassDefinition classDef, DictionaryService dictionaryService) {
        return getDefinedAssociationsMap(classDef, false, dictionaryService).keySet();
    }
    
    public static Set<QName> getDefinedAssociationNames(QName className, DictionaryService dictionaryService) {
        return getDefinedAssociationNames(dictionaryService.getClass(className), dictionaryService);
    }
    
    public static Set<AssociationDefinition> getDefinedChildAssociations(ClassDefinition classDef, DictionaryService dictionaryService) {
        return new HashSet<>(getDefinedAssociationsMap(classDef, true, dictionaryService).values());
    }
    
    public static Set<AssociationDefinition> getDefinedChildAssociations(QName className, DictionaryService dictionaryService) {
        return getDefinedChildAssociations(dictionaryService.getClass(className), dictionaryService);
    }
    
    public static Set<QName> getDefinedChildAssociationNames(ClassDefinition classDef, DictionaryService dictionaryService) {
        return getDefinedAssociationsMap(classDef, true, dictionaryService).keySet();
    }
    
    public static Set<QName> getDefinedChildAssociationNames(QName className, DictionaryService dictionaryService) {
        return getDefinedChildAssociationNames(dictionaryService.getClass(className), dictionaryService);
    }
    
    

    /**
     * Get defining classes for specified attributes (properties and/or associations).
     * 
     * @param attributeNames names of properties and/or associations
     * @param dictionaryService
     * @return set of defining class names
     */
    public static Set<QName> getDefiningClassNames(Collection<QName> attributeNames, DictionaryService dictionaryService) {
        Set<QName> classNames = new HashSet<>();
        for(QName attributeName : attributeNames) {
            PropertyDefinition propDef = dictionaryService.getProperty(attributeName);
            if(propDef != null) {
                classNames.add(propDef.getContainerClass().getName());
                continue;
            }
            AssociationDefinition assocDef = dictionaryService.getAssociation(attributeName);
            if(assocDef != null) {
                classNames.add(assocDef.getSourceClass().getName());
                continue;
            }
            throw new IllegalArgumentException("No such property or association: " + attributeName);
        }
        return classNames;
    }

    public static List<QName> getAllNodeTypeNames(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        return getAllTypeNames(nodeService.getType(nodeRef), dictionaryService);
    }

    public static List<QName> getAllTypeNames(QName type, DictionaryService dictionaryService) {
        return getClassNames(getAllTypes(type, dictionaryService));
    }

    public static Collection<TypeDefinition> getAllTypes(QName type, DictionaryService dictionaryService) {
        return getAllTypes(dictionaryService.getType(type), dictionaryService);
    }

    public static Collection<TypeDefinition> getAllTypes(TypeDefinition type, DictionaryService dictionaryService) {
        List<TypeDefinition> types = new LinkedList<>();
        while(type != null) {
            types.add(type);
            type = (TypeDefinition) type.getParentClassDefinition();
        }
        return types;
    }

    public static Set<QName> getAllPropertyNames(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        Collection<QName> classNames = DictionaryUtils.getAllNodeClassNames(nodeRef, nodeService, dictionaryService);
        return getAllPropertyNames(classNames, dictionaryService);
    }
    
    public static Set<QName> getAllAssociationNames(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        Collection<QName> classNames = DictionaryUtils.getAllNodeClassNames(nodeRef, nodeService, dictionaryService);
        return getAllAssociationNames(classNames, dictionaryService);
    }
    
    public static Set<QName> getAllChildAssociationNames(NodeRef nodeRef, NodeService nodeService, DictionaryService dictionaryService) {
        Collection<QName> classNames = DictionaryUtils.getAllNodeClassNames(nodeRef, nodeService, dictionaryService);
        return getAllChildAssociationNames(classNames, dictionaryService);
    }

}
