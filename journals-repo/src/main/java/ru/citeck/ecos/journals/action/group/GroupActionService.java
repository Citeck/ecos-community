package ru.citeck.ecos.journals.action.group;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;
import java.util.Map;

/**
 * Service to execute some action with every node in group within separate transactions
 *
 * @author Pavel Simonov
 */
public interface GroupActionService {

    Map<NodeRef, GroupActionResult> invoke(List<NodeRef> nodeRefs, String actionId, Map<String, String> params);

    Map<NodeRef, GroupActionResult> invokeBatch(List<NodeRef> nodeRefs, String actionId, Map<String, String> params);

    void register(GroupActionExecutor executor);
}
