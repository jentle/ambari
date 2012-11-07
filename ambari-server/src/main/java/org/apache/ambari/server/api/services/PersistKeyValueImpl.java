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

package org.apache.ambari.server.api.services;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.mortbay.jetty.Response;

import com.google.inject.Singleton;

@Singleton
public class PersistKeyValueImpl {
  Map<String, String> keyvalues = new HashMap<String, String>();
  
  public synchronized String getValue(String key) {
    if (keyvalues.containsKey(key)) {
      return keyvalues.get(key);
    }
    throw new WebApplicationException(Response.SC_NOT_FOUND);
  }
  
  public synchronized void put(String key, String value) {
    keyvalues.put(key, value);
  }
  
  public synchronized Map<String, String> getAllKeyValues() {
    Map<String, String> clone = new HashMap<String, String>(keyvalues);
    return keyvalues;
  }
}
