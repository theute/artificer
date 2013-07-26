/*
 * Copyright 2012 JBoss Inc
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
package org.overlord.sramp.wagon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.OutputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactEnum;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.BaseArtifactType;
import org.oasis_open.docs.s_ramp.ns.s_ramp_v1.ExtendedArtifactType;
import org.overlord.sramp.atom.archive.SrampArchive;
import org.overlord.sramp.atom.archive.SrampArchiveEntry;
import org.overlord.sramp.atom.archive.SrampArchiveException;
import org.overlord.sramp.atom.archive.expand.DefaultMetaDataFactory;
import org.overlord.sramp.atom.archive.expand.MetaDataProvider;
import org.overlord.sramp.atom.archive.expand.ZipToSrampArchive;
import org.overlord.sramp.atom.archive.expand.registry.ZipToSrampArchiveRegistry;
import org.overlord.sramp.atom.err.SrampAtomException;
import org.overlord.sramp.client.SrampAtomApiClient;
import org.overlord.sramp.client.SrampClientException;
import org.overlord.sramp.client.query.ArtifactSummary;
import org.overlord.sramp.client.query.QueryResultSet;
import org.overlord.sramp.common.ArtifactType;
import org.overlord.sramp.common.SrampModelUtils;
import org.overlord.sramp.wagon.models.MavenGavInfo;
import org.overlord.sramp.wagon.util.DevNullOutputStream;

/**
 * Implements a wagon provider that uses the S-RAMP Atom API.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("unchecked")
@Component(role = Wagon.class, hint = "sramp", instantiationStrategy = "per-lookup")
public class SrampWagon extends StreamWagon {

	@Requirement
	private Logger logger;

	private transient SrampArchive archive;
	private transient SrampAtomApiClient client;

	/**
	 * Constructor.
	 */
	public SrampWagon() {
	}

	/**
	 * @return the endpoint to use for the s-ramp repo
	 */
	private String getSrampEndpoint() {
		String pomUrl = getRepository().getUrl();
		if (pomUrl.indexOf('?') > 0) {
		    pomUrl = pomUrl.substring(0, pomUrl.indexOf('?'));
		}
        String replace = pomUrl.replace("sramp:", "http:").replace("sramps:", "https:");
        return replace;
	}

	/**
	 * @see org.apache.maven.wagon.AbstractWagon#openConnectionInternal()
	 */
	@Override
	protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
		// Even though the S-RAMP Atom API is session-less, use this open method
		// to start building up an S-RAMP archive containing the artifacts we are
		// storing in the repository (along with the meta-data for those artifacts).
		// The archive will serve as a temporary place to stash information we may
		// need later.
        ClassLoader oldCtxCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SrampWagon.class.getClassLoader());
		try {
		    // Create the archive
			this.archive = new SrampArchive();

			// Now create and configure the client.
            String endpoint = getSrampEndpoint();
            // Use sensible defaults
            String username = "admin";
            String password = "overlord";
            AuthenticationInfo authInfo = this.getAuthenticationInfo();
            if (authInfo != null) {
                if (authInfo.getUserName() != null) {
                    username = authInfo.getUserName();
                }
                if (authInfo.getPassword() != null) {
                    password = authInfo.getPassword();
                }
            }

            this.client = new SrampAtomApiClient(endpoint, username, password, true);
		} catch (SrampArchiveException e) {
			throw new ConnectionException("Failed to create the s-ramp archive (temporary storage).", e);
		} catch (SrampClientException e) {
            throw new ConnectionException("Failed to connect to the S-RAMP repository.", e);
        } catch (SrampAtomException e) {
            throw new ConnectionException("Failed to connect to the S-RAMP repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCtxCL);
        }
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#closeConnection()
	 */
	@Override
	public void closeConnection() throws ConnectionException {
		SrampArchive.closeQuietly(archive);
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#fillInputData(org.apache.maven.wagon.InputData)
	 */
	@Override
	public void fillInputData(InputData inputData) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		Resource resource = inputData.getResource();
		// Skip maven-metadata.xml files - they are not (yet?) supported
		if (resource.getName().contains("maven-metadata.xml"))
			throw new ResourceDoesNotExistException("Could not find file: '" + resource + "'");

		logger.debug("Looking up resource from s-ramp repository: " + resource);

		MavenGavInfo gavInfo = MavenGavInfo.fromResource(resource);
		if (gavInfo.isHash()) {
			doGetHash(gavInfo, inputData);
		} else {
			doGetArtifact(gavInfo, inputData);
		}

	}

	/**
	 * Gets the hash data from the s-ramp repository and stores it in the {@link InputData} for
	 * use by Maven.
	 * @param gavInfo
	 * @param inputData
	 * @throws TransferFailedException
	 * @throws ResourceDoesNotExistException
	 * @throws AuthorizationException
	 */
	private void doGetHash(MavenGavInfo gavInfo, InputData inputData) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		String artyPath = gavInfo.getFullName();
		String hashPropName;
		if (gavInfo.getType().endsWith(".md5")) {
			hashPropName = "maven.hash.md5";
			artyPath = artyPath.substring(0, artyPath.length() - 4);
		} else {
			hashPropName = "maven.hash.sha1";
			artyPath = artyPath.substring(0, artyPath.length() - 5);
		}
        // See the comment in {@link SrampWagon#fillInputData(InputData)} about why we're doing this
        // context classloader magic.
        ClassLoader oldCtxCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SrampWagon.class.getClassLoader());
        try {
    		SrampArchiveEntry entry = this.archive.getEntry(artyPath);
    		if (entry == null) {
    			throw new ResourceDoesNotExistException("Failed to find resource hash: " + gavInfo.getName());
    		}
    		BaseArtifactType metaData = entry.getMetaData();

    		String hashValue = SrampModelUtils.getCustomProperty(metaData, hashPropName);
    		if (hashValue == null) {
    			throw new ResourceDoesNotExistException("Failed to find resource hash: " + gavInfo.getName());
    		}
    		inputData.setInputStream(IOUtils.toInputStream(hashValue));
        } finally {
            Thread.currentThread().setContextClassLoader(oldCtxCL);
        }
	}

	/***
	 * Gets the artifact content from the s-ramp repository and stores it in the {@link InputData}
	 * object for use by Maven.
	 * @param gavInfo
	 * @param inputData
	 * @throws TransferFailedException
	 * @throws ResourceDoesNotExistException
	 * @throws AuthorizationException
	 */
	private void doGetArtifact(MavenGavInfo gavInfo, InputData inputData) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		// RESTEasy uses the current thread's context classloader to load its logger class.  This
		// fails in Maven because the context classloader is the wagon plugin's classloader, which
		// doesn't know about any of the RESTEasy JARs.  So here we're temporarily setting the
		// context classloader to the s-ramp wagon extension's classloader, which should have access
		// to all the right stuff.
		ClassLoader oldCtxCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(SrampWagon.class.getClassLoader());
		try {
			// Query the artifact meta data using GAV info
			BaseArtifactType artifact = findExistingArtifact(client, gavInfo);
			if (artifact == null)
				throw new ResourceDoesNotExistException("Artifact not found in s-ramp repository: '" + gavInfo.getName() + "'");
			this.archive.addEntry(gavInfo.getFullName(), artifact, null);
			ArtifactType type = ArtifactType.valueOf(artifact);

			// Get the artifact content as an input stream
			InputStream artifactContent = client.getArtifactContent(type, artifact.getUuid());
			inputData.setInputStream(artifactContent);
		} catch (ResourceDoesNotExistException e) {
			throw e;
		} catch (SrampClientException e) {
			if (e.getCause() instanceof HttpHostConnectException) {
				this.logger.debug("Could not connect to s-ramp repository: " + e.getMessage());
			} else {
				this.logger.error(e.getMessage(), e);
			}
			throw new ResourceDoesNotExistException("Failed to get resource from s-ramp: " + gavInfo.getName());
		} catch (Throwable t) {
			this.logger.error(t.getMessage(), t);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxCL);
		}
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#putFromStream(java.io.InputStream, java.lang.String)
	 */
	@Override
	public void putFromStream(InputStream stream, String destination) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		Resource resource = new Resource(destination);
		putCommon(resource, null, stream);
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#putFromStream(java.io.InputStream, java.lang.String, long, long)
	 */
	@Override
	public void putFromStream(InputStream stream, String destination, long contentLength, long lastModified)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		Resource resource = new Resource(destination);
		resource.setContentLength(contentLength);
		resource.setLastModified(lastModified);
		putCommon(resource, null, stream);
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#put(java.io.File, java.lang.String)
	 */
	@Override
	public void put(File source, String resourceName) throws TransferFailedException,
			ResourceDoesNotExistException, AuthorizationException {
		InputStream resourceInputStream = null;
		try {
			resourceInputStream = new FileInputStream(source);
		} catch (FileNotFoundException e) {
			throw new TransferFailedException(e.getMessage());
		}

		Resource resource = new Resource(resourceName);
		resource.setContentLength(source.length());
		resource.setLastModified(source.lastModified());
		putCommon(resource, source, resourceInputStream);
	}

	/**
	 * Common put implementation.  Handles firing events and ultimately sending the data via the
	 * s-ramp client.
	 * @param resource
	 * @param source
	 * @param content
	 * @throws TransferFailedException
	 * @throws ResourceDoesNotExistException
	 * @throws AuthorizationException
	 */
	private void putCommon(Resource resource, File source, InputStream content)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		logger.info("Uploading s-ramp artifact: " + resource.getName());
		firePutInitiated(resource, source);

		firePutStarted(resource, source);
		if (resource.getName().contains("maven-metadata.xml")) {
			logger.info("Skipping unsupported artifact: " + resource.getName());
			try {
				transfer(resource, content, new DevNullOutputStream(), TransferEvent.REQUEST_PUT);
			} catch (IOException e) {
				throw new TransferFailedException(e.getMessage(), e);
			}
		} else {
			doPut(resource, content);
		}
		firePutCompleted(resource, source);
	}

	/**
	 * Gets the artifact type from the resource.
	 * @param gavInfo
	 */
	private ArtifactType getArtifactType(MavenGavInfo gavInfo) {
        String customAT = getParamFromRepositoryUrl("artifactType");
	    if (gavInfo.getType().equals("pom")) {
	        return ArtifactType.valueOf("MavenPom");
	    } else if (isPrimaryArtifact(gavInfo) && customAT != null) {
	        return ArtifactType.valueOf(customAT);
	    }
		String fileName = gavInfo.getName();
		int extensionIdx = fileName.lastIndexOf('.');
		String extension = gavInfo.getName().substring(extensionIdx + 1);
		return ArtifactType.fromFileExtension(extension);
	}

	/**
	 * Puts the maven resource into the s-ramp repository.
	 * @param resource
	 * @param resourceInputStream
	 * @throws TransferFailedException
	 */
	private void doPut(Resource resource, InputStream resourceInputStream) throws TransferFailedException {
		MavenGavInfo gavInfo = MavenGavInfo.fromResource(resource);
		if (gavInfo.isHash()) {
			doPutHash(gavInfo, resourceInputStream);
		} else {
			doPutArtifact(gavInfo, resourceInputStream);
		}
	}

	/**
	 * Updates an artifact by storing its hash value as an S-RAMP property.
	 * @param gavInfo
	 * @param resourceInputStream
	 * @throws TransferFailedException
	 */
	private void doPutHash(MavenGavInfo gavInfo, InputStream resourceInputStream) throws TransferFailedException {
		logger.info("Storing hash value as s-ramp property: " + gavInfo.getName());
		try {
			String artyPath = gavInfo.getFullName();
			String hashPropName;
			if (gavInfo.getType().endsWith(".md5")) {
				hashPropName = "maven.hash.md5";
				artyPath = artyPath.substring(0, artyPath.length() - 4);
			} else {
				hashPropName = "maven.hash.sha1";
				artyPath = artyPath.substring(0, artyPath.length() - 5);
			}
			String hashValue = IOUtils.toString(resourceInputStream);

            // See the comment in {@link SrampWagon#fillInputData(InputData)} about why we're doing this
            // context classloader magic.
            ClassLoader oldCtxCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(SrampWagon.class.getClassLoader());
            try {
    			SrampArchiveEntry entry = this.archive.getEntry(artyPath);
    			BaseArtifactType metaData = entry.getMetaData();
    			SrampModelUtils.setCustomProperty(metaData, hashPropName, hashValue);
    			this.archive.updateEntry(entry, null);

    			// The meta-data has been updated in the local/temp archive - now send it to the remote repo
				client.updateArtifactMetaData(metaData);
			} catch (Throwable t) {
				throw new TransferFailedException(t.getMessage(), t);
			} finally {
				Thread.currentThread().setContextClassLoader(oldCtxCL);
			}
		} catch (Exception e) {
			throw new TransferFailedException("Failed to store a hash: " + gavInfo.getName(), e);
		}
	}

	/**
	 * Puts the artifact into the s-ramp repository.
	 * @param gavInfo
	 * @param resourceInputStream
	 * @throws TransferFailedException
	 */
	private void doPutArtifact(final MavenGavInfo gavInfo, InputStream resourceInputStream) throws TransferFailedException {
		ArtifactType artifactType = getArtifactType(gavInfo);
		// See the comment in {@link SrampWagon#fillInputData(InputData)} about why we're doing this
		// context classloader magic.
		ClassLoader oldCtxCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(SrampWagon.class.getClassLoader());
		File tempResourceFile = null;
        ZipToSrampArchive expander = null;
		SrampArchive archive = null;
		BaseArtifactType artifactGrouping = null;
		try {
			// First, stash the content in a temp file - we may need it multiple times.
			tempResourceFile = stashResourceContent(resourceInputStream);
			resourceInputStream = FileUtils.openInputStream(tempResourceFile);

			// Is the artifact grouping option enabled?
			if (isPrimaryArtifact(gavInfo) && getParamFromRepositoryUrl("artifactGrouping") != null) {
			    artifactGrouping = ensureArtifactGrouping();
			}

			// Only search for existing artifacts by GAV info here
			BaseArtifactType artifact = findExistingArtifactByGAV(client, gavInfo);
			// If we found an artifact, we should update its content.  If not, we should upload
			// the artifact to the repository.
			if (artifact != null) {
				this.archive.addEntry(gavInfo.getFullName(), artifact, null);
				client.updateArtifactContent(artifact, resourceInputStream);
				if (ZipToSrampArchiveRegistry.canExpand(artifactType)) {
					final String parentUUID = artifact.getUuid();
					cleanExpandedArtifacts(client, parentUUID);
				}
			} else {
				// Upload the content, then add the maven properties to the artifact
				// as meta-data
				artifact = client.uploadArtifact(artifactType, resourceInputStream, gavInfo.getName());
				SrampModelUtils.setCustomProperty(artifact, "maven.groupId", gavInfo.getGroupId());
				SrampModelUtils.setCustomProperty(artifact, "maven.artifactId", gavInfo.getArtifactId());
				SrampModelUtils.setCustomProperty(artifact, "maven.version", gavInfo.getVersion());
				artifact.setVersion(gavInfo.getVersion());
				if (gavInfo.getClassifier() != null)
					SrampModelUtils.setCustomProperty(artifact, "maven.classifier", gavInfo.getClassifier());
				SrampModelUtils.setCustomProperty(artifact, "maven.type", gavInfo.getType());
				// Also create a relationship to the artifact grouping, if necessary
				if (artifactGrouping != null) {
				    SrampModelUtils.addGenericRelationship(artifact, "groupedBy", artifactGrouping.getUuid());
				    SrampModelUtils.addGenericRelationship(artifactGrouping, "groups", artifact.getUuid());
				    client.updateArtifactMetaData(artifactGrouping);
				}

				client.updateArtifactMetaData(artifact);
				this.archive.addEntry(gavInfo.getFullName(), artifact, null);
			}

			// Now also add "expanded" content to the s-ramp repository
            expander = ZipToSrampArchiveRegistry.createExpander(artifactType, tempResourceFile);
            if (expander != null) {
                expander.setContextParam(DefaultMetaDataFactory.PARENT_UUID, artifact.getUuid());
                expander.addMetaDataProvider(new MetaDataProvider() {
                    @Override
                    public void provideMetaData(BaseArtifactType artifact) {
                        SrampModelUtils.setCustomProperty(artifact, "maven.parent-groupId", gavInfo.getGroupId());
                        SrampModelUtils.setCustomProperty(artifact, "maven.parent-artifactId", gavInfo.getArtifactId());
                        SrampModelUtils.setCustomProperty(artifact, "maven.parent-version", gavInfo.getVersion());
                        SrampModelUtils.setCustomProperty(artifact, "maven.parent-type", gavInfo.getType());
                    }
                });
                archive = expander.createSrampArchive();
                client.uploadBatch(archive);
            }
		} catch (Throwable t) {
			throw new TransferFailedException(t.getMessage(), t);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxCL);
			SrampArchive.closeQuietly(archive);
            ZipToSrampArchive.closeQuietly(expander);
			FileUtils.deleteQuietly(tempResourceFile);
		}
	}

    /**
     * Ensures that the required ArtifactGrouping is present in the repository.
	 * @throws SrampAtomException
	 * @throws SrampClientException
     */
    private BaseArtifactType ensureArtifactGrouping() throws SrampClientException, SrampAtomException {
        String groupingName = getParamFromRepositoryUrl("artifactGrouping");
        if (groupingName == null || groupingName.trim().length() == 0) {
            logger.warn("No Artifact Grouping name configured.");
            return null;
        }
        QueryResultSet query = client.buildQuery("/s-ramp/ext/ArtifactGrouping[@name = ?]").parameter(groupingName).count(2).query();
        if (query.size() > 1) {
            logger.warn("Multiple Artifact Groupings found with the same name: " + groupingName);
            return null;
        } else if (query.size() == 1) {
            ArtifactSummary summary = query.get(0);
            return client.getArtifactMetaData(summary.getType(), summary.getUuid());
        } else {
            ExtendedArtifactType groupingArtifact = new ExtendedArtifactType();
            groupingArtifact.setArtifactType(BaseArtifactEnum.EXTENDED_ARTIFACT_TYPE);
            groupingArtifact.setExtendedType("ArtifactGrouping");
            groupingArtifact.setName(groupingName);
            groupingArtifact.setDescription("An Artifact Grouping automatically created by the S-RAMP Maven Wagon (integration between S-RAMP and Maven).");
            return client.createArtifact(groupingArtifact);
        }
    }

    /**
	 * Deletes the 'expanded' artifacts from the s-ramp repository.
	 * @param client
	 * @param parentUUID
	 * @throws SrampClientException
	 * @throws SrampAtomException
	 */
	private void cleanExpandedArtifacts(SrampAtomApiClient client, String parentUUID) throws SrampAtomException, SrampClientException {
		String query = String.format("/s-ramp[mavenParent[@uuid = '%1$s']]", parentUUID);
		boolean done = false;
		while (!done) {
			QueryResultSet rset = client.query(query, 0, 20, "name", true);
			if (rset.size() == 0) {
				done = true;
			} else {
				for (ArtifactSummary entry : rset) {
					ArtifactType artifactType = entry.getType();
					String uuid = entry.getUuid();
					client.deleteArtifact(uuid, artifactType);
				}
			}
		}
	}

	/**
	 * Make a temporary copy of the resource by saving the content to a temp file.
	 * @param resourceInputStream
	 * @throws IOException
	 */
	private File stashResourceContent(InputStream resourceInputStream) throws IOException {
		File resourceTempFile = null;
		OutputStream oStream = null;
		try {
			resourceTempFile = File.createTempFile("s-ramp-wagon-resource", ".tmp");
			oStream = FileUtils.openOutputStream(resourceTempFile);
		} finally {
			IOUtils.copy(resourceInputStream, oStream);
			IOUtils.closeQuietly(resourceInputStream);
			IOUtils.closeQuietly(oStream);
		}
		return resourceTempFile;
	}

	/**
	 * Finds an existing artifact in the s-ramp repository that matches the type and GAV information.
	 * @param client
	 * @param artifactType
	 * @param gavInfo
	 * @return an s-ramp artifact (if found) or null (if not found)
	 * @throws SrampClientException
	 * @throws SrampAtomException
	 * @throws JAXBException
	 */
	private BaseArtifactType findExistingArtifact(SrampAtomApiClient client, MavenGavInfo gavInfo)
			throws SrampAtomException, SrampClientException, JAXBException {
		BaseArtifactType artifact = findExistingArtifactByGAV(client, gavInfo);
		if (artifact == null)
			artifact = findExistingArtifactByUniversal(client, gavInfo);
		return artifact;
	}

	/**
	 * Finds an existing artifact in the s-ramp repository that matches the GAV information.
	 * @param client
	 * @param gavInfo
	 * @return an s-ramp artifact (if found) or null (if not found)
	 * @throws SrampClientException
	 * @throws SrampAtomException
	 * @throws JAXBException
	 */
	private BaseArtifactType findExistingArtifactByGAV(SrampAtomApiClient client, MavenGavInfo gavInfo)
			throws SrampAtomException, SrampClientException, JAXBException {
		String query = null;
		// Search by classifier if we have one...
		if (gavInfo.getClassifier() == null) {
			query = String.format("/s-ramp[@maven.groupId = '%1$s' and @maven.artifactId = '%2$s' and @maven.version = '%3$s' and @maven.type = '%4$s']",
					gavInfo.getGroupId(), gavInfo.getArtifactId(), gavInfo.getVersion(), gavInfo.getType());
		} else {
			query = String.format("/s-ramp[@maven.groupId = '%1$s' and @maven.artifactId = '%2$s' and @maven.version = '%3$s' and @maven.classifier = '%4$s' and @maven.type = '%5$s']",
					gavInfo.getGroupId(), gavInfo.getArtifactId(), gavInfo.getVersion(), gavInfo.getClassifier(), gavInfo.getType());
		}
		QueryResultSet rset = client.query(query);
		if (rset.size() > 0) {
			for (ArtifactSummary summary : rset) {
				String uuid = summary.getUuid();
				ArtifactType artifactType = summary.getType();
				BaseArtifactType arty = client.getArtifactMetaData(artifactType, uuid);
				// If no classifier in the GAV info, only return the artifact that also has no classifier
				if (gavInfo.getClassifier() == null) {
					String artyClassifier = SrampModelUtils.getCustomProperty(arty, "maven.classifier");
					if (artyClassifier == null) {
						return arty;
					}
				} else {
					// If classifier was supplied in the GAV info, we'll get the first artifact <shrug>
					return arty;
				}
			}
		}
		return null;
	}

	/**
	 * Finds an existing artifact in the s-ramp repository using 'universal' form.  This allows
	 * any artifact in the s-ramp repository to be referenced as a Maven dependency using the
	 * model.type and UUID of the artifact.
	 * @param client
	 * @param artifactType
	 * @param gavInfo
	 * @return an existing s-ramp artifact (if found) or null (if not found)
	 * @throws SrampClientException
	 * @throws SrampAtomException
	 * @throws JAXBException
	 */
	private BaseArtifactType findExistingArtifactByUniversal(SrampAtomApiClient client, MavenGavInfo gavInfo)
			throws SrampAtomException, SrampClientException, JAXBException {
		String artifactType = gavInfo.getGroupId().substring(gavInfo.getGroupId().indexOf('.') + 1);
		String uuid = gavInfo.getArtifactId();
		try {
			return client.getArtifactMetaData(ArtifactType.valueOf(artifactType), uuid);
		} catch (Throwable t) {
			logger.debug(t.getMessage());
		}
		return null;
	}

	/**
	 * @see org.apache.maven.wagon.StreamWagon#fillOutputData(org.apache.maven.wagon.OutputData)
	 */
	@Override
	public void fillOutputData(OutputData outputData) throws TransferFailedException {
		// Since the wagon is implementing the put method directly, the StreamWagon's
		// implementation is never called.
		throw new RuntimeException("Should never get here!");
	}

	/**
	 * Gets a URL parameter by name from the repository URL.
	 * @param paramName
	 */
	protected String getParamFromRepositoryUrl(String paramName) {
	    String url = getRepository().getUrl();
	    int idx = url.indexOf('?');
	    if (idx == -1)
	        return null;
        String query = url.substring(idx + 1);
	    String [] params = query.split("&");
	    for (String paramPair : params) {
	        String [] pp = paramPair.split("=");
	        if (pp.length == 2) {
    	        String key = pp[0];
    	        String val = pp[1];
    	        if (key.equals(paramName)) {
    	            return val;
    	        }
	        } else {
	            throw new RuntimeException("Invalid query parameter in repository URL (param name without value).");
	        }
	    }
	    return null;
	}

    /**
     * Returns true if this represents the primary artifact in the Maven module.
     * @param gavInfo
     */
	protected boolean isPrimaryArtifact(MavenGavInfo gavInfo) {
        return gavInfo.getClassifier() == null && !gavInfo.getType().equals("pom");
    }

}
