<import resource="classpath:alfresco/module/contracts-repo/script/contract-cm-confirm-utils.js">

const MSG_TRANSLATOR = Packages.org.springframework.extensions.surf.util.I18NUtil;

function onCaseCreate() {
}

function onProcessStart() {
    if (document.properties['contracts:agreementNumber'] == null || document.properties['contracts:agreementNumber'] == ' ') {
        if (document.type == "{http://www.citeck.ru/model/contracts/1.0}agreement") {
            var numberTemplate = search.findNode("workspace://SpacesStore/agreement-number-template");
        } else {
            var numberTemplate = search.findNode("workspace://SpacesStore/supAgreement-number-template");
        }
        var registrationNumber = enumeration.getNumber(numberTemplate, document);
        document.properties['contracts:agreementNumber'] = registrationNumber;
    } else {
        var registrationNumber = document.properties['contracts:agreementNumber'];
    }

    document.properties['contracts:barcode'] = registrationNumber;
    document.save();
}

function beforeConfirm() {
    // /* this method impl. breaks second run of Approval task */
    // var lastResults = confirmUtils.getLastConfirmOutcomes(document);
    //
    // var optionalPerformers = [];
    // for (var performerRef in lastResults) {
    //     if (lastResults[performerRef] == "Confirm") {
    //         optionalPerformers.push(search.findNode(performerRef));
    //     }
    // }
    // process.optionalPerformers = optionalPerformers;
}

function afterConfirm() {
    confirmUtils.saveConfirmResults(document, process.bpm_package);
}

function isReworkAfterConfirmRequired() {
    if (process.wfcp_performOutcome == 'Reject') {
        return false;
    }

    var decisions = process.bpm_package.childAssocs['wfcp:performResults'] || [];

    for (var i in decisions) {
        var decision = decisions[i];
        if (decision && decision.properties['wfcp:resultOutcome'] == "Rework") {
            return true;
        }
    }
    return false;
}

function isConfirmResultPositive() {
    return !isReworkAfterConfirmRequired() && process.outcome != "Reject";
}

function isSelectSignerRequired() {
    var signer = document.assocs['idocs:signatory'];
    return !signer || signer.length == 0;
}

function beforeSigning() {
    return true;
}

function isContractActive() {
    return document.properties['contracts:agreementStartDate'] <= new Date();
}

function isDigiSigning() {
    return document.properties["contracts:digiSign"] != null ? document.properties["contracts:digiSign"] : false;
}

function setEdiWorkflowProperty() {
    document.properties['ufrm:firEWorkFlow'] = true;
    document.save();
}

function doNotSendContractToEdiFlag() {
    var digiSigning = document.properties["contracts:digiSign"],
    result = false;
    if (digiSigning) {
        var contractor = document.assocs['contracts:contractor'] != null ? document.assocs['contracts:contractor'][0] : null;
        var contractorDoNotSendContractToEdiFlag;
        if (contractor) {
            contractorDoNotSendContractToEdiFlag  = contractor.properties['idocs:doNotSendContractToEdiFlag'];
            if (contractorDoNotSendContractToEdiFlag) {
            result = contractorDoNotSendContractToEdiFlag;
            setEdiWorkflowProperty();
            }
        }
    }
    return result;
}

function isEdiConterpartyDocumentReceived() {
    var notReceivedStatuses = [
        'BUYER_TITLE_SIGNED',
        'SIGNED',
        'SIGN_SENT',
        'REJECTED',
        'REJECTION_SENT',
        'DELIVERED',
        'DELIVERY_FAILED'
    ];

    var caseDocuments = document.childAssocs['icase:documents'] != null ? document.childAssocs['icase:documents'] : null;
    for each (var attachment in caseDocuments) {
        var status = attachment.properties['sam:packageAttachmentStatus'];
        var messageId = attachment.properties['sam:messageId'];
        if (messageId && messageId !== "" && notReceivedStatuses.indexOf(status) < 0) {
            return true;
        }
    }

    var content = (document.assocs["sam:contentFromInboundPackage"] ||
        document.assocs["idocs:attachmentRkkCreatedFrom"] || [])[0];
    if (content != null) {
        return true;
    }

    return false;
}

function cancelRepeal() {
    utils.runAsSystem(function() {
        var startedNow = document.assocs['contracts:repealedActivity'];
        var lastStartedActivities = document.assocs['contracts:lastActiveActivities'];
        logger.log('lastStartedActivities = ' + lastStartedActivities);
        var lastActiveStatus = document.assocs['contracts:lastActiveStatus'] != null ? document.assocs['contracts:lastActiveStatus'][0] : null;
        logger.log('lastActiveStatus = ' + lastActiveStatus);
        if (lastStartedActivities !== undefined && lastStartedActivities !== null) {
            for each (var activity in lastStartedActivities) {
                caseActivityService.startActivity(activity);
                document.removeAssociation(activity, 'contracts:lastActiveActivities');
            }
        } else if (lastActiveStatus !== undefined && lastActiveStatus !== null) {
            var currentStatus = document.assocs['icase:caseStatusAssoc'] != null ? document.assocs['icase:caseStatusAssoc'][0] : null;
            if (currentStatus !== null) {
                document.removeAssociation(currentStatus, 'icase:caseStatusAssoc');
            }
            document.createAssociation(lastActiveStatus, 'icase:caseStatusAssoc');
        }
        if (startedNow !== undefined && startedNow !== null) {
            for each (var started in startedNow) {
                caseActivityService.reset(started);
            }
        }
        document.removeAspect('contracts:repealed');
    });
}

function changeSigner() {
    var signer = (additionalData.assocs['ctrEvent:signer'] || [])[0];

    if (signer) {
        var docSigner = (document.assocs['idocs:signatory'] || [])[0];
        if (docSigner) {
            document.removeAssociation(docSigner, 'idocs:signatory');
        }
        document.createAssociation(signer, 'idocs:signatory');
    }
}

function sendToContractorForESigning() {
    var docPackages = document.sourceAssocs["sam:packageDocumentLink"] || [];
    if (docPackages.length == 0) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-sam-package"));
    }

    var legalEntity = (document.assocs["contracts:agreementLegalEntity"] || [])[0];
    if (legalEntity == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-legal-entity"));
    }
    var clientBoxId = ediCounterpartyService.getCounterpartyBoxId(legalEntity, "KONTUR");
    if (clientBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    var contractor = (document.assocs["contracts:contractor"] || [])[0];
    if (!contractor) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.field-contractor-is-not-completed"));
    }
    var inn = contractor.properties["idocs:inn"];
    var counterpartyBoxId =  ediCounterpartyService.getCounterpartyBoxId(contractor, "KONTUR");
    if (counterpartyBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    if (!inn || !counterpartyBoxId || !ecosEdiService.isCounterpartyExists(clientBoxId, inn, "KONTUR")) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractor-not-found-at-diadoc"));
    }

    var caseDocs = document.childAssocs["icase:documents"] || [];
    var contentFromInboundPackage = (document.assocs["sam:contentFromInboundPackage"] ||
        document.assocs["idocs:attachmentRkkCreatedFrom"] || [])[0];

    for (var i = 0; i < docPackages.length; i++) {
        if (docPackages[i].typeShort.equals("sam:outboundPackage")) {
            var params = {
                "packageRef": docPackages[i].nodeRef,
                "clientBoxId": clientBoxId,
                "counterpartyBoxId": counterpartyBoxId
            };
            ecosEdiService.sendPackageToCounterparty(params, "KONTUR");
        }
    }

    var inboundDocs = [];

    if (contentFromInboundPackage != null) {
        inboundDocs.push(contentFromInboundPackage);
    }

    for (var i = 0; i < caseDocs.length; i++) {
        var pack = (caseDocs[i].sourceAssocs["sam:packageAttachments"] || [])[0];

        if (pack != null && pack.typeShort.equals("sam:inboundPackage")) {
            inboundDocs.push(caseDocs[i]);
        }
    }

    inboundDocs = inboundDocs.filter(function (att) {
        return _attachmentFilter(att);
    });

    if (inboundDocs.length > 0) {
        ecosEdiService.signDocuments(inboundDocs, "KONTUR");
    }
}

function sendESignsForInboundPackage() {
    var docPackages = document.sourceAssocs["sam:packageDocumentLink"] || [];
    if (docPackages.length == 0) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-sam-package"));
    }

    var legalEntity = (document.assocs["contracts:agreementLegalEntity"] || [])[0];
    if (legalEntity == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-legal-entity"));
    }
    var clientBoxId = ediCounterpartyService.getCounterpartyBoxId(legalEntity, "KONTUR");
    if (clientBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    var contractor = (document.assocs["contracts:contractor"] || [])[0];
    if (!contractor) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.field-contractor-is-not-completed"));
    }
    var inn = contractor.properties["idocs:inn"];
    var counterpartyBoxId =  ediCounterpartyService.getCounterpartyBoxId(contractor, "KONTUR");
    if (counterpartyBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    if (!inn || !counterpartyBoxId || !ecosEdiService.isCounterpartyExists(clientBoxId, inn, "KONTUR")) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractor-not-found-at-diadoc"));
    }

    var caseDocs = document.childAssocs["icase:documents"] || [];
    var contentFromInboundPackage = (document.assocs["sam:contentFromInboundPackage"] ||
            document.assocs["idocs:attachmentRkkCreatedFrom"] || [])[0];

    var inboundDocs = [];

    if (contentFromInboundPackage != null) {
        inboundDocs.push(contentFromInboundPackage);
    }

    for (var i = 0; i < caseDocs.length; i++) {
        var pack = (caseDocs[i].sourceAssocs["sam:packageAttachments"] || [])[0];
        if (pack != null && pack.typeShort.equals("sam:inboundPackage")) {
            inboundDocs.push(caseDocs[i]);
        }
    }

    inboundDocs = inboundDocs.filter(function (att) {
        return _attachmentFilter(att);
    });

    if (inboundDocs.length > 0) {
        ecosEdiService.signDocuments(inboundDocs, "KONTUR");
    }
}

function sendRejectSigns() {
    var docPackages = document.sourceAssocs["sam:packageDocumentLink"] || [];
    if (docPackages.length == 0) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-sam-package"));
    }

    var legalEntity = (document.assocs["contracts:agreementLegalEntity"] || [])[0];
    if (legalEntity == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.cant-find-link-to-legal-entity"));
    }
    var clientBoxId = ediCounterpartyService.getCounterpartyBoxId(legalEntity, "KONTUR");
    if (clientBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    var contractor = (document.assocs["contracts:contractor"] || [])[0];
    if (!contractor) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.field-contractor-is-not-completed"));
    }
    var inn = contractor.properties["idocs:inn"];
    var counterpartyBoxId =  ediCounterpartyService.getCounterpartyBoxId(contractor, "KONTUR");
    if (counterpartyBoxId == null) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    }
    if (!inn || !counterpartyBoxId || !ecosEdiService.isCounterpartyExists(clientBoxId, inn, "KONTUR")) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractor-not-found-at-diadoc"));
    }

    var caseDocs = document.childAssocs["icase:documents"] || [];
    var contentFromInboundPackage = (document.assocs["sam:contentFromInboundPackage"] ||
        document.assocs["idocs:attachmentRkkCreatedFrom"] || [])[0];

    var inboundDocs = [];

    if (contentFromInboundPackage != null) {
        inboundDocs.push(contentFromInboundPackage);
    }

    for (var i = 0; i < caseDocs.length; i++) {
        var pack = (caseDocs[i].sourceAssocs["sam:packageAttachments"] || [])[0];
        if (pack != null && pack.typeShort.equals("sam:inboundPackage")) {
            inboundDocs.push(caseDocs[i]);
        }
    }

    inboundDocs = inboundDocs.filter(function (att) {
        return _attachmentFilter(att);
    });

    if (inboundDocs.length > 0) {
        ecosEdiService.rejectDocuments(inboundDocs, "KONTUR");
    }
}

function _attachmentFilter(attachment) {
    if (!attachment.properties['sam:shouldBeSigned']) {
        return false;
    }
    var status = attachment.properties['sam:packageAttachmentStatus'];
    return ['RECEIVED', 'DELIVERY_FAILED'].indexOf(status) != -1;
}

function allDocsAreSigned() {
    var completeStatuses = ["SIGNED"];
    var caseDocs = getCaseDocs();
    var result = true;

    for (var i = 0; i < caseDocs.length; i++) {
        var status = caseDocs[i].properties["sam:packageAttachmentStatus"] || "";
        var shouldBeSigned = caseDocs[i].properties["sam:shouldBeSigned"];

        if (completeStatuses.indexOf(status) < 0 && shouldBeSigned) {
            result = false;
            break;
        }
    }

    return result;
}

function getCaseDocs() {
    var caseDocs = document.childAssocs["icase:documents"] || [];
    var content = (document.assocs["sam:contentFromInboundPackage"] ||
        document.assocs["idocs:attachmentRkkCreatedFrom"] || [])[0];

    if (content != null) {
        caseDocs.push(content);
    }

    return caseDocs;
}

function resetCase() {
    //reset activities
    caseActivityService.reset(document);

    //remove all events
    var events = document.sourceAssocs['event:document'] || [];
    for each(var event in events) {
        event.addAspect("sys:temporary");
        event.remove();
    }

    //remove all documents
    var docs = document.childAssocs['icase:documents'] || [];
    for each(var doc in docs) {
        doc.addAspect("sys:temporary");
        doc.remove();
    }

    //remove all workflows
    var workflows = services.get('WorkflowService').getWorkflowsForContent(document.nodeRef, false);
    for (var i = 0; i < workflows.size(); i++) {
        services.get('WorkflowService').deleteWorkflow(workflows.get(i).getId());
    }
}
