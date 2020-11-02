/**
 * Create test data for community FitNesse tests
 */

//USERS
const FAKE_MAIL_PART = ".fake.email@gmail.com";
const USER_PASSWORD = "One two three";

people.createPerson("ivan.petrov", "Иван", "Петров", "ivan.petrov" + FAKE_MAIL_PART, USER_PASSWORD, true);
people.createPerson("kata.petrova", "Екатерина", "Петрова", "kata.petrova" + FAKE_MAIL_PART, USER_PASSWORD, true);

var initiator = people.createPerson("initiator", "Initiator", "1", " initiator@citeck.ru", USER_PASSWORD, true),
    clerk = people.createPerson("clerk", "Clerk", "1", "clerk@citeck.ru", USER_PASSWORD, true),
    approver = people.createPerson("approver", "Approver", "1", "approver@citeck.ru", USER_PASSWORD, true),
    signatory = people.createPerson("signatory", "Signatory", "1", "signatory@citeck.ru", USER_PASSWORD, true),
    accountant = people.createPerson("accountant", "Accountant", "1", "accountant@citeck.ru", USER_PASSWORD, true),
    ordtech = people.createPerson("ordtech", "Ordtech", "1", "ordtech@citeck.ru", USER_PASSWORD, true);

addUserToSitesWithCollaboratorRole(initiator);
addUserToSitesWithCollaboratorRole(clerk);
addUserToSitesWithCollaboratorRole(accountant);

var clerksGroup = groups.getGroup("clerks"),
    directorGroup = groups.getGroup("company_director"),
    accountantGroup = groups.getGroup("company_accountant"),
    chiefAccountantGroup = groups.getGroup("company_chief_accountant"),
    ordersTechnologistGroup = groups.getGroup("orders-technologist"),
	contractManagerGroup = groups.getGroup("contract-manager");

clerksGroup.addAuthority(clerk.properties["cm:userName"]);
directorGroup.addAuthority(approver.properties["cm:userName"]);
accountantGroup.addAuthority(accountant.properties["cm:userName"]);
chiefAccountantGroup.addAuthority(accountant.properties["cm:userName"]);
ordersTechnologistGroup.addAuthority(ordtech.properties["cm:userName"]);
contractManagerGroup.addAuthority(initiator.properties["cm:userName"]);

//DATA LIST
const ASSOC_TYPE_CONTAINS = "cm:contains";

//LEGAL ENTITIES
const LEGAL_ENTITIES_PATH = "/app:company_home/app:dictionary/cm:dataLists/cm:legal-entities";
const LEGAL_ENTITY_TYPE = "idocs:legalEntity";

var legalEntityRoot = getDataListRootByXpath(LEGAL_ENTITIES_PATH);

legalEntityRoot.createNode(null, LEGAL_ENTITY_TYPE, {
    "idocs:fullOrganizationName": "ООО \"ФИНТЕХ\"",
    "idocs:juridicalAddress": "г. Москва, ул. Ленина 982, оф 311",
    "idocs:postAddress": "г. Москва, ул. Ленина 982, оф 311",
    "idocs:inn": "1000900334",
    "idocs:kpp": "3900030427"
}, ASSOC_TYPE_CONTAINS);

var techComplLegalEntity = legalEntityRoot.createNode(null, LEGAL_ENTITY_TYPE, {
    "idocs:fullOrganizationName": "ОАО \"ТЕХ-КОМПЛЕКТ\"",
    "idocs:juridicalAddress": "г. Новосибирск, ул. Ворошилова 4, оф 407",
    "idocs:postAddress": "г. Новосибирск, ул. Ворошилова 4, оф 407",
    "idocs:inn": "84800003737",
    "idocs:kpp": "80378363663"
}, ASSOC_TYPE_CONTAINS);

//COUNTERPARTY
const COUNTERPARTY_PATH = "/app:company_home/app:dictionary/cm:dataLists/cm:contractors";
const COUNTERPARTY_TYPE = "idocs:contractor";

var counterpartyRoot = getDataListRootByXpath(COUNTERPARTY_PATH);

counterpartyRoot.createNode(null, COUNTERPARTY_TYPE, {
    "idocs:fullOrganizationName": "ОАО \"СЕВЕР-СТАЛЬ\"",
    "idocs:contractorKind": "individual",
    "idocs:juridicalAddress": "г. Томск, ул. Веры Волошиной 92 б, оф 300",
    "idocs:postAddress": "г. Томск, ул. Веры Волошиной 92 б, оф 300",
    "idocs:inn": "7003244330",
    "idocs:CEOname": "Балашев Дмитрий Александрович"
}, ASSOC_TYPE_CONTAINS);

var testCounterparty = counterpartyRoot.createNode(null, COUNTERPARTY_TYPE, {
    "idocs:fullOrganizationName": "ОАО ТЕСТ ",
    "idocs:contractorKind": "individual",
    "idocs:juridicalAddress": "г. Омск, ул. Нужная 56, оф 5",
    "idocs:postAddress": "г. Омск, ул. Нужная 56, оф 5",
    "idocs:inn": "1003244440",
    "idocs:CEOname": "Журавлёв Вальтер Макарович"
}, ASSOC_TYPE_CONTAINS);

var steelCounterparty = counterpartyRoot.createNode(null, COUNTERPARTY_TYPE, {
    "idocs:fullOrganizationName": "ООО Стальпром ",
    "idocs:contractorKind": "individual",
    "idocs:juridicalAddress": "г. Новосибирск, ул. Ленина 23",
    "idocs:postAddress": "г. Новосибирск, ул. Ленина 23",
    "idocs:inn": "4233244411",
    "idocs:CEOname": "Ионов Степан Сергеевич"
}, ASSOC_TYPE_CONTAINS);

//BUDGET ITEMS
const BUDGET_ITEMS_PATH = "/app:company_home/app:dictionary/cm:dataLists/cm:budget-items";
const BUDGET_ITEMS_TYPE = "budget:item";

var budgetItemsRoot = getDataListRootByXpath(BUDGET_ITEMS_PATH);

budgetItemsRoot.createNode(null, BUDGET_ITEMS_TYPE, {
    "budget:itemCode": "10",
    "budget:itemName": "Затраты на нужды офиса"
}, ASSOC_TYPE_CONTAINS);

//CONTRACT SUBJECTS
const SUBJECT_ITEMS_PATH = "/app:company_home/st:sites/cm:contracts/cm:dataLists/cm:subjects";
const SUBJECT_ITEMS_TYPE = "contracts:subject";

var subjectsRoot = getDataListRootByXpath(SUBJECT_ITEMS_PATH);

subjectsRoot.createNode(null, SUBJECT_ITEMS_TYPE, {
    "contracts:subjectCode": "30",
    "contracts:subjectName": "Продажа"
}, ASSOC_TYPE_CONTAINS);

//CONTRACT
const CONTRACT_PATH = "/app:company_home/st:sites/cm:contracts/cm:documentLibrary/cm:contracts";
const CONTRACT_TYPE = "contracts:agreement";

var contractRoot = getDataListRootByXpath(CONTRACT_PATH);

var contract = contractRoot.createNode(null, CONTRACT_TYPE, {
    "contracts:contractWith": "client",
    "contracts:agreementNumber": "111-forFitNesse",
    "tk:kind": "workspace://SpacesStore/contracts-cat-contract-services"
}, ASSOC_TYPE_CONTAINS);

contract.createAssociation(techComplLegalEntity, "contracts:agreementLegalEntity");
contract.createAssociation(testCounterparty, "contracts:contractor");

var currencyEur = search.findNode("workspace://SpacesStore/currency-eur");
contract.createAssociation(currencyEur, "contracts:agreementCurrency");

//FILES NOMENCLATURES
const FILES_NOMENCLATURES_PATH = "/app:company_home/cm:dataLists/cm:filesNomenclature";
const FILES_NOMENCLATURES_TYPE = "idocs:filesNomenclature";

var filesNomenclaturesRoot = getDataListRootByXpath(FILES_NOMENCLATURES_PATH);
filesNomenclaturesRoot.createNode(null, FILES_NOMENCLATURES_TYPE, {
    "idocs:fileIndex": "15463",
    "idocs:fileName": "Тестовое"
}, ASSOC_TYPE_CONTAINS);


function addUserToSitesWithCollaboratorRole(user) {
    var collaboratorsGroupId = ["site_contracts_SiteCollaborator", "site_letters_SiteCollaborator",
        "site_cases_SiteCollaborator", "site_orders_SiteCollaborator", "site_order-pass_SiteCollaborator",
        "site_meetings_SiteCollaborator", "site_common-documents_SiteCollaborator"];

    for each (var groupId in collaboratorsGroupId) {
        var group = groups.getGroup(groupId);
        group.addAuthority(user.properties["cm:userName"]);
    }
}

function getDataListRootByXpath(xpath) {
    var dataList = search.xpathSearch(xpath);

    if (!dataList || dataList.length === 0) {
        throw ("Root node by xpath <" + xpath + "> not found")
    }

    return dataList[0];
}

