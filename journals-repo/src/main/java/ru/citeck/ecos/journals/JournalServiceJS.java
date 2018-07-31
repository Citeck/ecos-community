/*
 * Copyright (C) 2008-2015 Citeck LLC.
 *
 * This file is part of Citeck EcoS
 *
 * Citeck EcoS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Citeck EcoS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Citeck EcoS. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.citeck.ecos.journals;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.service.ServiceRegistry;

import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import ru.citeck.ecos.graphql.journal.JGqlPageInfoInput;
import ru.citeck.ecos.invariants.InvariantDefinition;
import ru.citeck.ecos.journals.records.JournalRecords;
import ru.citeck.ecos.utils.AlfrescoScopableProcessorExtension;

public class JournalServiceJS extends AlfrescoScopableProcessorExtension {
    
    private JournalService impl;
    private ServiceRegistry serviceRegistry;
    private NamespaceService namespaceService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ValueConverter valueConverter = new ValueConverter();

    public JournalTypeJS getJournalType(String id) {
        JournalType type = impl.getJournalType(id);
        return type == null ? null : new JournalTypeJS(type, serviceRegistry);
    }
    
    public JournalTypeJS[] getAllJournalTypes() {
        Collection<JournalType> journalTypes = impl.getAllJournalTypes();
        JournalTypeJS[] result = new JournalTypeJS[journalTypes.size()];
        int i = 0;
        for(JournalType type : journalTypes) {
            result[i++] = new JournalTypeJS(type, serviceRegistry);
        }
        return result;
    }

    public JournalRecords getRecordsLazy(String journalId,
                                         String query,
                                         String language,
                                         Object pageInfo) {

        JGqlPageInfoInput pageInfoInput = null;
        if (pageInfo != null) {
            Object javaPageInfo = valueConverter.convertValueForJava(pageInfo);
            pageInfoInput = objectMapper.convertValue(javaPageInfo, JGqlPageInfoInput.class);
        }

        return impl.getRecordsLazy(journalId, query, language, pageInfoInput);
    }

    public InvariantDefinition[] getCriterionInvariants(String journalId, Object attribute) {
        QName attributeQName;
        if (attribute instanceof String) {
            attributeQName = QName.resolveToQName(namespaceService, (String) attribute);
        } else {
            attributeQName = (QName) attribute;
        }
        List<InvariantDefinition> invariants = impl.getCriterionInvariants(journalId, attributeQName);
        return invariants.toArray(new InvariantDefinition[invariants.size()]);
    }

    public void setJournalService(JournalService impl) {
        this.impl = impl;
    }
    
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.namespaceService = serviceRegistry.getNamespaceService();
    }
    
}
