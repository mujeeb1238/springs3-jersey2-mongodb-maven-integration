/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.examples.server.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import jersey.repackaged.com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.glassfish.jersey.examples.server.async.data.Customer;
import org.glassfish.jersey.examples.server.async.persistence.CustomerDao;
import org.glassfish.jersey.examples.server.async.service.CustomerService;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

/**
 * Example of a simple resource with a long-running operation executed in a
 * custom application thread.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path(App.ASYNC_LONG_RUNNING_OP_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimpleLongRunningResource {

    public static final String NOTIFICATION_RESPONSE = "Hello async world!";
    private static final Logger LOGGER = Logger.getLogger(SimpleLongRunningResource.class.getName());
    private static final int SLEEP_TIME_IN_MILLIS = 5000;
    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("long-running-resource-executor-%d")
            .setUncaughtExceptionHandler(new JerseyProcessingUncaughtExceptionHandler())
            .build());
    
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private CustomerDao customerDao;
    
    
    @POST
	@Path("save")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public Response saveCustomer(String c) {
    	Gson g = new Gson();
    	Customer customer = g.fromJson(c, Customer.class);
    	
    	customerService.save(customer);
    	
		return Response.status(Status.OK).entity(g.toJson(customer).toString()).type(MediaType.APPLICATION_JSON).build();
	}
    
    @GET
	@Path("save/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
	public void getCustomer(@PathParam("id") final Long id, @Suspended final AsyncResponse ar) {
    	
    	
    	final Gson g = new Gson();
    	
    	
    	TASK_EXECUTOR.submit(new Runnable() {
    		
    		Customer customer = null;
             
             public void run() {
                 customer = customerService.retrieve(id);
				//Thread.sleep(2000);
                 ar.resume(Response.status(Status.OK).entity(g.toJson(customer).toString()).type(MediaType.APPLICATION_JSON).build());
             }
         });
    	
    	
    	System.out.println("out of method");
	}
    

    @GET
    public void longGet(@Suspended final AsyncResponse ar) {
    	
        TASK_EXECUTOR.submit(new Runnable() {

            
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME_IN_MILLIS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.SEVERE, "Response processing interrupted", ex);
                }
                ar.resume(NOTIFICATION_RESPONSE);
            }
        });
    }
}
