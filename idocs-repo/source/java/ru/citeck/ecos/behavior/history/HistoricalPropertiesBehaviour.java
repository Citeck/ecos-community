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
package ru.citeck.ecos.behavior.history;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.I18NUtil;
import ru.citeck.ecos.history.HistoryService;
import ru.citeck.ecos.model.ClassificationModel;
import ru.citeck.ecos.model.HistoryModel;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricalPropertiesBehaviour implements 
	NodeServicePolicies.OnCreateNodePolicy,
	NodeServicePolicies.OnUpdatePropertiesPolicy,
	NodeServicePolicies.OnCreateAssociationPolicy,
	NodeServicePolicies.OnDeleteAssociationPolicy,
	NodeServicePolicies.OnCreateChildAssociationPolicy,
	NodeServicePolicies.OnDeleteChildAssociationPolicy	
{

	private static final Serializable NODE_CREATED = "node.created";
	private static final Serializable NODE_UPDATED = "node.updated";
	private static final Serializable ASSOC_ADDED = "assoc.added";
	private static final Serializable ASSOC_REMOVED = "assoc.removed";
	private PolicyComponent policyComponent;
	private NodeService nodeService;
	private HistoryService historyService;
		
	private QName className;
	private List<QName> allowedProperties;
	private List<QName> ignoreAssocsWithTypes;
    private DictionaryService dictionaryService;
	private static Map<String,Long> createdNodes = new HashMap<String,Long>();
	protected boolean enableHistoryOnCreateNode;
	protected boolean enableHistoryOnUpdateProps;
	protected boolean enableHistoryOnAddAssocs;
	protected boolean enableHistoryOnDeleteAssocs;
	protected boolean enableHistoryOnAddChildAssocs;
	protected boolean enableHistoryOnDeleteChildAssocs;

    private static final Log logger = LogFactory.getLog(HistoricalPropertiesBehaviour.class);
	
	public void init() {		
		//added		
		policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, 
				className, new JavaBehaviour(this, "onCreateNode", NotificationFrequency.FIRST_EVENT));
		
		policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, className, 
				new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.EVERY_EVENT));
				
		policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateAssociationPolicy.QNAME, className,
				new JavaBehaviour(this, "onCreateAssociation", NotificationFrequency.TRANSACTION_COMMIT));
				
		policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnDeleteAssociationPolicy.QNAME, className,
				new JavaBehaviour(this, "onDeleteAssociation", NotificationFrequency.TRANSACTION_COMMIT)
        );
		policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateChildAssociationPolicy.QNAME, className,
				new JavaBehaviour(this, "onCreateChildAssociation", NotificationFrequency.TRANSACTION_COMMIT)
        );
		policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnDeleteChildAssociationPolicy.QNAME, className,
				new JavaBehaviour(this, "onDeleteChildAssociation", NotificationFrequency.TRANSACTION_COMMIT)
        );
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setHistoryService(HistoryService historyService) {
		this.historyService = historyService;
	}
	
	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		logger.debug("HistoricalPropertiesBehaviour onCreateNode="+this);
		logger.debug("HistoricalPropertiesBehaviour onCreateNode="+childAssocRef);
		NodeRef nodeRef = childAssocRef.getChildRef();
		NodeRef.Status status = nodeService.getNodeStatus(nodeRef);
		synchronized (createdNodes) {
			createdNodes.put(nodeRef.getId(),status.getDbTxnId());
		}
		logger.debug("HistoricalPropertiesBehaviour onCreateNode1="+isNewNode(nodeRef));
	}
	
	private boolean isNewNode(NodeRef nodeRef) {
		NodeRef.Status status = nodeService.getNodeStatus(nodeRef);
		synchronized (createdNodes) {
			Long createdDbTxnId = createdNodes.get(nodeRef.getId());
			if (createdDbTxnId!=null) {
				if (createdDbTxnId.equals(status.getDbTxnId())) {
					return true;
				} else {
					// Remove from cache if not new
					createdNodes.remove(nodeRef.getId());
					return false;
				}
			} else {
				return false;
			}
		}
	}
	
	@Override
	public void onUpdateProperties(final NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) 
	{
		logger.debug("HistoricalPropertiesBehaviour onUpdateProperties="+this);
		logger.debug("HistoricalPropertiesBehaviour onUpdateProperties="+nodeRef);
		logger.debug("HistoricalPropertiesBehaviour onUpdateProperties="+isNewNode(nodeRef));
		
		logger.debug("onUpdateProperties event");
		if(!isNewNode(nodeRef) && enableHistoryOnUpdateProps && nodeService.exists(nodeRef) && className!=null && className.equals(nodeService.getType(nodeRef))) 
		{
			if(before.get(ContentModel.PROP_NODE_UUID)!=null )
			{
				logger.debug("onUpdateProperties edit mode");
				Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
				for(Map.Entry<QName, Serializable> entry : properties.entrySet()) {
					Object propBefore = (Object) before.get(entry.getKey());
					Object propAfter = (Object) after.get(entry.getKey());
					if(allowedProperties!=null && allowedProperties.contains(entry.getKey()))
					{
						if((propBefore!=null && !propBefore.equals(propAfter))||(propBefore==null && (propAfter!=null && !"".equals(propAfter))))
						{
							logger.debug("onUpdateProperties propBefore "+propBefore+" propAfter "+propAfter);
							Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
							eventProperties.put(HistoryModel.PROP_NAME, NODE_UPDATED);
							eventProperties.put(HistoryModel.ASSOC_DOCUMENT, nodeRef);
							eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, entry.getKey());
							eventProperties.put(HistoryModel.PROP_PROPERTY_VALUE, String.valueOf(propAfter));
							String comment = getKeyValue(entry.getKey())
									+ ": "
									+ getKeyValue(entry.getKey(), propBefore)
									+ " -> "
									+ getKeyValue(entry.getKey(), propAfter);
							if ("content".equals(entry.getKey().getLocalName())) {
								comment = getKeyValue(entry.getKey());
							}
							eventProperties.put(HistoryModel.PROP_TASK_COMMENT, comment);
							historyService.persistEvent(HistoryModel.TYPE_BASIC_EVENT, eventProperties);
						}
					}
				}
			}
		}
	}

	@Override
	public void onCreateAssociation(AssociationRef nodeAssocRef) {
		logger.debug("HistoricalPropertiesBehaviour onCreateAssociation="+this);
		logger.debug("onCreateAssociation event");
		AssociationDefinition assoc = dictionaryService.getAssociation(nodeAssocRef.getTypeQName());
		NodeRef nodeSource = nodeAssocRef.getSourceRef();
		NodeRef nodeTarget = nodeAssocRef.getTargetRef();
		if(!isNewNode(nodeAssocRef.getSourceRef()) && enableHistoryOnAddAssocs && nodeService.exists(nodeSource) && className!=null && className.equals(nodeService.getType(nodeSource)) && allowedProperties!=null && allowedProperties.contains(nodeAssocRef.getTypeQName()))
		{
			if(assoc!=null) {
				Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
				eventProperties.put(HistoryModel.PROP_NAME, ASSOC_ADDED);
				eventProperties.put(HistoryModel.ASSOC_DOCUMENT, nodeSource);
				eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, assoc.getName());
				eventProperties.put(HistoryModel.PROP_PROPERTY_VALUE, nodeTarget.toString());

				String comment = getAssocKeyValue(assoc.getName())
						+ ": "
						+ getChangeValue(nodeAssocRef.getTargetRef());
				eventProperties.put(HistoryModel.PROP_TASK_COMMENT, comment);
				historyService.persistEvent(HistoryModel.TYPE_BASIC_EVENT, eventProperties);
			}
		}
	}
	
	@Override
	public void onDeleteAssociation(AssociationRef nodeAssocRef) {
		logger.debug("onDeleteAssociation event");
		NodeRef nodeSource = nodeAssocRef.getSourceRef();
		AssociationDefinition assoc = dictionaryService.getAssociation(nodeAssocRef.getTypeQName());
		if(!isNewNode(nodeAssocRef.getSourceRef()) && enableHistoryOnDeleteAssocs && nodeService.exists(nodeSource) && className!=null && className.equals(nodeService.getType(nodeSource)) && allowedProperties!=null && allowedProperties.contains(nodeAssocRef.getTypeQName()))
		{
			if(assoc!=null) {
				Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
				eventProperties.put(HistoryModel.PROP_NAME, ASSOC_REMOVED);
				eventProperties.put(HistoryModel.ASSOC_DOCUMENT, nodeSource);
				eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, assoc.getName());
				String comment = getAssocKeyValue(assoc.getName())
						+ ": "
						+ getChangeValue(nodeAssocRef.getTargetRef());
				eventProperties.put(HistoryModel.PROP_TASK_COMMENT, comment);
				historyService.persistEvent(HistoryModel.TYPE_BASIC_EVENT, eventProperties);
			}
		}
	}

	@Override
	public void onCreateChildAssociation(ChildAssociationRef childAssociationRef, boolean isNew) {
		logger.debug("HistoricalPropertiesBehaviour onCreateChildAssociation="+this);
		
		
		logger.debug("onCreateChildAssociation event");
		AssociationDefinition assoc = dictionaryService.getAssociation(childAssociationRef.getTypeQName());
		NodeRef nodeSource = childAssociationRef.getParentRef();
		NodeRef nodeTarget = childAssociationRef.getChildRef();
		if(enableHistoryOnAddChildAssocs && nodeService.exists(nodeSource) && className!=null && className.equals(nodeService.getType(nodeSource)) && allowedProperties!=null && allowedProperties.contains(childAssociationRef.getTypeQName()) && nodeService.exists(nodeTarget))
		{
			if(assoc!=null && (ignoreAssocsWithTypes==null || ignoreAssocsWithTypes!=null && !ignoreAssocsWithTypes.contains(nodeService.getType(nodeTarget)))) {
				Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
				eventProperties.put(HistoryModel.PROP_NAME, ASSOC_ADDED);
				eventProperties.put(HistoryModel.ASSOC_DOCUMENT, nodeSource);
				eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, assoc.getName());
				eventProperties.put(HistoryModel.PROP_PROPERTY_VALUE, nodeTarget.toString());
				eventProperties.put(HistoryModel.PROP_TARGET_NODE_TYPE, nodeService.getProperty(nodeTarget, ClassificationModel.PROP_DOCUMENT_TYPE));
				eventProperties.put(HistoryModel.PROP_TARGET_NODE_KIND, nodeService.getProperty(nodeTarget, ClassificationModel.PROP_DOCUMENT_KIND));
				historyService.persistEvent(HistoryModel.TYPE_BASIC_EVENT, eventProperties);
			}
		}
	}
	
	@Override
	public void onDeleteChildAssociation(ChildAssociationRef childAssociationRef) {
		logger.debug("onDeleteChildAssociation event");
		AssociationDefinition assoc = dictionaryService.getAssociation(childAssociationRef.getTypeQName());
		NodeRef nodeSource = childAssociationRef.getParentRef();
		NodeRef nodeTarget = childAssociationRef.getChildRef();
		if(enableHistoryOnDeleteChildAssocs && nodeService.exists(nodeSource) && className!=null && className.equals(nodeService.getType(nodeSource)) && allowedProperties!=null && allowedProperties.contains(childAssociationRef.getTypeQName()))
		{
			if(assoc!=null && (ignoreAssocsWithTypes==null || ignoreAssocsWithTypes!=null && nodeService.exists(nodeTarget) && !ignoreAssocsWithTypes.contains(nodeService.getType(nodeTarget)))) {
				Map<QName, Serializable> eventProperties = new HashMap<QName, Serializable>(7);
				eventProperties.put(HistoryModel.PROP_NAME, ASSOC_REMOVED);
				eventProperties.put(HistoryModel.ASSOC_DOCUMENT, nodeSource);
				eventProperties.put(HistoryModel.PROP_PROPERTY_NAME, assoc.getName());
				eventProperties.put(HistoryModel.PROP_TARGET_NODE_TYPE, nodeService.getProperty(nodeTarget, ClassificationModel.PROP_DOCUMENT_TYPE));
				eventProperties.put(HistoryModel.PROP_TARGET_NODE_KIND, nodeService.getProperty(nodeTarget, ClassificationModel.PROP_DOCUMENT_KIND));
				historyService.persistEvent(HistoryModel.TYPE_BASIC_EVENT, eventProperties);
			}
		}
	}
	
	private String getKeyValue(QName qName) {
		String modelName = dictionaryService.getProperty(qName).getModel().getName().getPrefixString().replace(":", "_");
		String propName = dictionaryService.getProperty(qName).getName().getPrefixString().replace(":", "_");
		return I18NUtil.getMessage(modelName + ".property." + propName + ".title");
	}

	private Object getKeyValue(QName qName, Object constraint) {
		if ("boolean".equals(dictionaryService.getProperty(qName).getDataType().getName().getLocalName())) {
			if (constraint == null || constraint.equals(false)) {
				return "Нет";
			} else {
				return  "Да";
			}
		}
		if (constraint != null && "date".equals(dictionaryService.getProperty(qName).getDataType().getName().getLocalName())) {
			return new SimpleDateFormat("dd/MM/yyyy").format(constraint);
		}
		if (constraint != null && ClassificationModel.PROP_DOCUMENT_KIND.equals(qName)) {
			return nodeService.getProperty((NodeRef) constraint, ContentModel.PROP_NAME);
		}
		if (dictionaryService.getProperty(qName).getConstraints().size() > 0) {
			String localName = dictionaryService.getProperty(qName).getConstraints().get(0).getConstraint().getShortName().replace(":", "_");
			return I18NUtil.getMessage("listconstraint." + localName + "." + constraint);
		} else {
			return constraint == null ? "" : constraint;
		}
	}

	private String getChangeValue(NodeRef nodeRef) {
		if (ContentModel.TYPE_PERSON.equals(nodeService.getType(nodeRef))) {
			return nodeService.getProperty(nodeRef, ContentModel.PROP_LASTNAME)
					+ " " + nodeService.getProperty(nodeRef, ContentModel.PROP_FIRSTNAME);
		} else {
			return String.valueOf(nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE) != null
					? nodeService.getProperty(nodeRef, ContentModel.PROP_TITLE)
					: nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
		}
	}

	private String getAssocKeyValue(QName qName) {
		String modelName = dictionaryService.getAssociation(qName).getModel().getName().getPrefixString().replace(":", "_");
		String propName = dictionaryService.getAssociation(qName).getName().getPrefixString().replace(":", "_");
		return I18NUtil.getMessage(modelName + ".association." + propName + ".title");
	}

	public void setAllowedProperties(List<QName> allowedProperties) {
		this.allowedProperties = allowedProperties;
	}
	
	public void setClassName(QName className) {
		this.className = className;
	}

	public void setIgnoreAssocsWithTypes(List<QName> ignoreAssocsWithTypes) {
		this.ignoreAssocsWithTypes = ignoreAssocsWithTypes;
	}
	
	/**
	* enabled
	* @param true or false
	*/
	public void setEnableHistoryOnUpdateProps(Boolean enableHistoryOnUpdateProps) {
    	this.enableHistoryOnUpdateProps = enableHistoryOnUpdateProps.booleanValue();
    }
	/**
	* enabled
	* @param true or false
	*/
	public void setEnableHistoryOnAddAssocs(Boolean enableHistoryOnAddAssocs) {
    	this.enableHistoryOnAddAssocs = enableHistoryOnAddAssocs.booleanValue();
    }
	/**
	* enabled
	* @param true or false
	*/
	public void setEnableHistoryOnDeleteAssocs(Boolean enableHistoryOnDeleteAssocs) {
    	this.enableHistoryOnDeleteAssocs = enableHistoryOnDeleteAssocs.booleanValue();
    }
	/**
	* enabled
	* @param true or false
	*/
	public void setEnableHistoryOnAddChildAssocs(Boolean enableHistoryOnAddChildAssocs) {
    	this.enableHistoryOnAddChildAssocs = enableHistoryOnAddChildAssocs.booleanValue();
    }
	/**
	* enabled
	* @param true or false
	*/
	public void setEnableHistoryOnDeleteChildAssocs(Boolean enableHistoryOnDeleteChildAssocs) {
    	this.enableHistoryOnDeleteChildAssocs = enableHistoryOnDeleteChildAssocs.booleanValue();
    }
}
