package ru.citeck.ecos.utils;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.ClassAttributeDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DictUtils {

    private DictionaryService dictionaryService;

    /**
     * Search property definition in specified container or associated default aspects
     * @param containerName aspect or type name. If null then default property definition will be returned
     * @param propertyName property name. Must be not null
     * @return property definition or null if definition not found
     * @throws NullPointerException if propertyName is null
     */
    public PropertyDefinition getPropDef(QName containerName, QName propertyName) {

        PropertyDefinition propDef = dictionaryService.getProperty(propertyName);

        if (propDef == null) {
            return null;
        }

        if (containerName != null) {

            ClassDefinition propContainerClass = propDef.getContainerClass();

            if (!propContainerClass.getName().equals(containerName)) {

                if (dictionaryService.isSubClass(containerName, propContainerClass.getName())) {

                    propContainerClass = dictionaryService.getClass(containerName);

                } else if (propContainerClass.isAspect()) {

                    ClassDefinition containerClass = dictionaryService.getClass(containerName);

                    for (ClassDefinition aspectDef : containerClass.getDefaultAspects(true)) {
                        if (dictionaryService.isSubClass(aspectDef.getName(), propContainerClass.getName())) {
                            propContainerClass = aspectDef;
                        }
                    }
                }

                propDef = dictionaryService.getProperty(propContainerClass.getName(), propertyName);
            }
        }

        return propDef;
    }

    @Autowired
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        dictionaryService = serviceRegistry.getDictionaryService();
    }
}
