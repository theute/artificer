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
package org.overlord.sramp.governance.services;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.test.BaseResourceTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Tests the Deployment API.
 *
 * @author kurt.stam@redhat.com
 */
public class NotificationResourceTest extends BaseResourceTest {
	
	/**
	 * This is an integration test, and only works if artifact 'e67e1b09-1de7-4945-a47f-45646752437a'
     * exists in the repo; check the following urls to find out:
     * 
	 * http://localhost:8080/s-ramp-server/s-ramp?query=/s-ramp[@uuid%3D'e67e1b09-1de7-4945-a47f-45646752437a']
	 * http://localhost:8080/s-ramp-server/s-ramp/user/BpmnDocument/e67e1b09-1de7-4945-a47f-45646752437a
	 * 
	 * @throws Exception
	 */
	@Test
	public void testNotify() {
	    try {
	        String notificationUrl = "http://localhost:8080/s-ramp-governance/notify/email/dev/deployed/dev/${uuid}";
	        String uuid="3c7bb7f7-a811-4080-82db-5ece86993a11";
	        URL url = new URL(notificationUrl.replace("${uuid}", uuid));
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("POST");
	        connection.setConnectTimeout(10000);
	        connection.setReadTimeout(10000);
	        connection.connect();
	        int responseCode = connection.getResponseCode();
	        if (responseCode == 200) {
	             InputStream is = (InputStream) connection.getContent();
	             String reply = IOUtils.toString(is);
	             System.out.println("reply=" + reply);
	        } else {
	            System.err.println("endpoint could not be reached");
	            Assert.fail();
	        }
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        Assert.fail();
	    }
	    
	}
}
