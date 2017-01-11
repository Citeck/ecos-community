package ru.citeck.ecos.history;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.surf.util.I18NUtil;
import ru.citeck.ecos.model.ClassificationModel;
import ru.citeck.ecos.model.HistoryModel;
import ru.citeck.ecos.utils.TransactionUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by andrey.kozlov on 20.12.2016.
 */
public class HistoryUtils {

    public static final Serializable NODE_CREATED = "node.created";
    public static final Serializable NODE_UPDATED = "node.updated";
    public static final Serializable ASSOC_ADDED = "assoc.added";
    public static final Serializable ASSOC_REMOVED = "assoc.removed";
    public static final Serializable ASSOC_UPDATED = "assoc.updated";

    public static Map<QName, Serializable> eventProperties(Serializable name, Serializable assocDocument, Serializable propertyName, Serializable propertyValue, Serializable taskComment, Serializable propTargetNodeType, Serializable propTargetNodeKind) {
        Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
        eventProperties.put(HistoryModel.PROP_NAME, name);
        eventProperties.put(HistoryModel.ASSOC_DOCUMENT, assocDocument);
        eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, propertyName);
        if (propertyValue != null) {
            eventProperties.put(HistoryModel.PROP_PROPERTY_VALUE, propertyValue);
        }
        eventProperties.put(HistoryModel.PROP_TASK_COMMENT, taskComment);
        if (propertyValue != null) {
            eventProperties.put(HistoryModel.PROP_TARGET_NODE_TYPE, propTargetNodeType);
        }
        if (propertyValue != null) {
            eventProperties.put(HistoryModel.PROP_TARGET_NODE_KIND, propTargetNodeKind);
        }
        return eventProperties;
    }


    public static String getKeyValue(QName qName, DictionaryService dictionaryService) {
        String modelName = dictionaryService.getProperty(qName).getModel().getName().getPrefixString().replace(":", "_");
        String propName = dictionaryService.getProperty(qName).getName().getPrefixString().replace(":", "_");
        return I18NUtil.getMessage(modelName + ".property." + propName + ".title");
    }

    public static Object getKeyValue(QName qName, Object constraint, DictionaryService dictionaryService, NodeService nodeService) {
        if (DataTypeDefinition.BOOLEAN.equals(dictionaryService.getProperty(qName).getDataType().getName())) {
            if (constraint == null || constraint.equals(false)) {
                return "Нет";
            } else {
                return  "Да";
            }
        }
        if (constraint == null) {
            return "";
        }
        if (DataTypeDefinition.DATE.equals(dictionaryService.getProperty(qName).getDataType().getName())) {
            return new SimpleDateFormat("dd/MM/yyyy").format(constraint);
        }
        if (ClassificationModel.PROP_DOCUMENT_KIND.equals(qName)) {
            return nodeService.getProperty((NodeRef) constraint, ContentModel.PROP_NAME);
        }
        if (dictionaryService.getProperty(qName).getConstraints().size() > 0) {
            String localName = dictionaryService.getProperty(qName).getConstraints().get(0).getConstraint().getShortName().replace(":", "_");
            return I18NUtil.getMessage("listconstraint." + localName + "." + constraint);
        } else {
            return constraint;
        }
    }

    public static String getChangeValue(NodeRef nodeRef, NodeService nodeService) {
        if (ContentModel.TYPE_PERSON.equals(nodeService.getType(nodeRef))) {
            return nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME)
                    + " " + nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME);
        } else {
            return String.valueOf(nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE) != null
                    ? nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE)
                    : nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
        }
    }

    public static String getAssocKeyValue(QName qName, DictionaryService dictionaryService) {
        String modelName = dictionaryService.getAssociation(qName).getModel().getName().getPrefixString().replace(":", "_");
        String propName = dictionaryService.getAssociation(qName).getName().getPrefixString().replace(":", "_");
        return I18NUtil.getMessage(modelName + ".association." + propName + ".title");
    }


    public static void addResourceTransaction(Serializable resourceKey, AssociationRef nodeAssocRef) {
        List<Serializable> listAssocRef = new ArrayList<>();
        listAssocRef.add(nodeAssocRef);
        if (AlfrescoTransactionSupport.getResource(resourceKey) != null) {
            List<AssociationRef> associationRefList = AlfrescoTransactionSupport.getResource(resourceKey);
            listAssocRef.addAll(associationRefList);
        }
        AlfrescoTransactionSupport.bindResource(resourceKey, listAssocRef);
    }

    public static void addUpdateResourseToTransaction(final Serializable resourceKey, final HistoryService historyService, final DictionaryService dictionaryService, final NodeService nodeService) {
        TransactionUtils.doBeforeCommit(new Runnable() {
            @Override
            public void run() {
                List<AssociationRef> added = new ArrayList<AssociationRef>();
                List<AssociationRef> removed =  new ArrayList<AssociationRef>();

                if (AlfrescoTransactionSupport.getResource(ASSOC_ADDED) != null) {
                    added.addAll((Collection<AssociationRef>) AlfrescoTransactionSupport.getResource(ASSOC_ADDED));
                }
                if (AlfrescoTransactionSupport.getResource(ASSOC_REMOVED) != null) {
                    removed.addAll((Collection<AssociationRef>) AlfrescoTransactionSupport.getResource(ASSOC_REMOVED));
                }
                if (added.size() > 0 && removed.size() > 0) {
                    Iterator<AssociationRef> iterAdded = added.iterator();
                    while (iterAdded.hasNext()) {
                        AssociationRef associationRefAdded = iterAdded.next();
                        Iterator<AssociationRef> iterRemoved = removed.iterator();
                        while (iterRemoved.hasNext()) {
                            AssociationRef associationRefRemoved = iterRemoved.next();
                            if (associationRefAdded.getTypeQName().equals(associationRefRemoved.getTypeQName())) {
                                historyService.persistEvent(
                                        HistoryModel.TYPE_BASIC_EVENT,
                                        HistoryUtils.eventProperties(
                                                ASSOC_UPDATED, associationRefAdded.getSourceRef(), associationRefAdded.getTypeQName(), associationRefAdded.getTargetRef().toString(), getAssocComment(associationRefAdded, associationRefRemoved, dictionaryService, nodeService), null, null
                                        )
                                );
                                iterAdded.remove();
                                iterRemoved.remove();break;
                            }
                        }
                    }
                    AlfrescoTransactionSupport.bindResource(ASSOC_ADDED, added);
                    AlfrescoTransactionSupport.bindResource(ASSOC_REMOVED, removed);
                } else if (added.size() > 0 || removed.size() > 0){
                    if (resourceKey.equals(ASSOC_ADDED)) {
                        Iterator<AssociationRef> iter = added.iterator();
                        while (iter.hasNext()) {
                            AssociationRef associationRefAdded = iter.next();
                            historyService.persistEvent(
                                    HistoryModel.TYPE_BASIC_EVENT,
                                    HistoryUtils.eventProperties(
                                            ASSOC_UPDATED, associationRefAdded.getSourceRef(), associationRefAdded.getTypeQName(), associationRefAdded.getTargetRef().toString(), getAssocComment(associationRefAdded, null, dictionaryService, nodeService), null, null
                                    )
                            );
                            iter.remove();
                        }
                        AlfrescoTransactionSupport.bindResource(resourceKey, added);
                    } else {
                        Iterator<AssociationRef> iter = removed.iterator();
                        while (iter.hasNext()) {
                            AssociationRef associationRefRemoved = iter.next();
                            historyService.persistEvent(
                                    HistoryModel.TYPE_BASIC_EVENT,
                                    HistoryUtils.eventProperties(
                                            ASSOC_UPDATED, associationRefRemoved.getSourceRef(), associationRefRemoved.getTypeQName(), null, getAssocComment(null, associationRefRemoved, dictionaryService, nodeService), null, null
                                    )
                            );
                            iter.remove();
                        }
                        AlfrescoTransactionSupport.bindResource(resourceKey, removed);
                    }
                }
            }
        });
    }

    public static String getAssocComment(AssociationRef added, AssociationRef removed, DictionaryService dictionaryService, NodeService nodeService) {
        if (added != null && removed != null) {
            return HistoryUtils.getAssocKeyValue(added.getTypeQName(), dictionaryService)
                    + ": "
                    + HistoryUtils.getChangeValue(removed.getTargetRef(), nodeService)
                    + " -> "
                    + HistoryUtils.getChangeValue(added.getTargetRef(), nodeService);
        } else if (added != null) {
            return HistoryUtils.getAssocKeyValue(added.getTypeQName(), dictionaryService)
                    + ": -> "
                    + HistoryUtils.getChangeValue(added.getTargetRef(), nodeService);
        } else if (removed != null) {
            return HistoryUtils.getAssocKeyValue(removed.getTypeQName(), dictionaryService)
                    + ": "
                    + HistoryUtils.getChangeValue(removed.getTargetRef(), nodeService)
                    + " -> ";
        } else {
            return "Something went wrong... Contact the administrator.";
        }
    }

}
