(function () {
    var document = search.findNode(args.nodeRef);

    if (!document) {
        status.setCode(status.STATUS_NOT_FOUND, "Could not find document " + args.nodeRef);
        return;
    }

    var registrationNumber = document.properties["contracts:agreementNumber"];

    if (!registrationNumber) {
        model.success = false;
    } else {
        document.properties["contracts:barcode"] = registrationNumber;
        document.save();
        model.success = true;
    }
})();
