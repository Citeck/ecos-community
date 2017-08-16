package ru.citeck.ecos.history.impl;

import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.citeck.ecos.constants.DocumentHistoryConstants;
import ru.citeck.ecos.history.HistoryRemoteService;
import ru.citeck.ecos.model.HistoryModel;
import ru.citeck.ecos.model.ICaseTaskModel;
import ru.citeck.ecos.model.IdocsModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * History remote service
 */
public class HistoryRemoteServiceImpl implements HistoryRemoteService {

    /** JSON constants */
    private static final String USERNAME = "username";
    private static final String USER_ID = "userId";
    private static final String INITIATOR = "initiator";
    private static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    private static final String WORKFLOW_DESCRIPTION = "workflowDescription";
    private static final String TASK_EVENT_INSTANCE_ID = "taskEventInstanceId";
    private static final String PROPERTY_NAME = "propertyName";
    private static final String EXPECTED_PERFORM_TIME = "expectedPerformTime";

    /**
     * Constants
     */
    private static final String[] KEYS = {
            "historyEventId", "documentId", "eventType", "comments", "version", "creationTime", "username", "userId",
            "taskRole", "taskOutcome", "taskType", "initiator", "workflowInstanceId", "workflowDescription",
            "taskEventInstanceId", "documentVersion", "propertyName", "expectedPerformTime"
    };
    private static final String HISTORY_RECORD_FILE_NAME = "history_record";
    private static final String DELIMETER = ";";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private static final SimpleDateFormat importDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final String SEND_NEW_RECORD_QUEUE = "send_new_record_queue";
    private static final String SEND_NEW_RECORDS_QUEUE = "send_new_records_queue";
    private static final String DELETE_RECORDS_BY_DOCUMENT_QUEUE = "delete_records_by_document_queue";
    private static final String DEFAULT_RESULT_CSV_FOLDER = "/citeck/ecos/history_record_csv/";

    /**
     * Use active mq
     */
    private static final String USE_ACTIVE_MQ = "ecos.citeck.history.service.use.activemq";
    private static final String CSV_RESULT_FOLDER = "ecos.citeck.history.service.csv.folder";
    private static final String HISTORY_SERVICE_HOST = "ecos.citeck.history.service.host";

    /**
     * Path constants
     */
    private static final String GET_BY_DOCUMENT_ID_PATH = "/history_records/by_document_id/";
    private static final String DELETE_BY_DOCUMENT_ID_PATH = "/history_records/by_document_id/";
    private static final String INSERT_RECORD_PATH = "/history_records/insert_record";
    private static final String INSERT_RECORDS_PATH = "/history_records/insert_records";

    /**
     * Logger
     */
    private static Log logger = LogFactory.getLog(HistoryRemoteServiceImpl.class);

    /**
     * Global properties
     */
    @Autowired
    @Qualifier("global-properties")
    private Properties properties;

    /**
     * Services
     */
    private RestTemplate restTemplate;

    private TransactionService transactionService;

    private NodeService nodeService;

    private PersonService personService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    /**
     * Get history records
     * @param documentUuid Document uuid
     * @return List of maps
     */
    @Override
    public List getHistoryRecords(String documentUuid) {
        return restTemplate.getForObject(properties.getProperty(HISTORY_SERVICE_HOST) + GET_BY_DOCUMENT_ID_PATH + documentUuid, List.class);
    }

    /**
     * Send history event to remote service
     * @param requestParams Request params
     */
    @Override
    public void sendHistoryEventToRemoteService(Map<String, Object> requestParams) {
        try {
            if (useActiveMq()) {
                if (rabbitTemplate != null) {
                    rabbitTemplate.convertAndSend(SEND_NEW_RECORD_QUEUE, convertMapToJsonString(requestParams));
                }
                if (jmsTemplate != null) {
                    jmsTemplate.convertAndSend(SEND_NEW_RECORD_QUEUE, convertMapToJsonString(requestParams));
                }
            } else {
                MultiValueMap<String, Object> paramsMap = new LinkedMultiValueMap();
                paramsMap.setAll(requestParams);
                restTemplate.postForObject(properties.getProperty(HISTORY_SERVICE_HOST) + INSERT_RECORD_PATH, paramsMap, Boolean.class);
            }

        } catch (Exception exception) {
            logger.error(exception);
            saveHistoryRecordAsCsv(requestParams);
        }
    }

    /**
     * Send history events to remote service by document reference
     * @param documentRef Document reference
     */
    @Override
    public void sendHistoryEventsByDocumentToRemoteService(NodeRef documentRef) {
        /** Loaf associations */
        List<AssociationRef> associations = nodeService.getSourceAssocs(documentRef, HistoryModel.ASSOC_DOCUMENT);
        List<Map<String, Object>> result = new ArrayList<>(associations.size());
        /** Create entries */
        for (AssociationRef associationRef : associations) {
            Map<String, Object> entryMap = new HashMap<>();
            NodeRef eventRef = associationRef.getSourceRef();
            entryMap.put(DocumentHistoryConstants.DOCUMENT_ID.getValue(), documentRef.getId());
            entryMap.put(DocumentHistoryConstants.NODE_REF.getValue(), eventRef.getId());
            entryMap.put(DocumentHistoryConstants.EVENT_TYPE.getValue(),
                    nodeService.getProperty(eventRef, HistoryModel.PROP_NAME));
            entryMap.put(DocumentHistoryConstants.DOCUMENT_VERSION.getValue(),
                    nodeService.getProperty(eventRef, HistoryModel.PROP_DOCUMENT_VERSION));
            entryMap.put(DocumentHistoryConstants.COMMENTS.getValue(),
                    nodeService.getProperty(eventRef, HistoryModel.PROP_TASK_COMMENT));
            entryMap.put(DocumentHistoryConstants.DOCUMENT_DATE.getValue(),
                    importDateFormat.format((Date) nodeService.getProperty(eventRef, HistoryModel.PROP_DATE)));
            entryMap.put(DocumentHistoryConstants.TASK_ROLE.getValue(),
                    nodeService.getProperty(eventRef, HistoryModel.PROP_TASK_ROLE));
            entryMap.put(DocumentHistoryConstants.TASK_OUTCOME.getValue(),
                    nodeService.getProperty(eventRef, HistoryModel.PROP_TASK_OUTCOME));
            QName taskType = (QName) nodeService.getProperty(eventRef, HistoryModel.PROP_TASK_TYPE);
            entryMap.put(DocumentHistoryConstants.TASK_TYPE.getValue(),taskType != null ? taskType.toString() : "");
            /** Workflow */
            entryMap.put(WORKFLOW_INSTANCE_ID, nodeService.getProperty(eventRef, HistoryModel.PROP_WORKFLOW_INSTANCE_ID));
            entryMap.put(WORKFLOW_DESCRIPTION, nodeService.getProperty(eventRef, HistoryModel.PROP_WORKFLOW_DESCRIPTION));
            entryMap.put(TASK_EVENT_INSTANCE_ID, nodeService.getProperty(eventRef, HistoryModel.PROP_TASK_INSTANCE_ID));
            entryMap.put(INITIATOR, nodeService.getProperty(eventRef, HistoryModel.ASSOC_INITIATOR));
            QName propertyName = (QName) properties.get(HistoryModel.PROP_PROPERTY_NAME);
            entryMap.put(PROPERTY_NAME, propertyName != null ? propertyName.getLocalName() : null);
            /** Event task */
            NodeRef taskNodeRef = (NodeRef) nodeService.getProperty(eventRef, HistoryModel.PROP_CASE_TASK);
            if (taskNodeRef != null) {
                Integer expectedPerformTime = (Integer) nodeService.getProperty(taskNodeRef, ICaseTaskModel.PROP_EXPECTED_PERFORM_TIME);
                entryMap.put(EXPECTED_PERFORM_TIME, expectedPerformTime != null ? expectedPerformTime.toString() : null);
            }
            /** Username and user id */
            String username = (String) nodeService.getProperty(eventRef, HistoryModel.MODIFIER_PROPERTY);
            NodeRef userNodeRef = personService.getPerson(username);
            entryMap.put(USER_ID, userNodeRef != null ? userNodeRef.getId() : null);
            entryMap.put(USERNAME, username);
            result.add(entryMap);
        }
        /** Send data */
        try {
            if (useActiveMq()) {
                if (rabbitTemplate != null) {
                    rabbitTemplate.convertAndSend(SEND_NEW_RECORDS_QUEUE, convertListOfMapsToJsonString(result));
                }
                if (jmsTemplate != null) {
                    jmsTemplate.convertAndSend(SEND_NEW_RECORDS_QUEUE, convertListOfMapsToJsonString(result));
                }
            } else {
                MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
                map.add("records", convertListOfMapsToJsonString(result));
                restTemplate.postForObject(properties.getProperty(HISTORY_SERVICE_HOST) + INSERT_RECORDS_PATH, map, Boolean.class);
            }
            /** Update document status */
            updateDocumentHistoryStatus(documentRef, true);
        } catch (Exception exception) {
            logger.error(exception);
        }
    }

    /**
     * Convert list of maps to json string
     * @param records History records
     * @return Json string
     */
    private String convertListOfMapsToJsonString(List<Map<String, Object>> records) {
        List<String> jsonObjects = new ArrayList<>(records.size());
        for (Map objectMap : records) {
            jsonObjects.add(convertMapToJsonString(objectMap));
        }
        JSONArray result = new JSONArray(jsonObjects);
        return result.toString();
    }

    /**
     * Convert map to json string
     * @param requestParams Request params map
     * @return Json string
     */
    private String convertMapToJsonString(Map<String, Object> requestParams) {
        JSONObject jsonObject =  new JSONObject(requestParams);
        return jsonObject.toString();
    }

    /**
     * Save history record as csv file
     * @param requestParams Request params
     */
    private void saveHistoryRecordAsCsv(Map<String, Object> requestParams) {
        /** Make csv string */
        StringBuilder csvResult = new StringBuilder();
        for (String key : KEYS) {
            Object value = requestParams.get(key);
            csvResult.append((value != null ? value.toString() : "") + DELIMETER);
        }
        /** Create file */
        String currentDate = dateFormat.format(new Date());
        File csvFile = new File(properties.getProperty(CSV_RESULT_FOLDER, DEFAULT_RESULT_CSV_FOLDER)
                + HISTORY_RECORD_FILE_NAME + currentDate + ".csv");
        PrintWriter printWriter = null;
        try {
            csvFile.createNewFile();
            printWriter = new PrintWriter(csvFile);
            printWriter.print(csvResult.toString());
            printWriter.flush();
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    /**
     * Update document history status
     * @param documentNodeRef Document node reference
     * @param newStatus       New document status
     */
    @Override
    public void updateDocumentHistoryStatus(NodeRef documentNodeRef, boolean newStatus) {
        RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        txnHelper.setForceWritable(true);
        boolean requiresNew = false;
        if (AlfrescoTransactionSupport.getTransactionReadState() != AlfrescoTransactionSupport.TxnReadState.TXN_READ_WRITE) {
            requiresNew = true;
        }
        txnHelper.doInTransaction(() -> {
            try {
                if (documentNodeRef != null) {
                    nodeService.setProperty(documentNodeRef, IdocsModel.DOCUMENT_USE_NEW_HISTORY, newStatus);
                }
            } catch (Exception e) {
                logger.error("Unexpected error", e);
            }
            return null;
        }, false, requiresNew);

    }

    /**
     * Remove history events by document
     * @param documentNodeRef Document node reference
     */
    @Override
    public void removeEventsByDocument(NodeRef documentNodeRef) {
        if (useActiveMq()) {
            if (rabbitTemplate != null) {
                rabbitTemplate.convertAndSend(DELETE_RECORDS_BY_DOCUMENT_QUEUE, documentNodeRef.getId());
            }
            if (jmsTemplate != null) {
                jmsTemplate.convertAndSend(DELETE_RECORDS_BY_DOCUMENT_QUEUE, documentNodeRef.getId());
            }
        } else {
            restTemplate.delete(properties.getProperty(HISTORY_SERVICE_HOST) + DELETE_BY_DOCUMENT_ID_PATH + documentNodeRef.getId());
        }
    }

    /**
     * Check - use active mq for history records sending
     * @return Check result
     */
    private Boolean useActiveMq() {
        String propertyValue = properties.getProperty(USE_ACTIVE_MQ);
        if (propertyValue == null) {
            return false;
        } else {
            return Boolean.valueOf(propertyValue);
        }
    }

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }
}