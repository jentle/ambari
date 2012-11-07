/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ActionRequest;
import org.apache.ambari.server.controller.ActionResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RequestStatusRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.TaskStatusRequest;
import org.apache.ambari.server.controller.TaskStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic resource provider implementation that maps to a management controller.
 */
public abstract class ResourceProviderImpl implements ResourceProvider {

  /**
   * The set of property ids supported by this resource provider.
   */
  private final Set<PropertyId> propertyIds;

  /**
   * The management controller to delegate to.
   */
  private final AmbariManagementController managementController;

  /**
   * Key property mapping by resource type.
   */
  private final Map<Resource.Type, PropertyId> keyPropertyIds;


  // ----- Property ID constants ---------------------------------------------

  // Clusters
  protected static final PropertyId CLUSTER_ID_PROPERTY_ID      = PropertyHelper.getPropertyId("cluster_id", "Clusters");
  protected static final PropertyId CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("cluster_name", "Clusters");
  protected static final PropertyId CLUSTER_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("version", "Clusters");
  protected static final PropertyId CLUSTER_HOSTS_PROPERTY_ID   = PropertyHelper.getPropertyId("hosts", "Clusters");
  // Services
  protected static final PropertyId SERVICE_CLUSTER_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("cluster_name", "ServiceInfo");
  protected static final PropertyId SERVICE_SERVICE_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("service_name", "ServiceInfo");
  protected static final PropertyId SERVICE_SERVICE_STATE_PROPERTY_ID = PropertyHelper.getPropertyId("state", "ServiceInfo");

  // Components
  protected static final PropertyId COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("cluster_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("service_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "ServiceComponentInfo");
  protected static final PropertyId COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("state", "ServiceComponentInfo");
  // Hosts
  protected static final PropertyId HOST_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name", "Hosts");
  protected static final PropertyId HOST_NAME_PROPERTY_ID         = PropertyHelper.getPropertyId("host_name", "Hosts");
  protected static final PropertyId HOST_IP_PROPERTY_ID           = PropertyHelper.getPropertyId("ip", "Hosts");
  protected static final PropertyId HOST_TOTAL_MEM_PROPERTY_ID    = PropertyHelper.getPropertyId("total_mem", "Hosts");
  protected static final PropertyId HOST_CPU_COUNT_PROPERTY_ID    = PropertyHelper.getPropertyId("cpu_count", "Hosts");
  protected static final PropertyId HOST_OS_ARCH_PROPERTY_ID      = PropertyHelper.getPropertyId("os_arch", "Hosts");
  protected static final PropertyId HOST_OS_TYPE_PROPERTY_ID      = PropertyHelper.getPropertyId("os_type", "Hosts");
  // Host Components
  protected static final PropertyId HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("cluster_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("service_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("component_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("host_name", "HostRoles");
  protected static final PropertyId HOST_COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("state", "HostRoles");
  // Configurations (values are part of query strings and body post, so they don't have defined categories)
  protected static final PropertyId CONFIGURATION_CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("cluster_name","Config");
  protected static final PropertyId CONFIGURATION_CONFIG_TYPE_PROPERTY_ID     = PropertyHelper.getPropertyId("type");
  protected static final PropertyId CONFIGURATION_CONFIG_TAG_PROPERTY_ID      = PropertyHelper.getPropertyId("tag");
  // Actions
  protected static final PropertyId ACTION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name", "Actions");
  protected static final PropertyId ACTION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("service_name", "Actions");
  protected static final PropertyId ACTION_ACTION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("action_name", "Actions");
  // Requests
  protected static final PropertyId REQUEST_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name","Requests");
  protected static final PropertyId REQUEST_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("id","Requests");
  // Tasks
  protected static final PropertyId TASK_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name","Tasks");
  protected static final PropertyId TASK_REQUEST_ID_PROPERTY_ID   = PropertyHelper.getPropertyId("request_id","Tasks");
  protected static final PropertyId TASK_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("id","Tasks");
  protected static final PropertyId TASK_STAGE_ID_PROPERTY_ID     = PropertyHelper.getPropertyId("stage_id","Tasks");
  protected static final PropertyId TASK_HOST_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("host_name","Tasks");
  protected static final PropertyId TASK_ROLE_PROPERTY_ID         = PropertyHelper.getPropertyId("role","Tasks");
  protected static final PropertyId TASK_COMMAND_PROPERTY_ID      = PropertyHelper.getPropertyId("command","Tasks");
  protected static final PropertyId TASK_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("status","Tasks");
  protected static final PropertyId TASK_EXIT_CODE_PROPERTY_ID    = PropertyHelper.getPropertyId("exit_code","Tasks");
  protected static final PropertyId TASK_STDERR_PROPERTY_ID       = PropertyHelper.getPropertyId("stderr","Tasks");
  protected static final PropertyId TASK_STOUT_PROPERTY_ID        = PropertyHelper.getPropertyId("stdout","Tasks");
  protected static final PropertyId TASK_START_TIME_PROPERTY_ID   = PropertyHelper.getPropertyId("start_time","Tasks");
  protected static final PropertyId TASK_ATTEMPT_CNT_PROPERTY_ID  = PropertyHelper.getPropertyId("attempt_cnt","Tasks");

  private final static Logger LOG =
      LoggerFactory.getLogger(ResourceProviderImpl.class);

    // ----- Constructors ------------------------------------------------------
  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  private ResourceProviderImpl(Set<PropertyId> propertyIds,
                               Map<Resource.Type, PropertyId> keyPropertyIds,
                               AmbariManagementController managementController) {
    this.propertyIds          = propertyIds;
    this.keyPropertyIds       = keyPropertyIds;
    this.managementController = managementController;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Map<Resource.Type, PropertyId> getKeyPropertyIds() {
    return keyPropertyIds;
  }

  // ----- accessors ---------------------------------------------------------

  /**
   * Get the associated management controller.
   *
   * @return the associated management controller
   */
  public AmbariManagementController getManagementController() {
    return managementController;
  }

  // ----- utility methods ---------------------------------------------------

  protected abstract Set<PropertyId> getPKPropertyIds();

  protected Set<Map<PropertyId, Object>> getPropertyMaps(Map<PropertyId, Object> requestPropertyMap,
                                                         Predicate predicate)
      throws AmbariException{

    Set<PropertyId>              pkPropertyIds       = getPKPropertyIds();
    Set<Map<PropertyId, Object>> properties          = new HashSet<Map<PropertyId, Object>>();
    Set<Map<PropertyId, Object>> predicateProperties = new HashSet<Map<PropertyId, Object>>();

    if (predicate != null && pkPropertyIds.equals(PredicateHelper.getPropertyIds(predicate))) {
      predicateProperties.add(getProperties(predicate));
    } else {
      for (Resource resource : getResources(PropertyHelper.getReadRequest(pkPropertyIds), predicate)) {
        predicateProperties.add(PropertyHelper.getProperties(resource));
      }
    }

    for (Map<PropertyId, Object> predicatePropertyMap : predicateProperties) {
      // get properties from the given request properties
      Map<PropertyId, Object> propertyMap = requestPropertyMap == null ?
          new HashMap<PropertyId, Object>():
          new HashMap<PropertyId, Object>(requestPropertyMap);
      // add the pk properties
      setProperties(propertyMap, predicatePropertyMap, pkPropertyIds);
      properties.add(propertyMap);
    }
    return properties;
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response) {

    if (response != null){
      Resource requestResource = new ResourceImpl(Resource.Type.Request);
      requestResource.setProperty(PropertyHelper.getPropertyId("id", "Requests"), response.getRequestId());
      // TODO : how do we tell what a request status is?
      // for now make everything InProgress
      requestResource.setProperty(PropertyHelper.getPropertyId("status", "Requests"), "InProgress");
      return new RequestStatusImpl(requestResource);
    }
    return new RequestStatusImpl(null);
  }

  /**
   * Get a map of property values from a given predicate.
   *
   * @param predicate  the predicate
   *
   * @return the map of properties
   */
  private static Map<PropertyId, Object> getProperties(Predicate predicate) {
    if (predicate == null) {
      return Collections.emptyMap();
    }
    PropertyPredicateVisitor visitor = new PropertyPredicateVisitor();
    PredicateHelper.visit(predicate, visitor);
    return visitor.getProperties();
  }

  /**
   * Transfer property values from one map to another for the given list of property ids.
   *
   * @param to           the target map
   * @param from         the source map
   * @param propertyIds  the set of property ids
   */
  private static void setProperties(Map<PropertyId, Object> to, Map<PropertyId, Object> from, Set<PropertyId> propertyIds) {
    for (PropertyId propertyId : propertyIds) {
      if (from.containsKey(propertyId)) {
        to.put(propertyId, from.get(propertyId));
      }
    }
  }

  /**
   * Set a string property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, String value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting property for resource"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
      resource.setProperty(propertyId, value);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
    }
  }

  /**
   * Set a long property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, Long value, Set<PropertyId> requestedIds) {
    // FIXME requestedIds does not seem to be populated properly for get
    // requests where a full response was requested
    if (requestedIds == null
        || requestedIds.isEmpty()
        || requestedIds.contains(propertyId)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting property for resource"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
      resource.setProperty(propertyId, value);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
    }
  }

  /**
   * Set a integer property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, Integer value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting property for resource"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
      resource.setProperty(propertyId, value);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
    }
  }

  /**
   * Set a short property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  private static void setResourceProperty(Resource resource, PropertyId propertyId, Short value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      resource.setProperty(propertyId, value);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
    }
  }

  /**
   * Factory method for obtaining a resource provider based on a given type and management controller.
   *
   *
   * @param type                  the resource type
   * @param propertyIds           the property ids
   * @param managementController  the management controller
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     Set<PropertyId> propertyIds,
                                                     Map<Resource.Type, PropertyId> keyPropertyIds,
                                                     AmbariManagementController managementController) {
    switch (type) {
      case Cluster:
        return new ClusterResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Service:
        return new ServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Component:
        return new ComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Host:
        return new HostResourceProvider(propertyIds, keyPropertyIds, managementController);
      case HostComponent:
        return new HostComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Configuration:
        return new ConfigurationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Action:
        return new ActionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Request:
        return new RequestResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Task:
        return new TaskResourceProvider(propertyIds, keyPropertyIds, managementController);
      default:
        throw new IllegalArgumentException("Unknown type " + type);
    }
  }


  // ------ ClusterResourceProvider inner class ------------------------------

  private static class ClusterResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            CLUSTER_ID_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private ClusterResourceProvider(Set<PropertyId> propertyIds,
                                    Map<Resource.Type, PropertyId> keyPropertyIds,
                                    AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

// ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {

      for (Map<PropertyId, Object> properties : request.getProperties()) {
        getManagementController().createCluster(getRequest(properties));
      }
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      ClusterRequest  clusterRequest = getRequest(getProperties(predicate));
      Set<PropertyId> requestedIds   = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);

      // TODO : handle multiple requests
      Set<ClusterResponse> responses = getManagementController().getClusters(Collections.singleton(clusterRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ClusterResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Cluster);
        setResourceProperty(resource, CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
        setResourceProperty(resource, CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        // FIXME requestedIds does not seem to be filled in properly for
        // non-partial responses
        resource.setProperty(CLUSTER_VERSION_PROPERTY_ID,
            response.getDesiredStackVersion());
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
        ClusterRequest clusterRequest = getRequest(propertyMap);
        getManagementController().updateCluster(clusterRequest);
      }
      return getRequestStatus(null);
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
        ClusterRequest clusterRequest = getRequest(propertyMap);
        getManagementController().deleteCluster(clusterRequest);
      }
      return getRequestStatus(null);
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a cluster request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the cluster request object
     */
    private ClusterRequest getRequest(Map<PropertyId, Object> properties) {

      Long id = (Long) properties.get(CLUSTER_ID_PROPERTY_ID);
      String stackVersion = (String) properties.get(CLUSTER_VERSION_PROPERTY_ID);

      return new ClusterRequest(
          id == null ? null : id,
          (String) properties.get(CLUSTER_NAME_PROPERTY_ID),
          stackVersion == null ? "HDP-0.1" : stackVersion,    // TODO : looks like version is required
          /*properties.get(CLUSTER_HOSTS_PROPERTY_ID)*/ null);
    }
  }

  // ------ ServiceResourceProvider inner class ------------------------------

  private static class ServiceResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            SERVICE_CLUSTER_NAME_PROPERTY_ID,
            SERVICE_SERVICE_NAME_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private ServiceResourceProvider(Set<PropertyId> propertyIds,
                                    Map<Resource.Type, PropertyId> keyPropertyIds,
                                    AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
      for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().createServices(requests);
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      ServiceRequest  serviceRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<ServiceResponse> responses = getManagementController().getServices(Collections.singleton(serviceRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Service);
//        setResourceProperty(resource, SERVICE_CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);

        resource.setProperty(SERVICE_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
//        resource.setProperty(SERVICE_SERVICE_NAME_PROPERTY_ID, response.getServiceName());


//        setResourceProperty(resource, SERVICE_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, SERVICE_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
//        setResourceProperty(resource, SERVICE_VERSION_PROPERTY_ID, response.getCurrentStackVersion(), requestedIds);
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {

        Map<String, String> configMappings = new HashMap<String, String>();

        for (PropertyId id : propertyMap.keySet()) {
          if (id.getCategory().equals ("config")) {
            configMappings.put(id.getName(), (String) propertyMap.get(id));
          }
        }

        ServiceRequest svcRequest = getRequest(propertyMap);
        if (configMappings.size() > 0)
          svcRequest.setConfigVersions(configMappings);

        requests.add(svcRequest);
      }
      return getRequestStatus(getManagementController().updateServices(requests));
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      Set<ServiceRequest> requests = new HashSet<ServiceRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
        requests.add(getRequest(propertyMap));
      }
      return getRequestStatus(getManagementController().deleteServices(requests));
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a service request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the service request object
     */
    private ServiceRequest getRequest(Map<PropertyId, Object> properties) {
      return new ServiceRequest(
          (String) properties.get(SERVICE_CLUSTER_NAME_PROPERTY_ID),
          (String) properties.get(SERVICE_SERVICE_NAME_PROPERTY_ID),
          null,
          (String) properties.get(SERVICE_SERVICE_STATE_PROPERTY_ID));
    }
  }

  // ------ ComponentResourceProvider inner class ----------------------------

  private static class ComponentResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            COMPONENT_CLUSTER_NAME_PROPERTY_ID,
            COMPONENT_SERVICE_NAME_PROPERTY_ID,
            COMPONENT_COMPONENT_NAME_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private ComponentResourceProvider(Set<PropertyId> propertyIds,
                                      Map<Resource.Type, PropertyId> keyPropertyIds,
                                      AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
      for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().createComponents(requests);
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      ServiceComponentRequest serviceComponentRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<ServiceComponentResponse> responses = getManagementController().getComponents(Collections.singleton(serviceComponentRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceComponentResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Component);
//        setResourceProperty(resource, COMPONENT_CLUSTER_ID_PROPERTY_ID, response.getClusterId(), requestedIds);
        setResourceProperty(resource, COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
        setResourceProperty(resource, COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
//        setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID, response.getCurrentStackVersion(), requestedIds);
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
        ServiceComponentRequest compRequest = getRequest(propertyMap);

        Map<String, String> configMap = new HashMap<String,String>();

        for (Entry<PropertyId,Object> entry : propertyMap.entrySet()) {
          if (entry.getKey().getCategory().equals("config")) {
            configMap.put(entry.getKey().getName(), (String) entry.getValue());
          }
        }

        if (0 != configMap.size())
          compRequest.setConfigVersions(configMap);

        requests.add(compRequest);
      }
      return getRequestStatus(getManagementController().updateComponents(requests));
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      Set<ServiceComponentRequest> requests = new HashSet<ServiceComponentRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
        requests.add(getRequest(propertyMap));
      }
      return getRequestStatus(getManagementController().deleteComponents(requests));
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private ServiceComponentRequest getRequest(Map<PropertyId, Object> properties) {
      return new ServiceComponentRequest(
          (String) properties.get(COMPONENT_CLUSTER_NAME_PROPERTY_ID),
          (String) properties.get(COMPONENT_SERVICE_NAME_PROPERTY_ID),
          (String) properties.get(COMPONENT_COMPONENT_NAME_PROPERTY_ID),
          null,
          (String) properties.get(COMPONENT_STATE_PROPERTY_ID));
    }
  }

  // ------ HostResourceProvider inner class ---------------------------------

  private static class HostResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            HOST_NAME_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private HostResourceProvider(Set<PropertyId> propertyIds,
                                 Map<Resource.Type, PropertyId> keyPropertyIds,
                                 AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      Set<HostRequest> requests = new HashSet<HostRequest>();
      for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().createHosts(requests);
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      HostRequest     hostRequest  = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<HostResponse> responses = getManagementController().getHosts(Collections.singleton(hostRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (HostResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Host);

        // TODO : properly handle more than one cluster
        if (null != hostRequest.getClusterNames()) {
          for (String clusterName : hostRequest.getClusterNames()) {
            if (response.getClusterNames().contains(clusterName)) {
              setResourceProperty(resource, HOST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
            }
          }
        }

        setResourceProperty(resource, HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
        setResourceProperty(resource, HOST_IP_PROPERTY_ID, response.getIpv4(), requestedIds);
        setResourceProperty(resource, HOST_TOTAL_MEM_PROPERTY_ID, Long.valueOf(response.getTotalMemBytes()), requestedIds);
        setResourceProperty(resource, HOST_CPU_COUNT_PROPERTY_ID, Long.valueOf(response.getCpuCount()), requestedIds);
        setResourceProperty(resource, HOST_OS_ARCH_PROPERTY_ID, response.getOsArch(), requestedIds);
        setResourceProperty(resource, HOST_OS_TYPE_PROPERTY_ID, response.getOsType(), requestedIds);
        // TODO ...
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      Set<HostRequest> requests = new HashSet<HostRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().updateHosts(requests);
      return getRequestStatus(null);
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      Set<HostRequest> requests = new HashSet<HostRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().deleteHosts(requests);
      return getRequestStatus(null);
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private HostRequest getRequest(Map<PropertyId, Object> properties) {
      return new HostRequest(
          (String)  properties.get(HOST_NAME_PROPERTY_ID),
          // TODO : more than one cluster
          properties.containsKey(HOST_CLUSTER_NAME_PROPERTY_ID) ?
              Collections.singletonList((String)  properties.get(HOST_CLUSTER_NAME_PROPERTY_ID)) :
                Collections.<String>emptyList(),
          null);
    }
  }

  // ------ HostComponentResourceProvider inner class ------------------------

  private static class HostComponentResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID,
            HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID,
            HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID,
            HOST_COMPONENT_HOST_NAME_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private HostComponentResourceProvider(Set<PropertyId> propertyIds,
                                          Map<Resource.Type, PropertyId> keyPropertyIds,
                                          AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
      for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
        requests.add(getRequest(propertyMap));
      }
      getManagementController().createHostComponents(requests);
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      ServiceComponentHostRequest hostComponentRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<ServiceComponentHostResponse> responses = getManagementController().getHostComponents(Collections.singleton(hostComponentRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ServiceComponentHostResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.HostComponent);
        setResourceProperty(resource, HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, response.getClusterName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_HOST_NAME_PROPERTY_ID, response.getHostname(), requestedIds);
        setResourceProperty(resource, HOST_COMPONENT_STATE_PROPERTY_ID, response.getLiveState(), requestedIds);
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {

        ServiceComponentHostRequest hostCompRequest = getRequest(propertyMap);

        Map<String, String> configMap = new HashMap<String,String>();

        for (Entry<PropertyId,Object> entry : propertyMap.entrySet()) {
          if (entry.getKey().getCategory().equals("config")) {
            configMap.put(entry.getKey().getName(), (String) entry.getValue());
          }
        }

        if (0 != configMap.size())
          hostCompRequest.setConfigVersions(configMap);

        requests.add(hostCompRequest);
      }
      return getRequestStatus(getManagementController().updateHostComponents(requests));
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      Set<ServiceComponentHostRequest> requests = new HashSet<ServiceComponentHostRequest>();
      for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
        requests.add(getRequest(propertyMap));
      }
      return getRequestStatus(getManagementController().deleteHostComponents(requests));
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private ServiceComponentHostRequest getRequest(Map<PropertyId, Object> properties) {
      return new ServiceComponentHostRequest(
          (String) properties.get(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID),
          (String) properties.get(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID),
          (String) properties.get(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID),
          (String) properties.get(HOST_COMPONENT_HOST_NAME_PROPERTY_ID),
          null,
          (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID));
    }
  }

  /**
   * Resource provider for configuration resources.
   */
  private static class ConfigurationResourceProvider extends ResourceProviderImpl {

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            CONFIGURATION_CLUSTER_NAME_PROPERTY_ID,
            CONFIGURATION_CONFIG_TYPE_PROPERTY_ID }));

    private ConfigurationResourceProvider(Set<PropertyId> propertyIds,
        Map<Resource.Type, PropertyId> keyPropertyIds,
        AmbariManagementController managementController) {

      super(propertyIds, keyPropertyIds, managementController);

    }

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      for (Map<PropertyId, Object> map : request.getProperties()) {

        String cluster = (String) map.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
        String type = (String) map.get(PropertyHelper.getPropertyId("type", ""));
        String tag = (String) map.get(PropertyHelper.getPropertyId("tag", ""));
        Map<String, String> configMap = new HashMap<String, String>();

        Iterator<Entry<PropertyId, Object>> it1 = map.entrySet().iterator();
        while (it1.hasNext()) {
          Entry<PropertyId, Object> entry = it1.next();
          if (entry.getKey().getCategory().equals("properties") && null != entry.getValue()) {
            configMap.put(entry.getKey().getName(), entry.getValue().toString());
          }
        }

        ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap);

        getManagementController().createConfiguration(configRequest);
      }
      return getRequestStatus(null);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws AmbariException {

      ConfigurationRequest configRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<ConfigurationResponse> responses = getManagementController().getConfigurations(Collections.singleton(configRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ConfigurationResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Configuration);
        resource.setProperty(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, response.getType());
        resource.setProperty(CONFIGURATION_CONFIG_TAG_PROPERTY_ID, response.getVersionTag());
        if (null != response.getConfigs() && response.getConfigs().size() > 0) {
          Map<String, String> configs = response.getConfigs();

          for (Entry<String, String> entry : configs.entrySet()) {
            PropertyId id = PropertyHelper.getPropertyId(entry.getKey(), "properties");
            resource.setProperty(id, entry.getValue());
          }
        }

        resources.add(resource);
      }
      return resources;
    }

    /**
     * Throws an exception, as Configurations cannot be updated.
     */
    @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws AmbariException {
      throw new AmbariException ("Cannot update a Configuration resource.");
    }

    /**
     * Throws an exception, as Configurations cannot be deleted.
     */
    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      throw new AmbariException ("Cannot delete a Configuration resource.");
    }

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    private ConfigurationRequest getRequest(Map<PropertyId, Object> properties) {
      String type = (String) properties.get(CONFIGURATION_CONFIG_TYPE_PROPERTY_ID);

      String tag = (String) properties.get(CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

      return new ConfigurationRequest(
          (String) properties.get(CONFIGURATION_CLUSTER_NAME_PROPERTY_ID),
          type, tag, new HashMap<String, String>());
    }
  }

  private static class ActionResourceProvider extends ResourceProviderImpl {
    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            ACTION_CLUSTER_NAME_PROPERTY_ID,
            ACTION_SERVICE_NAME_PROPERTY_ID }));

    private ActionResourceProvider(Set<PropertyId> propertyIds,
        Map<Resource.Type, PropertyId> keyPropertyIds,
        AmbariManagementController managementController) {

      super(propertyIds, keyPropertyIds, managementController);
    }

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      Set<ActionRequest> requests = new HashSet<ActionRequest>();
      for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
        requests.add(getRequest(propertyMap));
      }
      return getRequestStatus(getManagementController().createActions(requests));
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate)
        throws AmbariException {
      ActionRequest actionRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      Set<ActionResponse> responses = getManagementController().getActions(
          Collections.singleton(actionRequest));

      Set<Resource> resources = new HashSet<Resource>();
      for (ActionResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Action);
        resource.setProperty(ACTION_CLUSTER_NAME_PROPERTY_ID, response.getClusterName());
        resource.setProperty(ACTION_SERVICE_NAME_PROPERTY_ID, response.getServiceName());
        resource.setProperty(ACTION_ACTION_NAME_PROPERTY_ID, response.getActionName());
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate)
        throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    private ActionRequest getRequest(Map<PropertyId, Object> properties) {
      return new ActionRequest(
          (String)  properties.get(ACTION_CLUSTER_NAME_PROPERTY_ID),
          (String)  properties.get(ACTION_SERVICE_NAME_PROPERTY_ID),
          (String)  properties.get(ACTION_ACTION_NAME_PROPERTY_ID),
          null);
    }
  }

  // ------ RequestResourceProvider inner class ------------------------------

  private static class RequestResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            REQUEST_ID_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private RequestResourceProvider(Set<PropertyId> propertyIds,
                                          Map<Resource.Type, PropertyId> keyPropertyIds,
                                          AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId>         requestedIds         = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      Map<PropertyId, Object> predicateProperties  = getProperties(predicate);
      RequestStatusRequest    requestStatusRequest = getRequest(predicateProperties);

      String clusterName = (String) predicateProperties.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);

      Set<RequestStatusResponse> responses = getManagementController()
          .getRequestStatus(requestStatusRequest);
      Set<Resource> resources = new HashSet<Resource>();
      for (RequestStatusResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Request);
        setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
        setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, response.getRequestId(), requestedIds);
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private RequestStatusRequest getRequest(Map<PropertyId, Object> properties) {
      Long requestId = null;
      if (properties.get(REQUEST_ID_PROPERTY_ID) != null) {
        requestId = Long.valueOf((String) properties
            .get(REQUEST_ID_PROPERTY_ID));
      }
      return new RequestStatusRequest(requestId);
    }
  }

  // ------ TaskResourceProvider inner class ------------------------

  private static class TaskResourceProvider extends ResourceProviderImpl{

    private static Set<PropertyId> pkPropertyIds =
        new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
            TASK_ID_PROPERTY_ID}));

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds           the property ids
     * @param keyPropertyIds        the key property ids
     * @param managementController  the management controller
     */
    private TaskResourceProvider(Set<PropertyId> propertyIds,
                                          Map<Resource.Type, PropertyId> keyPropertyIds,
                                          AmbariManagementController managementController) {
      super(propertyIds, keyPropertyIds, managementController);
    }

    // ----- ResourceProvider ------------------------------------------------

    @Override
    public RequestStatus createResources(Request request) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
      Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
      TaskStatusRequest taskStatusRequest = getRequest(getProperties(predicate));

      // TODO : handle multiple requests
      LOG.info("Request to management controller " + taskStatusRequest.getRequestId() +
          " taskid " + taskStatusRequest.getTaskId());

      Set<TaskStatusResponse> responses = getManagementController().getTaskStatus(Collections.singleton(taskStatusRequest));
      LOG.info("Printing size of responses " + responses.size());
      for (TaskStatusResponse response: responses) {
        LOG.info("Printing response from management controller " + response.toString());
      }

      Set<Resource> resources = new HashSet<Resource>();
      for (TaskStatusResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Task);
        resource.setProperty(TASK_ID_PROPERTY_ID, response.getTaskId() + "");
        resource.setProperty(TASK_STAGE_ID_PROPERTY_ID, response.getStageId() + "");
        resource.setProperty(TASK_HOST_NAME_PROPERTY_ID, response.getHostName() + "");
        resource.setProperty(TASK_ROLE_PROPERTY_ID, response.getRole() + "");
        resource.setProperty(TASK_COMMAND_PROPERTY_ID, response.getCommand() + "");
        resource.setProperty(TASK_STATUS_PROPERTY_ID, response.getStatus());
        resource.setProperty(TASK_EXIT_CODE_PROPERTY_ID, response.getExitCode() + "");
        resource.setProperty(TASK_STDERR_PROPERTY_ID, response.getStderr() + "");
        resource.setProperty(TASK_STOUT_PROPERTY_ID, response.getStdout() + "");
        resource.setProperty(TASK_START_TIME_PROPERTY_ID, response.getStartTime() + "");
        resource.setProperty(TASK_ATTEMPT_CNT_PROPERTY_ID, response.getAttemptCount() + "");
        LOG.info("Creating resource " + resource.toString());
        resources.add(resource);
      }
      return resources;
    }

    @Override
    public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    @Override
    public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
      throw new UnsupportedOperationException("Not currently supported.");
    }

    // ----- utility methods -------------------------------------------------

    @Override
    protected Set<PropertyId> getPKPropertyIds() {
      return pkPropertyIds;
    }

    /**
     * Get a component request object from a map of property values.
     *
     * @param properties  the predicate
     *
     * @return the component request object
     */
    private TaskStatusRequest getRequest(Map<PropertyId, Object> properties) {
      String taskId = (String) properties.get(TASK_ID_PROPERTY_ID);
      Long task_id = (taskId == null? null: Long.valueOf(taskId));
      return new TaskStatusRequest(
          Long.valueOf((String) properties.get(TASK_REQUEST_ID_PROPERTY_ID)),
          task_id);
    }
  }
}
