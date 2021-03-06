/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artificer.server;

import org.artificer.repository.AuditManager;
import org.artificer.repository.audit.AuditEntrySet;
import org.artificer.repository.query.ArtificerQueryArgs;
import org.artificer.server.core.api.AuditService;
import org.artificer.server.core.api.PagedResult;
import org.jboss.downloads.artificer._2013.auditing.AuditEntry;

import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.util.List;

/**
 * @author Brett Meyer.
 */
@Stateful(name = "AuditService")
@Remote(AuditService.class)
// Required so that artificer-repository-hibernate can control the transactions during EJB calls.
@TransactionManagement(TransactionManagementType.BEAN)
public class AuditServiceImpl extends AbstractServiceImpl implements AuditService {

    @Override
    public AuditEntry create(String artifactUuid, AuditEntry auditEntry) throws Exception {
        AuditManager auditManager = auditManager();
        return auditManager.addAuditEntry(artifactUuid, auditEntry);
    }

    @Override
    public AuditEntry get(String artifactUuid, String auditEntryUuid) throws Exception {
        AuditManager auditManager = auditManager();
        return auditManager.getArtifactAuditEntry(artifactUuid, auditEntryUuid);
    }

    @Override
    public List<AuditEntry> queryByArtifact(String artifactUuid) throws Exception {
        AuditManager auditManager = auditManager();
        AuditEntrySet results = auditManager.getArtifactAuditEntries(artifactUuid, new ArtificerQueryArgs());
        return results.list();
    }

    @Override
    public PagedResult<AuditEntry> queryByArtifact(String artifactUuid, Integer startPage, Integer startIndex, Integer count)
            throws Exception {
        AuditManager auditManager = auditManager();
        ArtificerQueryArgs args = new ArtificerQueryArgs(startPage, startIndex, count);
        AuditEntrySet results = auditManager.getArtifactAuditEntries(artifactUuid, args);
        return new PagedResult<>(results.list(), "", results.totalSize(), args);
    }

    @Override
    public List<AuditEntry> queryByUser(String username) throws Exception {
        AuditManager auditManager = auditManager();
        AuditEntrySet results = auditManager.getUserAuditEntries(username, new ArtificerQueryArgs());
        return results.list();
    }

    @Override
    public PagedResult<AuditEntry> queryByUser(String username, Integer startPage, Integer startIndex, Integer count)
            throws Exception {
        AuditManager auditManager = auditManager();
        ArtificerQueryArgs args = new ArtificerQueryArgs(startPage, startIndex, count);
        AuditEntrySet results = auditManager.getUserAuditEntries(username, args);
        return new PagedResult<>(results.list(), "", results.totalSize(), args);
    }
}
