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
package ru.citeck.ecos.action;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;


public class CustomExecuteScriptJob implements Job {
    private static final String PARAM_SCRIPT_LOCATION = "scriptLocation";
    private static final String PARAM_SCRIPT_SERVICE = "scriptService";
    private static final String PARAM_AUTHENTICATION_COMPONENT = "authenticationComponent";
    private static final String PARAMS = "params";


    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobData = context.getJobDetail().getJobDataMap();

        // Get the script service from the job map
        Object scriptServiceObj = jobData.get(PARAM_SCRIPT_SERVICE);
        if (!(scriptServiceObj instanceof ScriptService)) {
            throw new AlfrescoRuntimeException(
                    "ExecuteScriptJob data must contain valid script service");
        }

        // Get the script location from the job map
        Object scriptLocationObj = jobData.get(PARAM_SCRIPT_LOCATION);
        if (!(scriptLocationObj instanceof ScriptLocation)) {
            throw new AlfrescoRuntimeException(
                    "ExecuteScriptJob data must contain valid script location");
        }

        // Get the authentication component from the job map
        Object authenticationComponentObj = jobData.get(PARAM_AUTHENTICATION_COMPONENT);
        if (!(authenticationComponentObj instanceof AuthenticationComponent)) {
            throw new AlfrescoRuntimeException(
                    "ExecuteScriptJob data must contain valid authentication component");
        }


        // Execute the script as the system user
        ((AuthenticationComponent)authenticationComponentObj).setSystemUserAsCurrentUser();
        try
        {
            Map<String,Object> params = (Map<String,Object>)jobData.get(PARAMS);
            // Execute the script
            ((ScriptService)scriptServiceObj).executeScript((ScriptLocation)scriptLocationObj, params);
        }
        finally
        {
            ((AuthenticationComponent)authenticationComponentObj).clearCurrentSecurityContext();
        }
    }

}
