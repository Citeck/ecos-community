package ru.citeck.ecos.model;

import org.alfresco.service.namespace.QName;

public interface EcosModel {

    // model
    String ECOS_MODEL_PREFIX = "ecos";

    //Namespaces
    String ECOS_NAMESPACE = "http://www.citeck.ru/model/content/ecos/1.0";

    //Types
    QName TYPE_DOCUMENT = QName.createQName(ECOS_NAMESPACE, "document");
    QName TYPE_CASE = QName.createQName(ECOS_NAMESPACE, "case");
    QName TYPE_ICON = QName.createQName(ECOS_NAMESPACE, "icon");

    //Aspects
    QName ASPECT_HAS_RESPONSIBLE = QName.createQName(ECOS_NAMESPACE, "hasResponsible");
    QName ASPECT_DOCUMENT_TYPE = QName.createQName(ECOS_NAMESPACE, "documentType");
    QName ASPECT_HAS_NUMBER_AND_DATE = QName.createQName(ECOS_NAMESPACE, "hasNumberAndDate");
    QName ASPECT_HAS_AMOUNT = QName.createQName(ECOS_NAMESPACE, "hasAmount");
    QName ASPECT_HAS_VAT = QName.createQName(ECOS_NAMESPACE, "hasVAT");
    QName ASPECT_HAS_ADDITIONAL_PERSON_PROPERTIES = QName.createQName(ECOS_NAMESPACE, "hasAdditionalPersonProperties");

    //Properties
    QName PROP_DOCUMENT_NUMBER = QName.createQName(ECOS_NAMESPACE, "documentNumber");
    QName PROP_DOCUMENT_DATE = QName.createQName(ECOS_NAMESPACE, "documentDate");
    QName PROP_DOCUMENT_AMOUNT = QName.createQName(ECOS_NAMESPACE, "documentAmount");
    QName PROP__VAT = QName.createQName(ECOS_NAMESPACE, "VAT");
    QName PROP_PHOTO = QName.createQName(ECOS_NAMESPACE, "photo");
    QName PROP_BIRTH_DATE = QName.createQName(ECOS_NAMESPACE, "birthDate");
    QName PROP_BIRTH_MONTH_DAY = QName.createQName(ECOS_NAMESPACE, "birthMonthDay");
    QName PROP_CITY = QName.createQName(ECOS_NAMESPACE, "city");
    QName PROP_SEX = QName.createQName(ECOS_NAMESPACE, "sex");
    QName PROP_PHONE_WORKING = QName.createQName(ECOS_NAMESPACE, "phoneWorking");
    QName PROP_PHONE_INTERNAL = QName.createQName(ECOS_NAMESPACE, "phoneInternal");
    QName PROP_OLD_PASS = QName.createQName(ECOS_NAMESPACE, "oldPass");
    QName PROP_PASS = QName.createQName(ECOS_NAMESPACE, "pass");
    QName PROP_PASS_VERIFY = QName.createQName(ECOS_NAMESPACE, "passVerify");
    QName PROP_TIMEZONE = QName.createQName(ECOS_NAMESPACE, "timezone");
    QName PROP_IS_PERSON_DISABLED = QName.createQName(ECOS_NAMESPACE, "isPersonDisabled");
    QName PROP_ICON_TYPE = QName.createQName(ECOS_NAMESPACE, "iconType");
    QName PROP_FA_ICON_NAME = QName.createQName(ECOS_NAMESPACE, "faIconName");
    QName PROP_USER_INILA = QName.createQName(ECOS_NAMESPACE, "inila");
    QName PROP_USER_INN = QName.createQName(ECOS_NAMESPACE, "inn");
    QName PROP_TYPE_OF_EMPLOYMENT = QName.createQName(ECOS_NAMESPACE, "typeOfEmployment");

    //Associations
    QName ASSOC_RESPONSIBLE = QName.createQName(ECOS_NAMESPACE, "responsible");

}
