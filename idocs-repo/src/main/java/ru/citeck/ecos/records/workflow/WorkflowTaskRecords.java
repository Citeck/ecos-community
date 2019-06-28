package ru.citeck.ecos.records.workflow;

import lombok.Getter;
import lombok.Setter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.predicate.model.ComposedPredicate;
import ru.citeck.ecos.records.RecordConstants;
import ru.citeck.ecos.records.models.AuthorityDTO;
import ru.citeck.ecos.records2.RecordMeta;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.GqlContext;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records2.request.delete.RecordsDelResult;
import ru.citeck.ecos.records2.request.delete.RecordsDeletion;
import ru.citeck.ecos.records2.request.mutation.RecordsMutResult;
import ru.citeck.ecos.records2.request.mutation.RecordsMutation;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.RecordsQueryResult;
import ru.citeck.ecos.records2.source.dao.MutableRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsMetaLocalDAO;
import ru.citeck.ecos.records2.source.dao.local.RecordsQueryLocalDAO;
import ru.citeck.ecos.workflow.tasks.EcosTaskService;
import ru.citeck.ecos.workflow.tasks.TaskInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.citeck.ecos.records.workflow.WorkflowTaskRecordsConstants.*;

@Component
public class WorkflowTaskRecords extends LocalRecordsDAO
        implements RecordsMetaLocalDAO<MetaValue>,
        MutableRecordsDAO,
        RecordsQueryLocalDAO {

    private static final String DOCUMENT_FIELD_PREFIX = "_ECM_";
    private static final String OUTCOME_PREFIX = "outcome_";

    private static final String ID = "wftask";

    private final EcosTaskService ecosTaskService;
    private final AuthorityService authorityService;
    private final WorkflowTaskRecordsUtils workflowTaskRecordsUtils;

    private Map<String, String> sumAttributeByType = new ConcurrentHashMap<>();

    @Autowired
    public WorkflowTaskRecords(EcosTaskService ecosTaskService,
                               WorkflowTaskRecordsUtils workflowTaskRecordsUtils,
                               AuthorityService authorityService) {
        setId(ID);
        this.ecosTaskService = ecosTaskService;
        this.workflowTaskRecordsUtils = workflowTaskRecordsUtils;
        this.authorityService = authorityService;
    }

    @Override
    public RecordsMutResult mutate(RecordsMutation mutation) {

        RecordsMutResult result = new RecordsMutResult();

        result.setRecords(mutation.getRecords()
                .stream()
                .map(meta -> new RecordMeta(meta, RecordRef.valueOf(meta.getId().getId())))
                .map(this::mutate)
                .map(meta -> new RecordMeta(meta, RecordRef.create(getId(), meta.getId())))
                .collect(Collectors.toList()));

        return result;
    }

    private RecordMeta mutate(RecordMeta meta) {

        String taskId = meta.getId().getId();
        Optional<TaskInfo> taskInfoOpt = ecosTaskService.getTaskInfo(taskId);

        if (!taskInfoOpt.isPresent()) {
            throw new IllegalArgumentException("Task not found! id: " + taskId);
        }

        TaskInfo taskInfo = taskInfoOpt.get();

        RecordMeta documentProps = new RecordMeta();
        Map<String, Object> taskProps = new HashMap<>();

        String[] outcome = new String[1];

        meta.forEach((n, v) -> {
            if (n.startsWith(DOCUMENT_FIELD_PREFIX)) {
                documentProps.set(getEcmFieldName(n), v);
            }
            if (n.startsWith(OUTCOME_PREFIX)) {
                outcome[0] = n.substring(OUTCOME_PREFIX.length());
            } else {

                if (v.isTextual()) {
                    taskProps.put(n, v.asText());
                } else if (v.isBoolean()) {
                    taskProps.put(n, v.asBoolean());
                } else if (v.isDouble()) {
                    taskProps.put(n, v.asDouble());
                } else if (v.isInt()) {
                    taskProps.put(n, v.asInt());
                } else if (v.isLong()) {
                    taskProps.put(n, v.asLong());
                } else if (v.isNull()) {
                    taskProps.put(n, null);
                }
            }
        });

        if (outcome[0] == null) {
            throw new IllegalStateException(OUTCOME_PREFIX + "* field is mandatory for task completion");
        }

        if (documentProps.getAttributes().size() > 0) {
            RecordRef documentRef = taskInfo.getDocument();
            if (documentRef != RecordRef.EMPTY) {
                RecordMeta docMutateMeta = new RecordMeta(documentRef);
                RecordsMutation mutation = new RecordsMutation();
                mutation.setRecords(Collections.singletonList(docMutateMeta));
                recordsService.mutate(mutation);
            }
        }

        ecosTaskService.endTask(taskId, outcome[0], taskProps);
        return new RecordMeta(taskId);
    }

    private String getEcmFieldName(String name) {
        return name.substring(DOCUMENT_FIELD_PREFIX.length()).replaceAll("_", ":");
    }

    @Override
    public RecordsDelResult delete(RecordsDeletion deletion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecordsQueryResult<RecordRef> getLocalRecords(RecordsQuery query) {
        ComposedPredicate predicate = workflowTaskRecordsUtils.buildPredicateQuery(query);
        if (predicate == null || predicate.getPredicates().isEmpty()) {
            return new RecordsQueryResult<>();
        }

        RecordsQueryResult<TaskIdQuery> taskQueryResult = workflowTaskRecordsUtils.queryTasks(predicate, query);

        return new RecordsQueryResult<>(taskQueryResult, task -> RecordRef.valueOf(task.getTaskId()));
    }

    @Override
    public List<MetaValue> getMetaValues(List<RecordRef> records) {
        return records.stream().map(r -> {
            Optional<TaskInfo> info = ecosTaskService.getTaskInfo(r.getId());
            return info.isPresent() ? new Task(info.get()) : new EmptyTask(r.getId());
        }).collect(Collectors.toList());
    }

    public void setSumAttributeByType(String type, String attribute) {
        sumAttributeByType.put(type, attribute);
    }

    public static class TaskIdQuery {
        @MetaAtt("cm:name")
        @Getter @Setter public String taskId;
    }

    public static class TasksQuery {

        @Getter @Setter public String workflowId;
        @Getter @Setter public List<String> assignees;
        @Getter @Setter public List<String> actors;
        @Getter @Setter public Boolean active;
        @Getter @Setter public String docStatus;
        @Getter @Setter public String docType;
        @Getter @Setter public String document;

        public void setAssignee(String assignee) {
            if (assignees == null) {
                assignees = new ArrayList<>();
            }
            assignees.add(assignee);
        }

        public void setActor(String actor) {
            if (actors == null) {
                actors = new ArrayList<>();
            }
            actors.add(actor);
        }
    }

    public class EmptyTask implements MetaValue {

        private final String id;

        EmptyTask(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    public class Task implements MetaValue {

        private RecordRef documentRef;
        private RecordMeta documentInfo;
        private TaskInfo taskInfo;

        @Override
        public <T extends GqlContext> void init(T context, MetaField field) {
            Map<String, String> documentAttributes = new HashMap<>();
            RecordRef documentRef = getDocumentRef();
            for (String att : field.getInnerAttributes()) {
                switch (att) {
                    case ATT_DOC_SUM:
                        if (documentRef != null) {
                            String type = recordsService.getAttribute(documentRef, "_type").asText();
                            if (sumAttributeByType.containsKey(type)) {
                                documentAttributes.put(ATT_DOC_SUM, sumAttributeByType.get(type));
                            }
                        }
                        break;
                    case ATT_DOC_DISP_NAME:
                        documentAttributes.put(ATT_DOC_DISP_NAME, ".disp");
                        break;
                    case ATT_DOC_STATUS_TITLE:
                        documentAttributes.put(ATT_DOC_STATUS_TITLE, "icase:caseStatusAssoc.cm:title");
                        break;
                    case ATT_DOC_STATUS:
                        documentAttributes.put(ATT_DOC_STATUS, "icase:caseStatusAssoc.cm:name");
                        break;
                    case ATT_DOC_TYPE:
                        documentAttributes.put(ATT_DOC_TYPE, "_type");
                        break;
                    default:
                        if (att.startsWith(DOCUMENT_FIELD_PREFIX)) {
                            documentAttributes.put(att, getEcmFieldName(att));
                        }
                }
            }
            if (documentAttributes.isEmpty() || documentRef == null) {
                documentInfo = new RecordMeta();
            } else {
                documentInfo = recordsService.getAttributes(getDocumentRef(), documentAttributes);
            }
        }

        public Task(TaskInfo taskInfo) {
            this.taskInfo = taskInfo;
        }

        @Override
        public String getId() {
            return taskInfo.getId();
        }

        @Override
        public Object getAttribute(String name, MetaField field) {

            if (documentInfo.has(name)) {
                return documentInfo.get(name);
            }

            if (RecordConstants.ATT_FORM_KEY.equals(name)) {
                return taskInfo.getFormKey();
            }

            Map<String, Object> attributes = taskInfo.getAttributes();

            boolean hasPooledActors = CollectionUtils.isNotEmpty((List<?>) attributes.get("bpm_pooledActors"));
            boolean hasOwner = attributes.get("cm_owner") != null;
            boolean hasClaimOwner = attributes.get("claimOwner") != null;

            switch (name) {
                case ATT_SENDER:
                    String userName = (String) attributes.get("cwf_sender");
                    NodeRef userRef = authorityService.getAuthorityNodeRef(userName);
                    RecordRef userRecord = RecordRef.create("", userRef.toString());
                    return recordsService.getMeta(userRecord, AuthorityDTO.class);
                case ATT_ASSIGNEE:
                    String assignee = taskInfo.getAssignee();
                    if (StringUtils.isBlank(assignee)) {
                        return null;
                    }

                    NodeRef assigneeRef = authorityService.getAuthorityNodeRef(assignee);
                    RecordRef assigneeRecordRef = RecordRef.create("", assigneeRef.toString());
                    return recordsService.getMeta(assigneeRecordRef, AuthorityDTO.class);
                case ATT_CANDIDATE:
                    String candidate = taskInfo.getCandidate();
                    if (StringUtils.isBlank(candidate)) {
                        return null;
                    }

                    NodeRef candidateRef = authorityService.getAuthorityNodeRef(candidate);
                    RecordRef candidateRecordRef = RecordRef.create("", candidateRef.toString());
                    return recordsService.getMeta(candidateRecordRef, AuthorityDTO.class);
                case ATT_ACTORS:
                    return taskInfo.getActors()
                            .stream()
                            .map(actor -> {
                                RecordRef rr = RecordRef.create("", actor);
                                return recordsService.getMeta(rr, AuthorityDTO.class);
                            })
                            .collect(Collectors.toList());
                case ATT_DUE_DATE:
                    return attributes.get("bpm_dueDate");
                case ATT_STARTED:
                    return attributes.get("bpm_startDate");
                case ATT_LASTCOMMENT:
                    return attributes.get("cwf_lastcomment");
                case ATT_TITLE:
                    return taskInfo.getTitle();
                case ATT_REASSIGNABLE:
                    return isReassignable(attributes, hasOwner, hasClaimOwner);
                case ATT_CLAIMABLE:
                    return isClaimable(attributes, hasOwner, hasClaimOwner, hasPooledActors);
                case ATT_RELEASABLE:
                    return isReleasable(attributes, hasOwner, hasClaimOwner, hasPooledActors);
                case ATT_ACTIVE:
                    return attributes.get("bpm_completionDate") == null;
            }

            return attributes.get(name);
        }

        private boolean isReassignable(Map<String, Object> attributes, boolean hasOwner, boolean hasClaimOwner) {
            boolean bpmIsReassignable = Boolean.TRUE.equals(attributes.get("bpm_reassignable"));
            boolean isReassignableAllowed = bpmIsReassignable && (hasOwner || hasClaimOwner);
            boolean isReassignableDisabled = Boolean.FALSE.equals(attributes.get("cwf_isTaskReassignable"));
            return isReassignableAllowed && !isReassignableDisabled;
        }

        private boolean isClaimable(Map<String, Object> attributes, boolean hasOwner, boolean hasClaimOwner,
                                    boolean hasPooledActors) {
            boolean isClaimableAllowed = hasPooledActors && (!hasOwner && !hasClaimOwner);
            boolean isClaimableDisabled = Boolean.FALSE.equals(attributes.get("cwf_isTaskClaimable"));
            return isClaimableAllowed && !isClaimableDisabled;
        }

        private boolean isReleasable(Map<String, Object> attributes, boolean hasOwner, boolean hasClaimOwner,
                                     boolean hasPooledActors) {
            boolean isReleasableAllowed = hasPooledActors && (hasOwner || hasClaimOwner);
            boolean isReleasableDisabled = Boolean.FALSE.equals(attributes.get("cwf_isTaskReleasable"));
            return isReleasableAllowed && !isReleasableDisabled;
        }

        private RecordRef getDocumentRef() {
            if (documentRef == null) {
                documentRef = taskInfo.getDocument();
            }
            return documentRef;
        }
    }
}
