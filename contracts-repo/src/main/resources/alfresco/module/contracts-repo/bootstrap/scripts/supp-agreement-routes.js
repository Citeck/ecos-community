var routeFolder = search.findNode("workspace://SpacesStore/idocs-routes");
var suppType = "workspace://SpacesStore/contracts-cat-doctype-supp-agreement";

try {
    var typeNode = search.findNode(suppType);
    if (typeNode == null) {
        throw "Contract type is not exists: " + suppType;
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
        var stageProps = [];
        var stageName = "GROUP_company_director";
        stageProps["route:dueDateExpr"] = "8/h";
        stageProps["cm:position"] = "1";
        var routeStage = route.createNode(stageName, "route:stage", stageProps, "route:stages");
        if (!!routeStage) {
            var participantProps = [];
            var participantName = "GROUP_company_director";
            participantProps["cm:position"] = "1";
            routeParticipant = routeStage.createNode(participantName, "route:participant", participantProps, "route:participants");

            var groupCEO = search.selectNodes("/sys:system/sys:authorities/cm:GROUP_company_director");

            if (!!groupCEO && groupCEO.length > 0) {
                routeParticipant.createAssociation(groupCEO[0], "route:authority");

                route.properties["route:precedence"] = groupCEO[0].nodeRef + "_" + stageProps["route:dueDateExpr"];
                route.save();
            }
        }
    }
}
