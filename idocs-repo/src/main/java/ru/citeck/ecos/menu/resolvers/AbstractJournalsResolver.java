package ru.citeck.ecos.menu.resolvers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.citeck.ecos.graphql.journal.JGqlPageInfoInput;
import ru.citeck.ecos.journals.JournalService;
import ru.citeck.ecos.menu.dto.Element;
import ru.citeck.ecos.model.JournalsModel;
import ru.citeck.ecos.processor.TemplateExpressionEvaluator;
import ru.citeck.ecos.records.RecordRef;
import ru.citeck.ecos.records.query.RecordsResult;
import ru.citeck.ecos.search.SearchCriteria;
import ru.citeck.ecos.utils.RepoUtils;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class AbstractJournalsResolver extends AbstractMenuItemsResolver {

    protected static final String LIST_ID_KEY = "listId";
    private static final String JOURNAL_REF_KEY = "journalRef";
    private static final String JOURNAL_LINK_KEY = "JOURNAL_LINK";
    private static final Integer COUNT_MAX = 0;
    private static final JGqlPageInfoInput PAGE_INFO = new JGqlPageInfoInput(null, COUNT_MAX,
            Collections.emptyList(), 0);

    private TemplateExpressionEvaluator expressionEvaluator;
    private NamespaceService namespaceService;
    private JournalService journalService;
    private ObjectMapper objectMapper = new ObjectMapper();

    private LoadingCache<RequestKey, Long> itemsCount;

    public AbstractJournalsResolver() {
        this.itemsCount = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build(CacheLoader.from(this::journalItemsCount));
    }

    public void invalidateAll() {
        itemsCount.invalidateAll();
    }

    public void invalidate(String journalId) {
        itemsCount.invalidate(new RequestKey(journalId));
    }

    protected Element constructItem(NodeRef journalRef, Map<String, String> params, Element context) {
        /* get data */
        String title = RepoUtils.getProperty(journalRef, ContentModel.PROP_TITLE , nodeService);
        String journalId = RepoUtils.getProperty(journalRef, JournalsModel.PROP_JOURNAL_TYPE , nodeService);
        String elemIdVar = toUpperCase(journalId);
        String parentElemId = StringUtils.defaultString(context.getId());
        String elemId = String.format("%s_%s_JOURNAL", parentElemId, elemIdVar);
        Boolean displayCount = Boolean.parseBoolean(getParam(params, context, "displayCount"));
        String countForJournalsParam = getParam(params, context, "countForJournals");
        Set<String> countForJournals;
        if (displayCount && StringUtils.isNotEmpty(countForJournalsParam)) {
            countForJournals = new HashSet<>(Arrays.asList(countForJournalsParam.split(",")));
            displayCount = countForJournals.contains(journalId);
        }
        Boolean displayIcon = context.getParams().containsKey("rootElement");

        /* icon. if journal element is placed in root category */
        String icon = null;
        if (displayIcon) {
            icon = journalId;
        }
        /* put all action params from parent (siteName or listId) */
        Map<String, String> actionParams = new HashMap<>();
        if (context.getAction() != null) {
            Map<String, String> parentActionParams = context.getAction().getParams();
            actionParams.putAll(parentActionParams);
        } else {
            if (MapUtils.isNotEmpty(params) && params.containsKey(LIST_ID_KEY)) {
                actionParams.put(LIST_ID_KEY, params.get(LIST_ID_KEY));
            }
        }
        /* current element action params */
        actionParams.put(JOURNAL_REF_KEY, journalRef.toString());

        /* current element params */
        Map<String, String> elementParams = new HashMap<>();

        /* badge (items count) */
        if (displayCount) {
            RequestKey requestKey = new RequestKey(journalId);
            Long count = itemsCount.getUnchecked(requestKey);
            elementParams.put("count", count.toString());
        }

        /* additional params for constructing child items */
        elementParams.put(JOURNAL_ID_KEY, journalId);

        /* write to element */
        Element element = new Element();
        element.setId(elemId);
        element.setLabel(title);
        element.setIcon(icon);
        element.setAction(JOURNAL_LINK_KEY, actionParams);
        element.setParams(elementParams);
        return element;
    }

    private Long journalItemsCount(RequestKey requestKey) {
        String journalId = requestKey.getJournalId();
        NodeRef journalRef = journalService.getJournalRef(journalId);
        String query = buildJournalQuery(journalRef);
        RecordsResult<RecordRef> result = journalService.getRecords(journalId, query, null, PAGE_INFO);
        return result.getTotalCount();
    }

//    TODO: move this to CriteriaSearchService
    private String buildJournalQuery(NodeRef journalRef) {
        List<NodeRef> criteriaRefs = RepoUtils.getChildrenByAssoc(
                journalRef, JournalsModel.ASSOC_SEARCH_CRITERIA, nodeService);
        if (CollectionUtils.isEmpty(criteriaRefs)) {
            return "{}";
        }
        SearchCriteria searchCriteria = new SearchCriteria(namespaceService);
        criteriaRefs.forEach(nodeRef -> {
            Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
            QName fieldQName = (QName) props.get(JournalsModel.PROP_FIELD_QNAME);
            String predicate = (String) props.get(JournalsModel.PROP_PREDICATE);
            String criterionValue = (String) props.get(JournalsModel.PROP_CRITERION_VALUE);
            if (fieldQName == null || predicate == null || criterionValue == null) {
                return;
            }
            String field = fieldQName.toPrefixString(namespaceService);
            String criterion = "";
            if (StringUtils.isNotEmpty(criterionValue)) {
                Map<String, Object> model = new HashMap<>();
                /* this replacement is used to fix template strings like this one:
                 * <#list (people.getContainerGroups(person)![]) as group>#{group.nodeRef},</#list>#{person.nodeRef} */
                criterionValue = criterionValue.replace("#{", "${");
                criterion = (String) expressionEvaluator.evaluate(criterionValue, model);
            }
            searchCriteria.addCriteriaTriplet(field, predicate, criterion);
        });
        String criteria;
        try {
            criteria = objectMapper.writeValueAsString(searchCriteria);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json processing error.", e);
        }
        return criteria;
    }

    @Autowired
    public void setExpressionEvaluator(TemplateExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Autowired
    public void setJournalService(JournalService journalService) {
        this.journalService = journalService;
    }

    @Autowired
    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    private class RequestKey {

        private final String userName;
        private final String journalId;

        public RequestKey(String journalId) {
            Objects.requireNonNull(journalId);
            this.userName = StringUtils.defaultString(AuthenticationUtil.getFullyAuthenticatedUser());
            this.journalId = journalId;
        }

        public RequestKey(String userName, String journalId) {
            Objects.requireNonNull(journalId);
            Objects.requireNonNull(userName);
            this.userName = userName;
            this.journalId = journalId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RequestKey that = (RequestKey) o;
            return Objects.equals(userName, that.userName) &&
                    Objects.equals(journalId, that.journalId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, journalId);
        }

        public String getUserName() {
            return userName;
        }

        public String getJournalId() {
            return journalId;
        }

    }
}