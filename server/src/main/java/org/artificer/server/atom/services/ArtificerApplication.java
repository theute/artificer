/*
 * Copyright 2011 JBoss Inc
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
package org.artificer.server.atom.services;

import org.artificer.atom.providers.ArtificerConflictExceptionProvider;
import org.artificer.atom.providers.ArtificerNotFoundExceptionProvider;
import org.artificer.atom.providers.ArtificerServerExceptionProvider;
import org.artificer.atom.providers.ArtificerWrongModelExceptionProvider;
import org.artificer.atom.providers.AuditEntryProvider;
import org.artificer.atom.providers.HttpResponseProvider;
import org.artificer.atom.providers.OntologyProvider;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * The SRAMP RESTEasy application.  This is essentially the main entry point into a
 * RESTEasy application - it provides the resource implementation as well as any other
 * providers (mappers, etc).
 */
public class ArtificerApplication extends Application {

	private Set<Object> singletons = new HashSet<Object>();
	private Set<Class<?>> classes = new HashSet<Class<?>>();

	/**
	 * Constructor.
	 */
	public ArtificerApplication() {
		singletons.add(new ServiceDocumentResource());
		singletons.add(new ArtifactResource());
		singletons.add(new FeedResource());
		singletons.add(new QueryResource());
		singletons.add(new BatchResource());
        singletons.add(new OntologyResource());
        singletons.add(new AuditResource());
        singletons.add(new StoredQueryResource());
        singletons.add(new ArtificerResource());

		classes.add(ArtificerServerExceptionProvider.class);
        classes.add(ArtificerWrongModelExceptionProvider.class);
        classes.add(ArtificerConflictExceptionProvider.class);
        classes.add(ArtificerNotFoundExceptionProvider.class);
        
		classes.add(HttpResponseProvider.class);
        classes.add(OntologyProvider.class);
        classes.add(AuditEntryProvider.class);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
