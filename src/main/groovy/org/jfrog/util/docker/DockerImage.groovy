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

import groovyx.net.http.ContentType

/**
 * Created by matank on 4/27/15.
 */
class DockerImage {

    final String IMAGES_URL_PREFIX = "images"

    DockerClient dockerClient
    String image
    String tag
    String repo
    DockerRegistry registry

    String imageId

    Map<String, DockerContainer> containers

    def response

    DockerImage(DockerClient dockerClient) {
        this(dockerClient, null, null, null, null)
    }

    DockerImage(DockerClient dockerClient, String image, String tag, String repo, DockerRegistry registry) {
        this.dockerClient = dockerClient
        this.image = image
        if (tag == null) this.tag = "latest"
        this.repo = repo
        this.registry = registry
        this.containers = [:]
    }

    /**
     * Creates the image on the docker server, by pulling it if not exists. <br>
     * The response can be collected using "dockerImage.response".
     */
    DockerImage doCreate() {
        Map<String, String> query = [fromImage: getFullImageName(false)]
        if (tag != null) query.put("tag", tag)

        def response = post("create", ContentType.JSON, ContentType.TEXT, null, query)

        dockerClient.addImageToImagesMap(this)

        this.response = response
        return this
    }

    /**
     * Pushes the image to the required registry
     * @return HttpResponseDecorator
     */
    Map doPush() {
        Map<String, String> query = [tag: tag]
//        return dockerClient.request(Method.POST, "$IMAGES_URL_PREFIX/${getFullImageName(false)}/push", ContentType.JSON, query, registry.getXRegistryAuthHeader())
//        return post("${getFullImageName(false)}/push", ContentType.JSON, query, registry.getXRegistryAuthHeader())
        return post("${getFullImageName(false)}/push", ContentType.ANY, ContentType.JSON, null, query, registry.getXRegistryAuthHeader())
    }

    /**
     * Tags this image as new image (targetImage).
     * @param targetImage - DockerImage object.
     * @param isForce - override exists image.
     * @return True in successful tag, otherwise, throws exception.
     */
    def doTag(DockerImage targetImage, boolean isForce = false) {
        Map<String, String> query = [repo: targetImage.getFullImageName(false), tag: targetImage.tag]
        if (isForce) query.put("force", "1")
        post("${this.getFullImageName()}/tag", ContentType.JSON, ContentType.JSON, null, query)

        dockerClient.addImageToImagesMap(targetImage)
        return true
    }

    /**
     * Searches for the image in the specified registry, ignores the tag.
     * @return HttpResponseDecorator
     */
    List doSearch() {
        Map<String, String> query = [term: getFullImageName(false)]
        return get("search", ContentType.JSON, query)
    }

    /**
     * Deletes the image from docker server, doesn't delete it from the registry.org.jfrog.qa.docker* @param isForce - deletes even if there is dependency at it.
     * @param isNoPrune - prevent the deletion of parent images
     * @return HttpResponseDecorator
     */
    List doDelete(boolean isForce = false, boolean isNoPrune = false) {
        Map<String, String> query

        if (isForce || isNoPrune) {
            query = [:]
            if (isForce) query.put("force", "1")
            if (isNoPrune) query.put("noprune", "1")
        }

        dockerClient.removeImageFromImagesMap(this)
//        def response = dockerClient.request(Method.DELETE, "$IMAGES_URL_PREFIX/${getFullImageName()}", ContentType.JSON, query)
        def response = delete("${getFullImageName()}", ContentType.JSON, query)
        return response
    }

    /**
     * Inspect the image, queries the docker server at real time.org.jfrog.qa.docker* @return Json
     */
    def inspect() {
        return get("${getFullImageName()}/json", ContentType.JSON)
    }

    def history() {
        return get("${getFullImageName()}/history", ContentType.JSON)
    }

    /**
     * Creates new container from this image.
     * @param containerName - set the name for the new container
     * @return DockerContainer
     */
    DockerContainer getNewContainer(String containerName = null) {
        DockerContainer dockerContainer = new DockerContainer(this.dockerClient, this.toString())
        dockerContainer.name = containerName
        this.containers.put(dockerContainer.id, dockerContainer)

        return dockerContainer
    }

    private def post(String relativePath, ContentType requestContentType, ContentType contentType,
                     def body, Map<String, String> query = null, Map<String, String> headers = null) {
        /*return dockerClient.post("$IMAGES_URL_PREFIX/$relativePath",
                requestContentType,
                contentType,
                body,
                query,
                headers)*/
        return dockerClient.post("$IMAGES_URL_PREFIX/$relativePath",
                query,
                requestContentType,
                null,
                contentType,
                body,
                headers
        )
    }

    private def delete(String relativePath, ContentType contentType, Map<String, String> query) {
        return dockerClient.delete("$IMAGES_URL_PREFIX/$relativePath", query, contentType)
    }

    private def get(String relativePath, ContentType contentType, Map<String, String> query = null) {
        return dockerClient.get("$IMAGES_URL_PREFIX/$relativePath", contentType, query)
    }

    /**
     * Set the required registry to pull from or to push to.
     * @param registry
     */
    DockerImage registry(DockerRegistry registry) {
        this.registry =  registry
        return this
    }

    /**
     * Set the required registry to pull from or to push to.
     * @param registry
     */
    DockerImage registry(String registry) {
        this.registry = new DockerRegistry(registry)
        return this
    }

    /**
     * Please use registry("registry") instead
     */
    @Deprecated
    DockerImage fromRegistry(DockerRegistry dockerRegistry) {
        this.registry = dockerRegistry
        return this
    }

    @Deprecated
    DockerImage fromRegistry(String registry, String username = null, String password = null, String email = null, String auth = null) {
        this.registry = new DockerRegistry(registry, username, password, email, auth)
        return this
    }

    DockerImage namespace(String namespace) {
        this.repo = namespace
        return this
    }

    /**
     * Please use namespace("namespace") instead
     */
    @Deprecated
    DockerImage fromRepo(String repo) {
        this.repo = repo
        return this
    }

    DockerImage repository(String repository) {
        this.image = repository
        return this
    }

    DockerImage tag(String tag) {
        this.tag = tag
        return this
    }

    /**
     * Please use tag("tag") instead
     */
    @Deprecated
    DockerImage withTag(String tag) {
        this.tag = tag
        return this
    }

    String getFullImageName(boolean addTag = true) {
        StringBuilder sb = new StringBuilder()

        if (registry != null) sb.append("$registry.registryHost/")
        if (repo != null) sb.append("$repo/")
        sb.append(image)
        if (addTag && tag != null) sb.append(":$tag")

        return sb.toString()
    }

    @Override
    public String toString() {
        return getFullImageName()
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DockerImage that = (DockerImage) o

        if (dockerClient != that.dockerClient) return false
        if (image != that.image) return false
        if (registry != that.registry) return false
        if (repo != that.repo) return false
        if (tag != that.tag) return false

        return true
    }
}
