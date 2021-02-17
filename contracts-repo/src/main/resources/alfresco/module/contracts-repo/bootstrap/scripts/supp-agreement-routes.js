var routeFolder = search.findNode("workspace://SpacesStore/idocs-routes");
var suppType = "workspace://SpacesStore/contracts-cat-doctype-supp-agreement";

try {
    var typeNode = search.findNode(suppType);
    if (typeNode == null) {
        throw "Supplementary agreement type is not exists: " + suppType;
    }
    var suppTypeRef = typeNode.nodeRef;
    if (!!routeFolder) {
        var route = routeFolder.childByNamePath("supp-agreement-confirmation-by-ceo");
        if (route) {
            logger.info("[supp-agreement-routes.js] Route found");
            updateRoute(route, suppTypeRef);
        } else {
            logger.info("[supp-agreement-routes.js] Route not found");
            createRoute();
        }
    }
} catch (e) {
    logger.error("[supp-agreement-routes.js] Error when creating basic route for supp agreement case. " + e);
}

function updateRoute(route, suppTypeRef) {
    route.properties['tk:appliesToType'] = suppTypeRef;
    route.save();
}

function createRoute() {

    var routeProps = [];
    var routeName = "supp-agreement-confirmation-by-ceo";
    routeProps["tk:appliesToType"] = suppTypeRef;
    var route = routeFolder.createNode(routeName, "route:route", routeProps);
    if (!!route) {
        var routeStageNames = ["GROUP_company_director","GROUP_company_accountant"];
        var stagePositionIdx = 1;
        routeStageNames.forEach(function(stageName) {
            var stageProps = [];
            stageProps["route:dueDateExpr"] = "8/h";
            stageProps["cm:position"] = stagePositionIdx;
            stagePositionIdx++;
            var routeStage = route.createNode(stageName, "route:stage", stageProps, "route:stages");
            if (!!routeStage) {
                var participantProps = [];
                var participantName = stageName;
                participantProps["cm:position"] = "1";
                routeParticipant = routeStage.createNode(participantName, "route:participant", participantProps, "route:participants");

                var participantPath = "/sys:system/sys:authorities/cm:" + participantName;

                var participantGroup = search.selectNodes(participantPath);

                if (!!participantGroup && participantGroup.length > 0) {
                    routeParticipant.createAssociation(participantGroup[0], "route:authority");

                    route.properties["route:precedence"] = participantGroup[0].nodeRef + "_" + stageProps["route:dueDateExpr"];
                    route.save();
                }
            }
        });
    }
}
