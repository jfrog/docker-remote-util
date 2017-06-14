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
     */
    DockerRegistry(String registryHost, String username = null, String password = null, String email = null, String auth = null) {
        this.registryHost = registryHost
        this.username = username ? username : System.getenv(DOCKER_REGISTRY_USER_ENV)
        this.password = password ? password : System.getenv(DOCKER_REGISTRY_PASSWORD_ENV)
    }

    String getXRegistryAuth() {
        return """{
            \"username\":\"$username\",
            \"password\":\"$password\",
            \"auth\":\"${auth != null ? auth : ""}\",
            \"email\":\"${email != null ? email : ""}\"}""".bytes.encodeBase64()
    }

    Map<String, String> getXRegistryAuthHeader() {
        return ["X-Registry-Auth": getXRegistryAuth()]
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
