/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;

public class OpenAPIUtils {
    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    /**
     * The createBaseOpenAPIDocument method creates a default OpenAPI model object.
     * 
     * @return OpenAPI
     *             The default OpenAPI model
     */
    @Trivial
    public static OpenAPI createBaseOpenAPIDocument() {
        /*
         * The default OpenAPI document needs to be identical to the one that would be generated by the SmallRye
         * implementation. 
         */
        OpenAPI openAPI = new OpenAPIImpl();
        openAPI.setOpenapi(OpenApiConstants.OPEN_API_VERSION);
        openAPI.paths(new PathsImpl());
        openAPI.info(new InfoImpl().title(Constants.DEFAULT_OPENAPI_DOC_TITLE).version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Created base OpenAPI document");
        }
        return openAPI;
    }

    /**
     * The containsServersDefinition method checks whether the specified OpenAPI model defines any servers.
     * 
     * @param openAPI
     *            The OpenAPI model
     * @return boolean
     *            True iff the OpenAPI model already defines servers
     */
    @Trivial
    public static boolean containsServersDefinition(final OpenAPI openAPIModel) {
        // Create the variable to return
        boolean containsServers = false;
        
        // Return true if the model contains at least one server definition
        if (openAPIModel != null && openAPIModel.getServers() != null && openAPIModel.getServers().size() > 0) {
            containsServers = true;
        }
        
        return containsServers;
    }
    
    /**
     * The addServersToOpenAPIModel method creates new server defintions based on the specified ServerInfo object and
     * adds them to the specified OpenAPI model.  The OpenAPI model is not modified if already contains at least one
     * server definition. 
     * 
     * @param openAPIModel
     *          The OpenAPI model to update
     * @param serverInfo
     *          The ServerInfo object to use when creating the new servers model
     */
    public static void addServersToOpenAPIModel(final OpenAPI openAPIModel, final ServerInfo serverInfo) {

        // Only update the model if it does not contain any server definitions
        if (!containsServersDefinition(openAPIModel)) {
            
            // Remove any servers from the model... should only be an empty servers object at this point
            openAPIModel.setServers(null);

            final int httpPort  = serverInfo.getHttpPort();
            final int httpsPort = serverInfo.getHttpsPort();
            final String host   = serverInfo.getHost();
            final String applicationPath = serverInfo.getApplicationPath();
            
            if (httpPort > 0) {
                String port = httpPort == 80 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpPort);
                String url = Constants.SCHEME_HTTP + host + port;
                if (applicationPath != null) {
                    url += applicationPath;
                }
                Server server = OASFactory.createServer();
                server.setUrl(url);
                openAPIModel.addServer(server);
            }
            
            if (httpsPort > 0) {
                String port = httpsPort == 443 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpsPort);
                String secureUrl = Constants.SCHEME_HTTPS + host + port;
                if (applicationPath != null) {
                    secureUrl += applicationPath;
                }
                Server secureServer = OASFactory.createServer();
                secureServer.setUrl(secureUrl);
                openAPIModel.addServer(secureServer);
            }
        }
    }
    
    /**
     * The isDefaultOpenApiModel method checks whether the OpenAPI model specified is a default OpenAPI model generated
     * by the SmallRye implementation. 
     * 
     * @param model
     *            The OpenAPI model to check
     * @return boolean
     *            True if the OpenAPI model is a default model, false otherwise.
     */
    public static boolean isDefaultOpenApiModel(OpenAPI model) {
        
        // Create the variable to return
        boolean isDefault = false;

        /*
         * The SmallRye implementation generates an OpenAPI model regardless of whether the application contains any
         * OAS or JAX-RS annotations. The default model that is generated is of the form:
         * 
         *     openapi: 3.0.1
         *     info:
         *       title: Generated API
         *       version: "1.0"
         *     servers:
         *     - url: http://localhost:8010
         *     - url: https://localhost:8020
         *     paths: {}
         *     
         * This makes detecting whether the application is an OAS application a little more problematic.  We need to
         * introspect the generated OpenAPI model object to determine whether it is a real model instance or just a
         * default.
         */
        if (  model.getOpenapi().equals(OpenApiConstants.OPEN_API_VERSION) 
           && model.getInfo() != null
           && model.getInfo().getContact() == null
           && model.getInfo().getDescription() == null
           && model.getInfo().getLicense() == null
           && model.getInfo().getTermsOfService() == null
           && model.getInfo().getTitle().equals(Constants.DEFAULT_OPENAPI_DOC_TITLE)
           && model.getInfo().getVersion().equals(Constants.DEFAULT_OPENAPI_DOC_VERSION)
           && model.getPaths() != null
           && model.getPaths().getPathItems() == null
           && model.getComponents() == null
           && model.getExtensions() == null
           && model.getExternalDocs() == null
           && model.getSecurity() == null
           && model.getServers() == null
           && model.getTags() == null
           ) {
            isDefault = true;
        }

        return isDefault;
    }
    
    private OpenAPIUtils() {
        // This class is not meant to be instantiated.
    }
}
