(function () {

    var onRedirectToCreatePaymentPage = {type: "payments:payment", from: "contract"},
        onRedirectToCreateClosingDocPage = {type: "contracts:closingDocument", from: "contract"},
        onRedirectToCreateSupAgreementPage = {type: "contracts:supplementaryAgreement", from: "contract"},
        onRedirectToCreateClosingDocForInvoice = {type: "contracts:closingDocument", from: "invoice"};

    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onRedirectToCreatePaymentPage",
        fn: function (record) {
            redirectToCreatePage(record, onRedirectToCreatePaymentPage)
        }
    });

    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onRedirectToCreateClosingDocPage",
        fn: function (record) {
            redirectToCreatePage(record, onRedirectToCreateClosingDocPage)
        }
    });

    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onRedirectToCreateSupAgreementPage",
        fn: function (record) {
            redirectToCreatePage(record, onRedirectToCreateSupAgreementPage)
        }
    });

    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onRedirectToCreateClosingDocForInvoice",
        fn: function (record) {
            redirectToCreatePage(record, onRedirectToCreateClosingDocForInvoice)
        }
    });

    YAHOO.Bubbling.fire("registerAction", {
        actionName: "onActionGenerateBarcode",
        fn: function (record) {
            Alfresco.util.Ajax.jsonPost({
                url: Alfresco.constants.PROXY_URI + "citeck/barcode/generate-barcode?nodeRef=" + record.nodeRef,
                successCallback: {
                    scope: this,
                    fn: function (response) {
                        var result = JSON.parse(response.serverResponse.responseText);
                        if (result.success == "true") {
                            Alfresco.util.PopupManager.displayMessage({
                                text: Alfresco.util.message('actions.citeck.generate-barcode.success-message'),
                                displayTime: 5
                            });
                            YAHOO.Bubbling.fire("metadataRefresh");
                        } else {
                            Alfresco.util.PopupManager.displayMessage({
                                text: Alfresco.util.message('actions.citeck.generate-barcode.failure-message'),
                                displayTime: 5
                            });
                        }
                    }
                }
            });
        }
    });

    function redirectToCreatePage(record, pageOfCreate) {
        Alfresco.util.Ajax.jsonGet({
            url: Alfresco.constants.PROXY_URI + "api/journals/create-variants/site/contracts",
            successCallback: {
                scope: this,
                fn: function (response) {
                    var jsNode = record.jsNode,
                        destNodeRef = "";

                    for (var i = 0; i < response.json.createVariants.length; i++) {
                        if (response.json.createVariants[i].type == pageOfCreate.type) {
                            destNodeRef = response.json.createVariants[i].destination;
                            break;
                        }
                    }

                    if (destNodeRef) {
                        var redirection = '/share/page/node-create?type=' + pageOfCreate.type + '&destination=' + destNodeRef;

                        if (pageOfCreate.from == "invoice") {
                            var contracts = jsNode.properties["payments:basis_added"];
                            if (contracts != null && contracts.length > 0) {
                                redirection += '&param_contract=' + contracts[0];
                            }
                        }

                        if (jsNode.nodeRef) {
                            redirection += '&param_' + pageOfCreate.from + '=' + jsNode.nodeRef;
                        }

                        window.open(redirection);
                    } else {
                        showErrorPrompt();
                    }
                }
            },
            failureCallback: {
                fn: function () {
                    showErrorPrompt();
                },
                scope: this
            }
        });
    }

    function showErrorPrompt() {
        Alfresco.util.PopupManager.displayPrompt({
            title: Alfresco.util.message("message.failure"),
            text: Alfresco.util.message("Error getting url for redirection. Please contact administrator.")
        });
    }
})();