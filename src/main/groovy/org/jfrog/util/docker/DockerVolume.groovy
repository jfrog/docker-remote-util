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
import org.apache.http.client.HttpResponseException
import groovyx.net.http.*

/**
 * Created by matank on 4/27/15.
 */
class DockerVolume {

    final String VOLUMES_URL_PREFIX = "volumes"

    DockerClient dockerClient
    String name
    Map<String, String> labels
    String driver

    def response

    DockerVolume(DockerClient dockerClient) {
        this(dockerClient, null, null, null)
    }

    DockerVolume(DockerClient dockerClient, String name, Map<String, String> labels, String driver) {
        this.dockerClient = dockerClient
        this.name = name
        this.labels = labels
        this.driver = driver
    }
/**
 * Creates the volume on the docker server. <br>
 * The response can be collected using "dockerVolume.response".
 */
    DockerVolume doCreate() {
        Map body = [:]
        ["name", "labels", "driver"].each {
            if (this[it] != null) {
                body.put(it.capitalize(), this[it])
            }
        }

        def response = post("create",
                ContentType.JSON,
                ContentType.JSON,
                JsonOutput.toJson(body))

        this.response = response
        return this
    }

    /**
     * Deletes the image from docker server, doesn't delete it from the registry.org.jfrog.qa.docker* @param isForce - deletes even if there is dependency at it.
     * @param isNoPrune - prevent the deletion of parent images
     * @return HttpResponseDecorator
     */
    List doDelete() {
        def response = delete(name, ContentType.JSON, null)
        return response
    }

    /**
     * Inspect the volume, queries the docker server at real time.org.jfrog.qa.docker* @return Json
     */
    def inspect() {
        return get(this.name, ContentType.JSON)
    }

    /**
     * Check if container is exists in docker server. <br>
     * @return True only if exists on server, false if isn't.
     */
    public boolean isExists() {
        try {
            this.inspect()
            return true
        } catch (HttpResponseException hre) {
            if (hre.response.getStatus() == 404) {
                return false
            } else {
                throw hre
            }
        }
    }

    private def post(String relativePath, ContentType requestContentType, ContentType contentType,
                     def body, Map<String, String> query = null, Map<String, String> headers = null) {
        /*return dockerClient.post("$VOLUMES_URL_PREFIX/$relativePath",
                requestContentType,
                contentType,
                body,
                query,
                headers)*/
        return dockerClient.post("$VOLUMES_URL_PREFIX/$relativePath",
                query,
                requestContentType,
                null,
                contentType,
                body,
                headers
        )
    }

    private def delete(String relativePath, ContentType contentType, Map<String, String> query) {
        return dockerClient.delete("$VOLUMES_URL_PREFIX/$relativePath", query, contentType)
    }

    private def get(String relativePath, ContentType contentType, Map<String, String> query = null) {
        return dockerClient.get("$VOLUMES_URL_PREFIX/$relativePath", contentType, query)
    }

    DockerVolume name(String name) {
        this.name = name
        return this
    }

    DockerVolume labels(Map labels) {
        this.labels = labels
        return this
    }

    DockerVolume driver(Map driver) {
        this.driver = driver
        return this
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DockerVolume that = (DockerVolume) o

        if (dockerClient != that.dockerClient) return false
        if (driver != that.driver) return false
        if (labels != that.labels) return false
        if (name != that.name) return false

        return true
    }

    int hashCode() {
        int result
        result = dockerClient.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + driver.hashCode()
        return result
    }
}
