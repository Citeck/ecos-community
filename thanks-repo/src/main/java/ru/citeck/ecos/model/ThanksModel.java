package ru.citeck.ecos.model;

import org.alfresco.service.namespace.QName;

public interface ThanksModel {

    //Namespaces
    public static final String TH_NAMESPACE = "http://www.citeck.ru/model/thanks/1.0";

    //Types
    public static final QName TYPE_THANKS_TYPE = QName.createQName(TH_NAMESPACE, "thanksType");

    //Aspects

    //Properties
    public static final QName PROP_COMMENTARY = QName.createQName(TH_NAMESPACE, "commentary");
    public static final QName PROP_DATE = QName.createQName(TH_NAMESPACE, "date");

    //Associations
    public static final QName ASSOC_AUTHOR = QName.createQName(TH_NAMESPACE, "author");
    public static final QName ASSOC_RECIPIENT = QName.createQName(TH_NAMESPACE, "recipient");
}
