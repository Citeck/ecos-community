<import resource="classpath:alfresco/module/contracts-repo/script/contract-cm-roles.js">
<import resource="classpath:alfresco/module/contracts-repo/script/contract-cm-email.js">
<import resource="classpath:alfresco/module/contracts-repo/script/contract-cm-status.js">

const TASK_CONFIRM = "ctrwf:confirmTask";
const OPTIONAL_PERFORMERS = "optionalPerformers";

var confirmUtils = {

    _currentConfirmWF: null,

    cancelStrategyByStatus: {
        "approval": {
            fn: function() {
                var runtimeService = services.get("activitiRuntimeService");
                var workflowId = confirmUtils.getCurrentConfirmWF().getId().replaceAll("activiti\\$", "");

                var optionalPerformers = runtimeService.getVariable(workflowId, OPTIONAL_PERFORMERS);
                if (optionalPerformers) {
                    optionalPerformers.remove(person.nodeRef);
                    runtimeService.setVariable(workflowId, OPTIONAL_PERFORMERS, optionalPerformers);
                }

                confirmUtils.addParallelConfirmTask(person);
            },
            condition: function () {
                return !utils.isInvolvedInProcess(person, confirmUtils.getCurrentConfirmWF());
            }
        },
        "signing": {
            fn: function () {
                if (!document.properties['contracts:digiSign']) {
                    utils.resetActivity("signing-root-stage");
                } else {
                    utils.resetActivity("signing-eds-root-stage");
                }
                utils.restartActivity("rework-root-stage");
            }
        },
        "counterparty-signing": {
            fn: function () {
                utils.resetActivity("counterparty-signing-root-stage");
                utils.restartActivity("rework-root-stage");
            },
            condition: function() {
                return !document.properties['contracts:digiSign'];
            }
        },
        "approved": {
            fn: function () {
                utils.resetActivity("check-signer-root-stage");
                utils.restartActivity("rework-root-stage");
            }
        },
        "reworking": {}
    },

    getCurrentConfirmWF: function() {
        if (!this._currentConfirmWF) {
            this._currentConfirmWF = utils.getActiveWorkflowByTaskType(TASK_CONFIRM);
        }
        return this._currentConfirmWF;
    },

    addParallelConfirmTask: function(confirmer) {
        return utils.addParallelExecution(this.getCurrentConfirmWF(), confirmer);
    },

    notifyOnCancelDecision: function() {
        var confirmers = utils.getRoleAssignees(ROLE_CONFIRMERS) || [];
        var initiator = utils.getRoleAssignees(ROLE_INITIATOR) || [];

        var recipients = confirmers.concat(initiator).map(utils.getAuthorityName);
        recipients = recipients.filter(function(item, pos) {
            return recipients.indexOf(item) == pos;
        });

/*        var templateName = "cm:notification-template.html.ftl";
        var templatePath = "/app:company_home/app:dictionary/app:email_templates/app:notify_email_templates";
        var template = search.selectNodes(templatePath + "/" + templateName)[0];

        var args = {
            documentData: email.prepareTemplateData({comment: additionalData.properties['iEvent:comment']}),
            header: "По Договору было отозвано согласование. Согласующий: " + email.format(person),
            document: document
        };

        var mail = actions.create("mail");
        mail.parameters.to_many = recipients;
        mail.parameters.subject = "По договору было отозвано согласование";
        mail.parameters.from = services.get("global-properties")["mail.from.default"];
        mail.parameters.send_after_commit = true;
        mail.parameters.template = template;
        mail.parameters.template_model = {'args': args};
        mail.execute(document);*/
    },

    saveHistoryOnCancelDecision: function () {
        var props = {
            'event:name': 'approval.cancelled',
            'event:document': document,
            'event:taskComment': additionalData.properties['iEvent:comment'],
            'event:taskType': citeckUtils.createQName('ctrwf:confirmTask'),
            'event:taskOutcome': 'CancelApproval',
            'event:taskRole': 'Согласующие'
        };
        historyService.persistEvent("history:basicEvent", props);
    },

    cancelDecision: function() {
        utils.runAsSystem(function () {
            var lastDecision = confirmUtils.getLastConfirmDecisions(document)[person.nodeRef];

            if (confirmUtils.canCancelDecision(lastDecision)) {
                lastDecision.properties['wfcp:resultOutcome'] = "Confirm-revoked";
                lastDecision.save();

                confirmUtils.saveHistoryOnCancelDecision();
                confirmUtils.notifyOnCancelDecision();

                var status = (document.assocs["icase:caseStatusAssoc"] || [])[0];
                if (caseStatusService.getStatus(document) == STATUS_APPROVAL){
                     var strategy = confirmUtils.cancelStrategyByStatus["approval"];
                     if (strategy.fn) strategy.fn();
                }
                var strategy = confirmUtils.cancelStrategyByStatus[status.properties['cm:name']];
                if (strategy.fn) strategy.fn();
            }
        });
    },

    canCancelDecision: function(lastDecision) {
        if (!lastDecision) {
            lastDecision = this.getLastConfirmDecisions(document)[person.nodeRef];
        }
        if (!lastDecision || lastDecision.properties['wfcp:resultOutcome'] != "Confirm") {
            return false;
        }
        var status = (document.assocs["icase:caseStatusAssoc"] || [])[0];
        if (status) {
            var lastDecisionStatus = lastDecision.properties['wfcp:resultOutcome'];
            if (caseStatusService.getStatus(document) == STATUS_APPROVAL && lastDecisionStatus == "Confirm"){
                return true;
            }
            var strategy = this.cancelStrategyByStatus[status.properties['cm:name']];
            return strategy && (!strategy.condition || strategy.condition());
        }
        return false;
    },

    canAddConfirmer: function () {
        var status = caseStatusService.getStatus(document);

        if (status == STATUS_APPROVAL) {
            return caseRoleService.isRoleMember(document, ROLE_INITIATOR, person)
                || caseRoleService.isRoleMember(document, ROLE_CONFIRMERS, person);
        } else if (status == STATUS_REWORKING) {
            return caseRoleService.isRoleMember(document, ROLE_INITIATOR, person);
        }

        return false;
    },

    addAdditionalConfirmer: function () {

        if (!this.canAddConfirmer()) {
            return;
        }

        var confirmer = (additionalData.assocs['iEvent:confirmer'] || [])[0];
        if (!confirmer) throw "Согласующий не выбран";

        var confirmers = caseRoleService.getAssignees(document, ROLE_CONFIRMERS);
        confirmers = confirmers.concat(caseRoleService.getAssignees(document, ROLE_ADDITIONAL_CONFIRMERS));

        if (utils.containsNode(confirmers, confirmer)) {
            throw "Выбранный согласующий уже добавлен в процесс";
        }

        caseRoleService.addAssignees(document, ROLE_ADDITIONAL_CONFIRMERS, confirmer);
        caseRoleService.updateRole(document, ROLE_CONFIRMERS);
    },

    getLastConfirmOutcomes: function(document) {
        var outcomes = {};
        var lastDecisions = this.getLastConfirmDecisions(document);
        for (var performer in lastDecisions) {
            outcomes[performer] = lastDecisions[performer].properties['wfcp:resultOutcome'];
        }
        return outcomes;
    },

    getLastConfirmDecisions: function(document) {
        var addLastResult = function(decision, assoc, results) {
            var performer;
            performer = (decision.assocs[assoc] || [])[0];
            if (performer && !results[performer.nodeRef]) {
                results[performer.nodeRef] = decision;
            }
        };
        var decisions = this.getConfirmDecisions(document);
        var lastResults = {};
        for (var i in decisions) {
            addLastResult(decisions[i], "wfcp:resultPerformer", lastResults);
            addLastResult(decisions[i], "wfcp:resultPerson", lastResults);
        }
        return lastResults;
    },

    getConfirmDecisions: function(document) {
        var decisionComparator = function(decision0, decision1) {
            var date0 = decision0.properties['wfcp:resultDate'];
            var date1 = decision1.properties['wfcp:resultDate'];
            if (!date0 || !date1) {
                return 0;
            }
            return date1.getTime() - date0.getTime();
        };

        var decisions = document.assocs['contracts:confirmResults'] || [];
        var workflow = this.getCurrentConfirmWF();
        if (workflow) {
            var pack = search.findNode(workflow.getWorkflowPackage());
            decisions = decisions.concat(pack.childAssocs['wfcp:performResults'] || [])
        }
        decisions.sort(decisionComparator);
        return decisions;
    },

    saveConfirmResults: function(document, bpm_package) {
        var results = bpm_package.childAssocs['wfcp:performResults'] || [];
        for (var i in results) {
            document.createAssociation(results[i], 'contracts:confirmResults');
        }
        var lastPackage = (document.assocs['contracts:lastConfirmWFPackage'] || [])[0];
        if (lastPackage) {
            document.removeAssociation(lastPackage, 'contracts:lastConfirmWFPackage');
        }
        document.createAssociation(bpm_package, 'contracts:lastConfirmWFPackage');
    }
};
