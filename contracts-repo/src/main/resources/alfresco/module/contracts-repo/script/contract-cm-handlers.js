<import resource="classpath:alfresco/module/contracts-repo/script/contract-cm-confirm-utils.js">

const MSG_TRANSLATOR = Packages.org.springframework.extensions.surf.util.I18NUtil;

function onCaseCreate() {

    if (document.properties['contracts:agreementNumber'] == null) {
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

    //var signingEDS = document.properties['contracts:digiSign'];
    //if (!signingEDS) {
    document.properties['contracts:barcode'] = registrationNumber;
    //}
    document.save();
}

function onProcessStart() {
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
    var caseDocuments = document.childAssocs['icase:documents'] != null ? document.childAssocs['icase:documents'] : null;
    for each (var attachment in caseDocuments) {
        var status = attachment.properties['sam:packageAttachmentStatus'];
        if (attachment.properties['sam:messageId'] != null && attachment.properties['sam:messageId'] !== ""
        && attachment.properties['tk:kind'] != null && attachment.properties['tk:kind'].nodeRef == 'workspace://SpacesStore/kind-d-counterparty-documents'
        && attachment.properties['tk:type'] && attachment.properties['tk:type'].nodeRef == 'workspace://SpacesStore/category-document-type'
            && ['BUYER_TITLE_SIGNED', 'SIGNED', 'SIGN_SENT', 'REJECTED', 'REJECTION_SENT', 'DELIVERED', 'DELIVERY_FAILED'].indexOf(status) == -1
        ) {
            return true;
        }
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

    var contractor = (document.assocs["contracts:contractor"] || [])[0];
    if (!contractor) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.field-contractor-is-not-completed"));
    } else if (!contractor.properties["idocs:diadocBoxId"]) {
        throw (MSG_TRANSLATOR.getMessage("actions.messages.contractors-field-diadocBoxId-is-not-completed"));
    } else {
        var inn = contractor.properties["idocs:inn"];
        var boxId = contractor.properties["idocs:diadocBoxId"];
        if (!inn || !boxId || !diadocService.isCounterpartyExists(inn, boxId)) {
            throw (MSG_TRANSLATOR.getMessage("actions.messages.contractor-not-found-at-diadoc"));
        }
    }

    var caseDocs = document.childAssocs["icase:documents"] || [];
    var contentFromInboundPackage = (document.assocs["sam:contentFromInboundPackage"] || [])[0];

    for (var i = 0; i < docPackages.length; i++) {
        if (docPackages[i].typeShort.equals("sam:outboundPackage")) {
            diadocService.sendPackageToCounterparty(docPackages[i].nodeRef, contractor.nodeRef);
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
        diadocService.signInboundDocs(inboundDocs);
    }
}

function _attachmentFilter(attachment) {
    if (!attachment.properties['sam:shouldBeSigned']) {
        return false;
    }
    var status = attachment.properties['sam:packageAttachmentStatus'];
    return ['RECEIVED'].indexOf(status) != -1;
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
