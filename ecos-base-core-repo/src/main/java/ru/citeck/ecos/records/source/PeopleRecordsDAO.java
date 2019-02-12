package ru.citeck.ecos.records.source;

import com.fasterxml.jackson.databind.JsonNode;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.graphql.GqlContext;
import ru.citeck.ecos.graphql.meta.value.MetaValue;
import ru.citeck.ecos.records.RecordRef;
import ru.citeck.ecos.records.request.query.RecordsQuery;
import ru.citeck.ecos.records.request.query.RecordsQueryResult;
import ru.citeck.ecos.records.source.alfnode.AlfNodesRecordsDAO;
import ru.citeck.ecos.records.source.alfnode.meta.AlfNodeRecord;
import ru.citeck.ecos.utils.AuthorityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PeopleRecordsDAO extends LocalRecordsDAO
                              implements RecordsQueryDAO,
                                         RecordsWithMetaDAO,
                                         RecordsMetaDAO {

    public static final String ID = "people";

    private static final String PROP_USER_NAME = "userName";
    private static final String PROP_CM_USER_NAME = "cm:" + PROP_USER_NAME;

    private static final String PROP_FULL_NAME = "fullName";
    private static final String PROP_IS_AVAILABLE = "isAvailable";
    private static final String PROP_IS_MUTABLE = "isMutable";
    private static final String PROP_IS_ADMIN = "isAdmin";
    private static final String PROP_AUTHORITIES = "authorities";

    private static final Log logger = LogFactory.getLog(PeopleRecordsDAO.class);

    private AuthorityUtils authorityUtils;
    private AuthorityService authorityService;
    private AlfNodesRecordsDAO alfNodesRecordsDAO;
    private MutableAuthenticationService authenticationService;

    @Autowired
    public PeopleRecordsDAO(AuthorityUtils authorityUtils,
                            AuthorityService authorityService,
                            AlfNodesRecordsDAO alfNodesRecordsDAO,
                            MutableAuthenticationService authenticationService) {
        setId(ID);
        this.authorityUtils = authorityUtils;
        this.authorityService = authorityService;
        this.alfNodesRecordsDAO = alfNodesRecordsDAO;
        this.authenticationService = authenticationService;
    }

    @Override
    protected List<?> getMetaValues(List<RecordRef> records) {
        return records.stream()
                      .map(r -> new UserValue(r.getId()))
                      .collect(Collectors.toList());
    }

    @Override
    protected RecordsQueryResult<?> getMetaValues(RecordsQuery query) {

        if (SearchService.LANGUAGE_FTS_ALFRESCO.equals(query.getLanguage())) {

            RecordsQueryResult<RecordRef> records = alfNodesRecordsDAO.getRecords(query);
            return new RecordsQueryResult<>(records, UserValue::new);
        }

        JsonNode queryNode = query.getQuery();

        if (queryNode.isNull() || queryNode.isMissingNode()) {

            NodeRef ref = authorityService.getAuthorityNodeRef(authenticationService.getCurrentUserName());
            if (ref != null) {
                RecordsQueryResult<UserValue> result = new RecordsQueryResult<>();
                result.addRecord(new UserValue(ref));
                result.setTotalCount(1);
                result.setHasMore(false);
                return result;
            }
        }

        return super.getMetaValues(query);
    }

    public class UserValue implements MetaValue {

        private AlfNodeRecord alfNode;
        private String userName;
        private UserAuthorities userAuthorities;

        UserValue(String userName) {
            this.userName = userName;
            NodeRef nodeRef = authorityService.getAuthorityNodeRef(userName);
            alfNode = new AlfNodeRecord(new RecordRef("", nodeRef.toString()));
        }

        UserValue(NodeRef nodeRef) {
            this.alfNode = new AlfNodeRecord(new RecordRef("", nodeRef.toString()));
        }

        UserValue(RecordRef recordRef) {
            this.alfNode =  new AlfNodeRecord(recordRef);
        }

        @Override
        public MetaValue init(GqlContext context) {

            alfNode.init(context);

            if (userName == null) {
                try {
                    List<? extends MetaValue> attribute = alfNode.getAttribute(PROP_CM_USER_NAME);
                    userName = attribute.stream()
                                        .findFirst()
                                        .map(MetaValue::getString)
                                        .orElse(null);
                } catch (Exception e) {
                    throw new RuntimeException("Error! " + alfNode.getId(), e);
                }
                if (userName == null) {
                    userName = "";
                }
            }
            return this;
        }

        @Override
        public String getString() {
            return getId();
        }

        @Override
        public String getId() {
            return userName;
        }

        private UserAuthorities getUserAuthorities() {
            if (userAuthorities == null) {
                userAuthorities = new UserAuthorities(userName);
            }
            return userAuthorities;
        }

        @Override
        public Object getAttribute(String attributeName) {

            switch (attributeName) {
                case PROP_USER_NAME:
                    return userName;
                case PROP_FULL_NAME:
                    return alfNode.getString();
                case PROP_IS_AVAILABLE:
                    return authenticationService.getAuthenticationEnabled(userName);
                case PROP_IS_MUTABLE:
                    return authenticationService.isAuthenticationMutable(userName);
                case PROP_IS_ADMIN:
                    return authorityService.isAdminAuthority(userName);
                case PROP_AUTHORITIES:
                    return getUserAuthorities();
                case "_formKey":
                    return "FORM_KEY_FOR_PERSON";
            }

            return alfNode.getAttribute(attributeName);
        }
    }

    private class UserAuthorities implements MetaValue {

        private String userName;
        private Set<String> authorities;

        UserAuthorities(String userName) {
            this.userName = userName;
        }

        @Override
        public String getString() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            switch (name) {
                case "list":
                    return new ArrayList<>(getAuthorities());
            }
            return null;
        }

        private Set<String> getAuthorities() {
            if (authorities == null) {
                authorities = authorityUtils.getUserAuthorities(userName);
            }
            return authorities;
        }

        @Override
        public boolean hasAttribute(String authority) {
            return getAuthorities().contains(authority);
        }
    }
}