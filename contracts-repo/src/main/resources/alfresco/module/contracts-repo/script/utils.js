
var utils = {
    getFirstAssoc: function(node, assocType) {
        return (node.assocs[assocType] || [])[0];
    },

    addAssigneeToRole: function(role, authority) {
        this.addAssocs(this.getRole(role), "icaseRole:assignees", [authority]);
    },

    addAssocs: function(node, assocType, targets) {
        var existing = node.assocs[assocType] || [];
        for (var i in targets) {
            var target = targets[i];
            if (!this.containsNode(existing, target)) {
                node.createAssociation(target, assocType);
            }
        }
    },

    setAssocs: function(node, assocType, targets) {
        var diff = this.getNodesDifference(node.assocs[assocType] || [], targets);
        for (var i in diff.removed) {
            node.removeAssociation(diff.removed[i], assocType);
        }
        for (var i in diff.added) {
            node.createAssociation(diff.added[i], assocType);
        }
    },

    getNodesDifference: function(base, target) {
        var added = [], removed = [];
        for (var i = 0; i < base.length; i++) {
            if (!this.containsNode(target, base[i])) {
                removed.push(base[i]);
            }
        }
        for (var i = 0; i < target.length; i++) {
            if (!this.containsNode(base, target[i])) {
                added.push(target[i]);
            }
        }
        return {'added': added, 'removed': removed};
    },

    containsNode: function(array, node) {
        for (var i = 0; i < array.length; i++) {
            if (array[i].equals(node)) {
                return true;
            }
        }
        return false;
    },

    getRole: function(obj) {
        if (typeof obj === 'string' || obj instanceof String) {
            return this.getRoleByName(obj);
        } else {
            return obj;
        }
    },

    getRoleByName: function(name) {
        var roles = document.childAssocs['icaseRole:roles'] || [];
        for (var i in roles) {
            if (roles[i].properties['icaseRole:varName'] == name) {
                return roles[i];
            }
        }
        return null;
    },

    getRoleAssignees: function(obj) {
        var role = this.getRole(obj);
        if (role) {
            return role.assocs['icaseRole:assignees'] || [];
        } else {
            return [];
        }
    },

    runAsSystem: function(func) {
        return Packages.org.alfresco.repo.security.authentication.AuthenticationUtil.runAsSystem(func);
    },

    restartActivity: function(name) {
        var activity = this.getActivityByName(name);
        if (activity) {
            caseActivityService.reset(activity);
            caseActivityService.startActivity(activity);
        }
    },

    resetActivity: function(name) {
        var activity = this.getActivityByName(name);
        if (activity) caseActivityService.reset(activity);
    },

    startActivity: function(name) {
        var activity = this.getActivityByName(name);
        if (activity) caseActivityService.startActivity(activity);
    },

    getActivityByName: function(name) {
        return CaseActivityService.getActivityByName(document, name);
    },

    getActiveWorkflowByTaskType: function(taskType) {
        var runtimeService = services.get("activitiRuntimeService");

        var wfService = services.get("WorkflowService");
        var workflows = wfService.getWorkflowsForContent(document.nodeRef, true);

        if (!workflows) return null;

        var wfIt = workflows.iterator();
        while (wfIt.hasNext()) {
            var workflow = wfIt.next();
            var id = workflow.getId().replaceAll("activiti\\$", "");
            if (runtimeService.getVariable(id, "wfcp_formKey") == taskType) {
                return workflow;
            }
        }
        return null;
    },

    addParallelExecutionForTask: function(taskName, performer) {
        if (!performer || !taskName) return false;
        return this.addParallelExecution(this.getActiveWorkflowByTaskType(taskName), performer);
    },

    addParallelExecution: function(workflow, performer) {
        if (!workflow) return false;

        var runtimeService = services.get("activitiRuntimeService");

        var workflowId = workflow.getId().replaceAll("activiti\\$", "");
        var performers = new Packages.java.util.ArrayList();
        performers.add(performer.nodeRef);
        var command = new Packages.ru.citeck.ecos.workflow.activiti.cmd.AddParallelExecutionInstanceCmd(workflowId, 'perform-sub-process', performers);
        return runtimeService.getCommandExecutor().execute(command);
    },

    getAuthorityName: function(authority) {
        var authorityName = null;
        if (authority.isSubType("cm:person")) {
            authorityName = authority.properties['cm:userName'];
        } else if (authority.isSubType("cm:authorityContainer")) {
            authorityName = authority.properties['cm:authorityName'];
        }
        return authorityName;
    },

    isInvolvedInProcess: function(authority, workflow) {
        if (!authority || !workflow) return false;

        var query = new Packages.org.alfresco.service.cmr.workflow.WorkflowTaskQuery();
        query.setActive(true);
        query.setTaskState(Packages.org.alfresco.service.cmr.workflow.WorkflowTaskState.IN_PROGRESS);
        query.setProcessId(workflow.getId());
        query.setActorId(this.getAuthorityName(authority));
        var tasks = services.get("WorkflowService").queryTasks(query);

        return tasks && tasks.size() > 0;
    }
};

