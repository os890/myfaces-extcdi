/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.extensions.cdi.jsf.impl.listener.request;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowContext;
import org.apache.myfaces.extensions.cdi.jsf.api.config.JsfModuleConfig;
import org.apache.myfaces.extensions.cdi.jsf.api.listener.phase.JsfLifecyclePhaseInformation;
import org.apache.myfaces.extensions.cdi.jsf.api.listener.request.AfterFacesRequest;
import org.apache.myfaces.extensions.cdi.message.api.Message;

import javax.enterprise.event.Observes;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Gerhard Petracek
 */
public class FacesMessageObserver
{
    @Inject
    private JsfLifecyclePhaseInformation lifecyclePhaseInformation;

    @Inject
    private WindowContext windowContext;

    private boolean alwaysKeepMessages;

    protected FacesMessageObserver()
    {
    }

    @Inject
    protected FacesMessageObserver(JsfModuleConfig jsfModuleConfig)
    {
        this.alwaysKeepMessages = jsfModuleConfig.isAlwaysKeepMessages();
    }

    protected void saveFacesMessages(@Observes @AfterFacesRequest FacesContext facesContext)
    {
        if(this.alwaysKeepMessages && !this.lifecyclePhaseInformation.isRenderResponsePhase())
        {
            Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();

            @SuppressWarnings({"unchecked"})
            List<FacesMessageEntry> facesMessageEntryList =
                    (List<FacesMessageEntry>)requestMap.get(Message.class.getName());

            if(facesMessageEntryList == null)
            {
                facesMessageEntryList = new CopyOnWriteArrayList<FacesMessageEntry>();
            }
            this.windowContext.setAttribute(Message.class.getName(), facesMessageEntryList, true);
        }
    }
}