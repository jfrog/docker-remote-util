/*
 * Copyright (C) 2015 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.util.docker

import groovy.json.JsonOutput

/**
 * Created by matank on 4/27/15.
 */
class DockerRegistry {

    static final String DOCKER_REGISTRY_USER_ENV="DOCKER_REGISTRY_USER"
    static final String DOCKER_REGISTRY_PASSWORD_ENV="DOCKER_REGISTRY_PASSWORD"

    String registryHost
    String username
    String password
    String email
    String auth

    /**
     * Init DockerRegistry object
     * @param registryHost - The Registry
     * @param username - Username for authentication, If null, it will check if DOCKER_REGISTRY_USER env exists
     * @param password - Password for authentication, If null, it will check if DOCKER_REGISTRY_PASSWORD env exists
     * @param email
     * @param auth
     * @param anonymousAccess - if DOCKER_REGISTRY_USER and DOCKER_REGISTRY_PASSWORD is set, then ignores them.
     */
    DockerRegistry(String registryHost, String username = null, String password = null, String email = null, String auth = null, boolean anonymousAccess = false) {
        this.registryHost = registryHost
        if (!anonymousAccess) {
            this.username = username ? username : System.getenv(DOCKER_REGISTRY_USER_ENV)
            this.password = password ? password : System.getenv(DOCKER_REGISTRY_PASSWORD_ENV)
        }
    }

    DockerRegistry(String registryHost, Boolean anonymousAccess) {
        this(registryHost, null, null, null, null, anonymousAccess)
    }

    String getXRegistryAuth(boolean getWithHost = false) {
        String toReturn
        if (! getWithHost) {
            toReturn = """{
            \"username\":\"$username\",
            \"password\":\"$password\",
            \"auth\":\"${auth != null ? auth : ""}\",
            \"email\":\"${email != null ? email : ""}\"}"""
        } else {
            toReturn = JsonOutput.toJson(["$registryHost":["username":username, "password":password, "serveraddress": registryHost]])
        }
        return toReturn.bytes.encodeBase64()
    }

    Map<String, String> getXRegistryAuthHeader(boolean getWithHost = false) {
        return ["X-Registry-Auth": getXRegistryAuth(getWithHost)]
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DockerRegistry that = (DockerRegistry) o

        if (auth != that.auth) return false
        if (email != that.email) return false
        if (password != that.password) return false
        if (registryHost != that.registryHost) return false
        if (username != that.username) return false

        return true
    }
}
