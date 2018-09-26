package ru.citeck.ecos.webscripts.history;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import ru.citeck.ecos.constants.DocumentHistoryConstants;
import ru.citeck.ecos.history.HistoryRemoteService;
import ru.citeck.ecos.history.filter.Criteria;
import ru.citeck.ecos.history.impl.HistoryGetService;
import ru.citeck.ecos.model.IdocsModel;
import ru.citeck.ecos.spring.registry.MappingRegistry;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

public class DocumentHistoryGet extends DeclarativeWebScript {

    private static final String ENABLED_REMOTE_HISTORY_SERVICE = "ecos.citeck.history.service.enabled";

    /**
     * Constants
     */
    public static final String ALFRESCO_NAMESPACE = "http://www.alfresco.org/model/content/1.0";
    public static final String HISTORY_PROPERTY_NAME = "history";
    public static final String ATTRIBUTES_PROPERTY_NAME = "attributes";

    /**
     * Request params
     */
    private static final String PARAM_DOCUMENT_NODE_REF = "nodeRef";
    private static final String PARAM_EVENTS = "events";
    private static final String PARAM_FILTER = "filter";

    /**
     * Global properties
     */
    @Autowired
    @Qualifier("global-properties")
    private Properties properties;

    /**
     * Services
     */
    private HistoryRemoteService historyRemoteService;

    private PersonService personService;

    private NodeService nodeService;

    private HistoryGetService historyGetService;

    @Autowired
    private ServiceRegistry serviceRegistry;

    private MappingRegistry<String, Criteria> filterRegistry = new MappingRegistry<>();

    /**
     * Execute implementation
     *
     * @param req    Http-request
     * @param status Status
     * @param cache  Cache
     * @return Map of attributes
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        String nodeRefUuid = req.getParameter(PARAM_DOCUMENT_NODE_REF);
        String eventsParam = req.getParameter(PARAM_EVENTS);

        Set<String> includeEvents = null;
        if (StringUtils.isNotBlank(eventsParam)) {
            includeEvents = Arrays.stream(eventsParam.split(",")).collect(Collectors.toSet());
        }

        /* Check history event status */
        NodeRef documentRef = new NodeRef(nodeRefUuid);
        Boolean useNewHistory = (Boolean) nodeService.getProperty(documentRef, IdocsModel.PROP_USE_NEW_HISTORY);
        if ((useNewHistory == null || !useNewHistory) && isEnabledRemoteHistoryService()) {
            historyRemoteService.sendHistoryEventsByDocumentToRemoteService(documentRef);
        }
        /* Load data */
        List historyRecordMaps;
        if (isEnabledRemoteHistoryService()) {
            historyRecordMaps = historyRemoteService.getHistoryRecords(documentRef.getId());
        } else {
            historyRecordMaps = historyGetService.getHistoryEventsByDocumentRef(documentRef);
        }

        String filterParam = req.getParameter(PARAM_FILTER);
        if (StringUtils.isNotBlank(filterParam)) {
            Criteria filterCriteria = filterRegistry.getMapping().get(filterParam);
            if (filterCriteria == null) {
                status.setCode(Status.STATUS_BAD_REQUEST, "Filter with id: " + filterParam + " not found");
                return null;
            }

            historyRecordMaps = filterCriteria.meetCriteria(historyRecordMaps);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("jsonResult", createJsonResponse(historyRecordMaps, includeEvents));
        return result;
    }

    /**
     * Check - is remote history service enabled
     *
     * @return Check result
     */
    private Boolean isEnabledRemoteHistoryService() {
        String propertyValue = properties.getProperty(ENABLED_REMOTE_HISTORY_SERVICE);
        if (propertyValue == null) {
            return false;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    /**
     * Create json response
     *
     * @param historyRecordMaps History records maps
     * @return Json string
     */
    @SuppressWarnings("unchecked")
    private String createJsonResponse(List<Map> historyRecordMaps, Set<String> includeEvents) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode resultObjectNode = objectMapper.createObjectNode();
        ArrayNode arrayNode = objectMapper.createArrayNode();
        /* Transform records */
        for (Map<String, Object> historyRecordMap : historyRecordMaps) {

            String eventType = (String) historyRecordMap.get(DocumentHistoryConstants.EVENT_TYPE.getValue());

            if (includeEvents != null && !includeEvents.contains(eventType)) {
                continue;
            }

            ObjectNode recordObjectNode = objectMapper.createObjectNode();
            recordObjectNode.put(DocumentHistoryConstants.NODE_REF.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.NODE_REF.getValue()));
            ObjectNode attributesNode = objectMapper.createObjectNode();
            /* Populate object */
            Date date = new Date((Long) historyRecordMap.get(DocumentHistoryConstants.DOCUMENT_DATE.getValue()));
            ZoneOffset offset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            OffsetDateTime offsetDateTime = date.toInstant().atOffset(offset);
            attributesNode.put(DocumentHistoryConstants.DOCUMENT_DATE.getKey(), offsetDateTime.toString());
            attributesNode.put(DocumentHistoryConstants.DOCUMENT_VERSION.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.DOCUMENT_VERSION.getValue()));
            attributesNode.put(DocumentHistoryConstants.COMMENTS.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.COMMENTS.getValue()));
            attributesNode.put(DocumentHistoryConstants.EVENT_TYPE.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.EVENT_TYPE.getValue()));
            attributesNode.put(DocumentHistoryConstants.TASK_ROLE.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.TASK_ROLE.getValue()));
            attributesNode.put(DocumentHistoryConstants.TASK_OUTCOME.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.TASK_OUTCOME.getValue()));
            attributesNode.put(DocumentHistoryConstants.TASK_INSTANCE_ID.getKey(),
                    (String) historyRecordMap.get(DocumentHistoryConstants.TASK_INSTANCE_ID.getValue()));

            String taskType = (String) historyRecordMap.get(DocumentHistoryConstants.TASK_TYPE.getValue());

            if (StringUtils.isNotEmpty(taskType)) {

                QName taskTypeValue = QName.createQName(taskType);

                ObjectNode taskTypeNode = objectMapper.createObjectNode();
                taskTypeNode.put("fullQName", taskType);
                taskTypeNode.put("shortQName", taskTypeValue.toPrefixString(
                        serviceRegistry.getNamespaceService())
                );

                attributesNode.put(DocumentHistoryConstants.TASK_TYPE.getKey(), taskTypeNode);
            }

            ArrayList<NodeRef> attachments = (ArrayList<NodeRef>) historyRecordMap.get(
                    DocumentHistoryConstants.TASK_ATTACHMENTS.getValue());
            if (attachments != null) {
                attributesNode.put(DocumentHistoryConstants.TASK_ATTACHMENTS.getKey(),
                        transformNodeRefsToArrayNode(attachments));
            }

            ArrayList<NodeRef> pooledActors = (ArrayList<NodeRef>) historyRecordMap.get(
                    DocumentHistoryConstants.TASK_POOLED_ACTORS.getValue());
            if (pooledActors != null) {
                attributesNode.put(DocumentHistoryConstants.TASK_POOLED_ACTORS.getKey(),
                        transformNodeRefsToArrayNode(pooledActors));
            }

            /* User */
            NodeRef userNodeRef = personService.getPerson((String) historyRecordMap.get(
                    DocumentHistoryConstants.EVENT_INITIATOR.getValue())
            );
            if (userNodeRef != null) {
                attributesNode.put(DocumentHistoryConstants.EVENT_INITIATOR.getKey(), createUserNode(userNodeRef));
            }

            /* Add history node to result */
            recordObjectNode.put(ATTRIBUTES_PROPERTY_NAME, attributesNode);
            arrayNode.add(recordObjectNode);
        }
        resultObjectNode.put(HISTORY_PROPERTY_NAME, arrayNode);
        return resultObjectNode.toString();
    }

    private ArrayNode transformNodeRefsToArrayNode(ArrayList<NodeRef> nodes) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode result = objectMapper.createArrayNode();
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }

        for (NodeRef node : nodes) {
            ObjectNode attachmentNode = objectMapper.createObjectNode();
            attachmentNode.put("nodeRef", node.toString());
            result.add(attachmentNode);
        }

        return result;
    }

    /**
     * Create user node
     *
     * @param userNodeRef User node reference
     * @return Array node
     */
    private ArrayNode createUserNode(NodeRef userNodeRef) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode result = objectMapper.createArrayNode();
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put("nodeRef", userNodeRef.toString());
        userNode.put("type", "cm:person");
        userNode.put("cm:userName", (String) nodeService.getProperty(userNodeRef,
                QName.createQName(ALFRESCO_NAMESPACE, "userName")));
        userNode.put("cm:firstName", (String) nodeService.getProperty(userNodeRef,
                QName.createQName(ALFRESCO_NAMESPACE, "firstName")));
        userNode.put("cm:lastName", (String) nodeService.getProperty(userNodeRef,
                QName.createQName(ALFRESCO_NAMESPACE, "lastName")));
        userNode.put("cm:middleName", (String) nodeService.getProperty(userNodeRef,
                QName.createQName(ALFRESCO_NAMESPACE, "middleName")));
        String displayName = userNode.get("cm:lastName") + " " + userNode.get("cm:firstName") + " "
                + userNode.get("cm:middleName");
        userNode.put("displayName", displayName.trim());
        result.add(userNode);
        return result;

    }

    public void setHistoryRemoteService(HistoryRemoteService historyRemoteService) {
        this.historyRemoteService = historyRemoteService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setHistoryGetService(HistoryGetService historyGetService) {
        this.historyGetService = historyGetService;
    }

    public void setFilterRegistry(MappingRegistry<String, Criteria> filterRegistry) {
        this.filterRegistry = filterRegistry;
    }
}
