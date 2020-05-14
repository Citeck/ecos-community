(function () {
    var document = search.findNode(args.nodeRef);

    if (document.type == "{http://www.citeck.ru/model/contracts/1.0}agreement"){
        var propertyName = "contracts:barcode";
        } else {
        var propertyName = "idocs:barcode";
    }

    if (!document) {
        status.setCode(status.STATUS_NOT_FOUND, "Could not find document " + args.nodeRef);
        return;
    }

    var registrationNumber = document.properties["contracts:agreementNumber"];

    if (!registrationNumber) {
        model.success = false;
    } else {
        document.properties[propertyName] = registrationNumber;
        document.save();
        model.success = true;
    }
})();
