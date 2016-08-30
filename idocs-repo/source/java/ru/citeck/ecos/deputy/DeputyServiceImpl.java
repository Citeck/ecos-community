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
package ru.citeck.ecos.deputy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.citeck.ecos.model.DeputyModel;
import ru.citeck.ecos.orgstruct.OrgStructService;

import java.util.*;

public class DeputyServiceImpl implements DeputyService
{
	private static final Log logger = LogFactory.getLog(DeputyServiceImpl.class);
	
	private AuthenticationService authenticationService;
	private AuthorityService authorityService;
	private NodeService nodeService;
	private OrgStructService orgStructService;
	private AuthorityHelper authorityHelper;
	private AvailabilityService availabilityService;
	
	private CompositeDeputyListener deputyListener = new CompositeDeputyListener();
	
	private NodeRef deputationRecordsRoot;
	private QName deputationRecordAssoc;
	private String roleGroupType;
	private String branchGroupType;

	/////////////////////////////////////////////////////////////////
	//                      SPRING INTERFACE                       //
	/////////////////////////////////////////////////////////////////
	
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setOrgStructService(OrgStructService orgStructService) {
		this.orgStructService = orgStructService;
	}
	
	public void setAuthorityHelper(AuthorityHelper authorityHelper) {
		this.authorityHelper = authorityHelper;
	}
	
	public void setAvailabilityService(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	public void setDeputationRecordsRoot(NodeRef deputationRecordsRoot) {
		this.deputationRecordsRoot = deputationRecordsRoot;
	}

	public void setDeputationRecordAssoc(QName deputationRecordAssoc) {
		this.deputationRecordAssoc = deputationRecordAssoc;
	}

	public void setRoleGroupType(String roleGroupType) {
		this.roleGroupType = roleGroupType;
	}

    public void setBranchGroupType(String branchGroupType) {
        this.branchGroupType = branchGroupType;
    }
	

	/////////////////////////////////////////////////////////////////
	//                       ADMIN INTERFACE                       //
	/////////////////////////////////////////////////////////////////
	
	@Override
	public List<String> getUserDeputies(String userName) {
		NodeRef user = authorityHelper.needUser(userName);
		return getAuthorityNames(getAuthorityDeputies(user));
	}

	@Override
	public void addUserDeputies(String userName, List<String> deputies) {
		NodeRef user = authorityHelper.needUser(userName);
		addDeputationRecords(user, getAuthorities(deputies), false);
	}

	@Override
	public void removeUserDeputies(String userName, List<String> deputies) {
		NodeRef user = authorityHelper.needUser(userName);
		removeDeputationRecords(user, getAuthorities(deputies), false);
	}

	@Override
	public List<String> getUsersWhoHaveThisUserDeputy(String userName) {
		NodeRef user = authorityHelper.needUser(userName);
		return getAuthorityNames(getUsersDeputiedToUserImpl(user));
	}

	@Override
	public List<String> getRoleDeputies(String roleFullName) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		return getAuthorityNames(getAuthorityDeputies(role));
	}

	@Override
	public void addRoleDeputies(String roleFullName, List<String> deputies) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		addDeputationRecords(role, getAuthorities(deputies), true);
	}

	@Override
	public void removeRoleDeputies(String roleFullName, List<String> deputies) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		removeDeputationRecords(role, getAuthorities(deputies), true);
	}

	@Override
	public boolean isRoleDeputiedByMembers(String roleFullName) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		return isRoleDeputiedByMembers(role);
	}

	@Override
	public boolean isRoleDeputiedByUser(String roleFullName, String userName) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		NodeRef user = authorityHelper.needUser(userName);
		
		// zero - all roles are deputied by admins
		if(authorityService.isAdminAuthority(userName)) {
			return true;
		}
		
		// first - role should be deputied by members
		if(!isRoleDeputiedByMembers(role)) {
			return false;
		}
		
		// second - user should be the member of this role
		Set<String> userGroups = authorityService.getAuthoritiesForUser(userName);
		if(!userGroups.contains(roleFullName)) {
			return false;
		}
		
		// third - user should not be the deputy of this role
		NodeRef deputationRecord = this.getDeputationRecord(role, user);
		if(deputationRecord != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public boolean isRoleDeputiedToUser(String roleFullName, String userName) {
		NodeRef role = authorityHelper.needRole(roleFullName);
		NodeRef user = authorityHelper.needUser(userName);
		return getDeputationRecord(role, user) != null;
	}

	@Override
	public List<String> getUserRoles(String userName) {
		NodeRef user = authorityHelper.needUser(userName);
		// get all groups where user is member
		List<String> userRoles = orgStructService.getTypedGroupsForUser(userName, roleGroupType);
		
		// and exclude those, where user is deputy
		List<NodeRef> deputiedRoles = getRolesDeputiedToUserImpl(user);

		// use HashSet for fast search
		Set<NodeRef> deputiedRoleSet = new HashSet<NodeRef>(deputiedRoles.size());
		deputiedRoleSet.addAll(deputiedRoles);
		
		List<String> nonDeputiedRoles = new ArrayList<String>(deputiedRoles.size());
		for(String roleName : userRoles) {
			NodeRef role = authorityHelper.getRole(roleName);
			if(!deputiedRoleSet.contains(role)) {
				nonDeputiedRoles.add(roleName);
			}
		}
		return nonDeputiedRoles;
	}

    @Override
    public List<String> getUserBranches(String userName) {
        return orgStructService.getTypedGroupsForUser(userName, branchGroupType);
    }

    @Override
	public List<String> getRoleMembers(String roleFullName) {
		// get all members of the role:
		Set<String> members = authorityService.getContainedAuthorities(AuthorityType.USER, roleFullName, false);
		
		// and exclude those, that are deputies
		List<String> deputies = this.getRoleDeputies(roleFullName);
		Set<String> deputiesSet = new HashSet<String>(deputies.size());
		deputiesSet.addAll(deputies);
		
		List<String> fullMembers = new ArrayList<String>(members.size());
		for(String member : members) {
			if(!deputiesSet.contains(member)) {
				fullMembers.add(member);
			}
		}
		
		return fullMembers;
	}
	
	@Override
	public List<String> getRolesDeputiedByUser(String userName) {
		NodeRef user = authorityHelper.needUser(userName);
		// get all groups where user is member
		List<String> userRoles = orgStructService.getTypedGroupsForUser(userName, roleGroupType);
		
		// get those, where user is deputy
		List<NodeRef> deputiedRoles = getRolesDeputiedToUserImpl(user);

		// use HashSet for fast search
		Set<NodeRef> deputiedRoleSet = new HashSet<NodeRef>(deputiedRoles.size());
		deputiedRoleSet.addAll(deputiedRoles);
		
		List<String> managedRoles = new ArrayList<String>(userRoles.size());
		for(String roleName : userRoles) {
			NodeRef role = authorityHelper.getRole(roleName);
			// role should be not deputied to user
			// and it should be deputied (managed) by members
			if(!deputiedRoles.contains(role) && isRoleDeputiedByMembers(role)) {
				managedRoles.add(roleName);
			}
		}
		return managedRoles;
	}

	@Override
	public List<String> getRolesDeputiedToUser(String userName) {
		NodeRef user = authorityHelper.needUser(userName);
		return getAuthorityNames(getRolesDeputiedToUserImpl(user));
	}
	
	/////////////////////////////////////////////////////////////////
	//                  CURRENT USER INTERFACE                     //
	/////////////////////////////////////////////////////////////////

	@Override
	public List<String> getCurrentUserDeputies() {
        return getUserDeputies(getCurrentUserName());
	}

	@Override
	public void addCurrentUserDeputies(List<String> deputies) {
		addUserDeputies(getCurrentUserName(), deputies);
	}

	@Override
	public void removeCurrentUserDeputies(List<String> deputies) {
		removeUserDeputies(getCurrentUserName(), deputies);
	}

	@Override
	public boolean isRoleDeputiedByCurrentUser(final String roleFullName) {
        return isRoleDeputiedByUser(roleFullName, getCurrentUserName());
	}

	@Override
	public boolean isRoleDeputiedToCurrentUser(final String roleFullName) {
        return isRoleDeputiedToUser(roleFullName, getCurrentUserName());
	}

	@Override
	public List<String> getCurrentUserRoles() {
        return getUserRoles(getCurrentUserName());
	}

    @Override
    public List<String> getCurrentUserBranches() {
        return getUserBranches(getCurrentUserName());
    }

    @Override
	public List<String> getRolesDeputiedByCurrentUser() {
        return getRolesDeputiedByUser(getCurrentUserName());
	}

	@Override
	public List<String> getRolesDeputiedToCurrentUser() {
		return getRolesDeputiedToUser(getCurrentUserName());
	}
	
	/////////////////////////////////////////////////////////////////
	//                     LISTENER INTERFACE                      //
	/////////////////////////////////////////////////////////////////
	
	public void addDeputyListener(DeputyListener listener) {
		deputyListener.addDeputyListener(listener);
	}

	@Override
	public void userAvailabilityChanged(final String userName) {
		AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Object>() {
			@Override
			public Object doWork() throws Exception {
				authorityHelper.needUser(userName);
				boolean available = availabilityService.getUserAvailability(userName);
				List<String> userRoles = getUserRoles(userName);
				List<String> deputiedRoles = getRolesDeputiedToUser(userName);
				if(available) {
					deputyListener.onUserAvailable(userName);
					for(String role : userRoles) {
						deputyListener.onRoleMemberAvailable(role, userName);
					}
					for(String role : deputiedRoles) {
						deputyListener.onRoleDeputyAvailable(role, userName);
					}
				} else {
					deputyListener.onUserUnavailable(userName);
					for(String role : userRoles) {
						deputyListener.onRoleMemberUnavailable(role, userName);
					}
					for(String role : deputiedRoles) {
						deputyListener.onRoleDeputyUnavailable(role, userName);
					}
				}
				return null;
			}
		});
	}

	@Override
	public void userMembershipChanged(String userName, String groupFullName) {
		if(!authorityHelper.isRole(groupFullName)) {
			return;
		}
		authorityHelper.needUser(userName);
		
		// check whether user is in group
		Set<String> userGroups = authorityService.getAuthoritiesForUser(userName);

		if(userGroups.contains(groupFullName)) {
			deputyListener.onRoleMemberAvailable(groupFullName, userName);
		} else {
			deputyListener.onRoleMemberUnavailable(groupFullName, userName);
		}
	}

	/////////////////////////////////////////////////////////////////
	//                       PRIVATE STUFF                         //
	/////////////////////////////////////////////////////////////////
	
	private String getCurrentUserName() {
		return authenticationService.getCurrentUserName();
	}
	
	private List<NodeRef> getAuthorities(Collection<String> authorityNames) {
		List<NodeRef> authorities = new ArrayList<NodeRef>(authorityNames.size());
		for(String authorityName : authorityNames) {
			NodeRef authority = authorityHelper.getAuthority(authorityName);
			if(authority != null) {
				authorities.add(authority);
			}
		}
		return authorities;
	}
	
	private List<String> getAuthorityNames(Collection<NodeRef> authorities) {
		List<String> authorityNames = new ArrayList<String>(authorities.size());
		for(NodeRef authority : authorities) {
			String authorityName = authorityHelper.getAuthorityName(authority);
			if(authorityName != null) {
				authorityNames.add(authorityName);
			}
		}
		return authorityNames;
	}
	
	// get deputies for specified authority
	private List<NodeRef> getAuthorityDeputies(NodeRef authority) {
		List<NodeRef> deputationRecords = 
				searchDeputationRecords(DeputyModel.ASSOC_DEPUTIED_AUTHORITY, authority);
		Map<NodeRef, NodeRef> deputyMap = mapDeputationRecordsAssocs(deputationRecords, 
				DeputyModel.ASSOC_DEPUTY);
		List<NodeRef> deputies = new ArrayList<NodeRef>(deputyMap.size());
		deputies.addAll(deputyMap.values());
		return deputies;
	}
	
	// get roles deputied to user - without checks
	private List<NodeRef> getRolesDeputiedToUserImpl(NodeRef user) {
		List<NodeRef> deputationRecords = 
				searchDeputationRecords(DeputyModel.ASSOC_DEPUTY, user);
		Map<NodeRef, NodeRef> deputyMap = mapDeputationRecordsAssocs(deputationRecords, 
				DeputyModel.ASSOC_DEPUTIED_AUTHORITY);
		return filterAuthoritiesByType(deputyMap.values(), ContentModel.TYPE_AUTHORITY_CONTAINER);
	}

	private List<NodeRef> getUsersDeputiedToUserImpl(NodeRef user) {
		List<NodeRef> deputationRecords =
				searchDeputationRecords(DeputyModel.ASSOC_DEPUTY, user);
		Map<NodeRef, NodeRef> deputyMap = mapDeputationRecordsAssocs(deputationRecords,
				DeputyModel.ASSOC_DEPUTIED_AUTHORITY);
		return filterAuthoritiesByType(deputyMap.values(), ContentModel.TYPE_PERSON);
	}
	
	private List<NodeRef> filterAuthoritiesByType(Collection<NodeRef> authorities, QName requiredType) {
		List<NodeRef> filtered = new ArrayList<NodeRef>(authorities.size());
		for(NodeRef authority : authorities) {
			QName type = nodeService.getType(authority);
			if(requiredType.equals(type)) {
				filtered.add(authority);
			}
		}
		return filtered;
	}
	
	// search deputation records by association
	private List<NodeRef> searchDeputationRecords(QName searchAssoc, NodeRef assocValue) {
		if(!nodeService.exists(assocValue)) {
			return Collections.emptyList();
		}
		List<AssociationRef> assocs = nodeService.getSourceAssocs(assocValue, searchAssoc);
		List<NodeRef> deputationRecords = new ArrayList<NodeRef>(assocs.size());
		for(AssociationRef assoc : assocs) {
			deputationRecords.add(assoc.getSourceRef());
		}
		return deputationRecords;
	}
	
	// get map of deputationRecords assocs
	private Map<NodeRef, NodeRef> mapDeputationRecordsAssocs(Collection<NodeRef> deputationRecords, 
			QName assocName) 
	{
		Map<NodeRef, NodeRef> results = new HashMap<NodeRef, NodeRef>(deputationRecords.size());
		for(NodeRef deputationRecord : deputationRecords) {
			List<AssociationRef> assocs = nodeService.getTargetAssocs(deputationRecord, assocName);
			if(assocs == null || assocs.size() == 0) {
				continue;
			}
			
			NodeRef authorityRef = assocs.get(0).getTargetRef();
			if(authorityRef != null && nodeService.exists(authorityRef)) {
				results.put(deputationRecord, authorityRef);
			}
		}
		return results;
	}
	
	// get deputation record or null, if no such
	private NodeRef getDeputationRecord(NodeRef deputiedAuthority, NodeRef deputy) {
		List<AssociationRef> assocs = nodeService.getSourceAssocs(deputy, DeputyModel.ASSOC_DEPUTY);
		for(AssociationRef assoc : assocs) {
			NodeRef deputyRecord = assoc.getSourceRef();
			List<AssociationRef> authorityRefs = nodeService.getTargetAssocs(deputyRecord, DeputyModel.ASSOC_DEPUTIED_AUTHORITY);
			if(authorityRefs == null || authorityRefs.size() == 0) {
				continue;
			}
			if(deputiedAuthority.equals(authorityRefs.get(0).getTargetRef())) {
				return deputyRecord;
			}
		}
		return null;
	}
	
	// add deputation record and return nodeRef to it
	private NodeRef addDeputationRecord(NodeRef deputiedAuthority, 
			NodeRef deputy, boolean deputiedRole) 
	{
		// first search for existing record:
		NodeRef deputationRecord = getDeputationRecord(deputiedAuthority, deputy);
		// if it exists - simply return it
		if(deputationRecord != null && nodeService.exists(deputationRecord)) {
			return deputationRecord;
		}
		
		// otherwise create new deputationRecord
		QName assocName = QName.createQName(DeputyModel.NAMESPACE, GUID.generate());
		ChildAssociationRef childAssocRef = nodeService.createNode(deputationRecordsRoot, 
				deputationRecordAssoc, assocName, DeputyModel.TYPE_DEPUTATION_RECORD);
		deputationRecord = childAssocRef.getChildRef();
		
		nodeService.createAssociation(deputationRecord, deputiedAuthority, DeputyModel.ASSOC_DEPUTIED_AUTHORITY);
		nodeService.createAssociation(deputationRecord, deputy, DeputyModel.ASSOC_DEPUTY);
		
		// and call listener
		String authorityName = authorityHelper.getAuthorityName(deputiedAuthority);
		String deputyName = authorityHelper.getAuthorityName(deputy);
		if(deputiedRole) {
			deputyListener.onRoleDeputyAvailable(authorityName, deputyName);
		} else {
			deputyListener.onUserDeputyAvailable(authorityName, deputyName);
		}
		
		return childAssocRef.getChildRef();
	}
	
	// add deputation records and return nodeRefs of them
	private List<NodeRef> addDeputationRecords(NodeRef deputiedAuthority, 
			Collection<NodeRef> deputies, boolean deputiedRole) 
	{
		List<NodeRef> deputationRecords = new ArrayList<NodeRef>(deputies.size());
		// then add all deputation records
		for(NodeRef deputy : deputies) {
			NodeRef deputationRecord = addDeputationRecord(deputiedAuthority, deputy, deputiedRole);
			deputationRecords.add(deputationRecord);
		}
		return deputationRecords;
	}

	// remove specified deputation record, if exists
	private void removeDeputationRecord(NodeRef deputiedAuthority, 
			NodeRef deputy, boolean deputiedRole)
	{
		// first search for existing record:
		NodeRef deputationRecord = getDeputationRecord(deputiedAuthority, deputy);
		// if it is null or does not exist - there is nothing to do
		if(deputationRecord == null || !nodeService.exists(deputationRecord)) {
			return;
		}

		// otherwise perform deletion
		nodeService.deleteNode(deputationRecord);
		
		// and call listener
		String authorityName = authorityHelper.getAuthorityName(deputiedAuthority);
		String deputyName = authorityHelper.getAuthorityName(deputy);
		if(deputiedRole) {
			deputyListener.onRoleDeputyUnavailable(authorityName, deputyName);
		} else {
			deputyListener.onUserDeputyUnavailable(authorityName, deputyName);
		}
	}
	
	// remove specified deputation records, if any
	private void removeDeputationRecords(NodeRef deputiedAuthority, 
			Collection<NodeRef> deputies, boolean deputiedRole)
	{
		// then remove all deputation records
		for(NodeRef deputy : deputies) {
			removeDeputationRecord(deputiedAuthority, deputy, deputiedRole);
		}
	}
	
	private boolean isRoleDeputiedByMembers(NodeRef role) {
		Boolean managed = (Boolean) nodeService.getProperty(role, DeputyModel.PROP_MANAGED_BY_MEMBERS);
		// role is not managed by members by default
		return Boolean.TRUE.equals(managed);
	}

}
