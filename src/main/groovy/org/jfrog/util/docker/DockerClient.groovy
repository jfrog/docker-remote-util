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

import groovy.json.JsonSlurper
import groovyx.net.http.*
import org.apache.http.HttpResponse
import org.jfrog.util.docker.configurations.DockerFileBuilder
import org.jfrog.util.docker.filters.ContainerFilter
import org.jfrog.util.docker.utils.TarArchive

/**
 * Created by matank on 12/21/2014.
 */
class DockerClient {

    def url
    RESTClient client
    Map<String, Set<DockerImage>> images
    boolean isKeepAlive = true

    String apiVersion

    private final String contextName

    DockerClient(url) {
        if (url == null) {
            throw new NullPointerException("docker url cannot be null")
        }
        this.url = url
        client = new RESTClient(this.url)
        apiVersion = version().ApiVersion
        images = [:]
    }

    DockerImage image() {
        return new DockerImage(this)
    }

    /**
     * Please use image() instead and set the parameters there
     */
    @Deprecated
    DockerImage getImage(String image, String tag = null, String repo = null, String registry = null) {
        DockerImage dockerImage = new DockerImage(this, image, tag, repo, registry)
        return dockerImage
    }

    /**
     * Get images form org.jfrog.qa.docker server.
     * @param isAll - if true, returns entire list of images, false, only current process
     * @return Map of all images, Image id as key, each key contain similar images.
     */
    Map<String, Set<DockerImage>> getImagesFromServer(boolean isAll = false) {
        Map<String, String> query = null
        if (isAll) query = [all: "1"]
        def images = get("/images/json", ContentType.JSON, query)

        Map<String, Set<DockerImage>> imagesToReturn = [:]
        for (Object image : images) {
            def imageId = image.Id
            if (!imagesToReturn.containsKey(imageId)) imagesToReturn.put(imageId, [])
            image.RepoTags.each {
                DockerImage dockerImage = new DockerImage(this)
                def nameParts = it.split("/")
                if (nameParts[0].contains(":") || nameParts[0].contains(".") || nameParts[0].contains("localhost")) {
                    dockerImage.registry = new DockerRegistry(nameParts[0])
                }
                if (nameParts.size() > 2) {
                    dockerImage.repo = nameParts[1..-2].join("/")
                }

                def a = nameParts[-1].split(":")
                dockerImage.image = a[0]
                a[1] ? (dockerImage.tag = a[1]) : (dockerImage.tag = "latest")

                imagesToReturn.get(imageId).add(dockerImage)
            }
        }

        return imagesToReturn
    }

    void addImageToImagesMap(DockerImage dockerImage) {
        def imageId = dockerImage.imageId == null ? dockerImage.inspect().Id : dockerImage.imageId
        if (!this.images.containsKey(imageId)) this.images.put(imageId, [])
        this.images.get(imageId).add(dockerImage)
    }

    boolean removeImageFromImagesMap(DockerImage dockerImage) {
        def imageId = dockerImage.imageId == null ? dockerImage.inspect().Id : dockerImage.imageId
        if (!(this.images.containsKey(imageId))) return false
        this.images.get(imageId).remove(dockerImage)
    }

    /**
     * Get Containers from server based on specific filter.
     * @return Map of all running containers.
     */
    Map<String, DockerContainer> getContainersFromServer(boolean isAll, ContainerFilter filters) {
        return getContainersFromServer(isAll, null, null, null, false, filters)
    }

    Map<String, DockerContainer> getContainersFromServer(boolean isAll = false, Integer limit = null, String sinceId = null, String beforeId = null, boolean isShowSizes = false, ContainerFilter filters = null) {
        Map<String, String> query = [:]
        if (isAll) query.put("all", "1")
        if (limit != null) query.put("limit", limit.toString())
        if (sinceId != null) query.put("since", sinceId)
        if (beforeId != null) query.put("before", beforeId)
        if (isShowSizes) query.put("sizes", "1")
        if (filters != null) query.put("filters", filters.toJson())

        def containers = get("/containers/json", ContentType.JSON, query)

        Map<String, DockerContainer> containersToReturn = [:]

        for (Object container : containers) {
            DockerContainer dockerContainer = new DockerContainer(this, container.Image, container.Id)
            containersToReturn.put(container.Names[0].substring(container.Names[0].lastIndexOf("/") + 1), new DockerContainer(this, container.Image, container.Id))
        }

        return containersToReturn
    }

    /**
     * Ping server if Docker service is available.
     * @return True if Docker service is up, otherwise false.
     */
    boolean ping() {
        def response;
        try {
            response = this.get(
                    "_ping",
                    ContentType.TEXT
            )
        }
        //Server is dead
        catch (Exception e) {
            return false
        }
        return response.equals("OK")
    }

    void close() {
        client.shutdown()
    }

    String getUri() {
        return client.uri.toString()
    }

    protected InputStream getInputStream(String path, Map query = [:]) {
        // Otherwise when we leave the response.success block, ensureConsumed will be called to close the stream
        def ret = client.get([path: cleanPath(path), query: query, contentType: ContentType.BINARY])
        return ret.data
    }

    def <T> T get(String path, ContentType responseContentType = ContentType.ANY, Map query = [:],
                  def responseClass = null, Map headers = null) {
        get(path, query, responseContentType, responseClass, headers)
    }

    def <T> T get(String path, Map query, ContentType responseContentType = ContentType.ANY,
                  def responseClass = null, Map headers = null) {
        rest(Method.GET, path, query, responseContentType, responseClass, ContentType.ANY, null, headers)
    }

    def <T> T delete(String path, Map query = null, ContentType responseContentType = ContentType.ANY,
                     def responseClass = null, Map addlHeaders = null) {
        rest(Method.DELETE, path, query, responseContentType, responseClass, ContentType.TEXT, null, addlHeaders)
    }

    def <T> T post(String path, Map query = null, ContentType responseContentType = ContentType.ANY,
                   def responseClass = null, ContentType requestContentType = ContentType.JSON, requestBody = null, Map addlHeaders = null, long contentLength = -1) {
        rest(Method.POST, path, query, responseContentType, responseClass, requestContentType, requestBody, addlHeaders, contentLength)
    }

    def <T> T put(String path, Map query = null, ContentType responseContentType = ContentType.ANY,
                  def responseClass = null, ContentType requestContentType = ContentType.JSON, requestBody = null, Map addlHeaders = null, long contentLength = -1) {
        rest(Method.PUT, path, query, responseContentType, responseClass, requestContentType, requestBody, addlHeaders, contentLength)
    }

    /**
     *
     * @param method
     * @param path
     * @param query
     * @param responseType Header and parsing type for response
     * @param responseClass If responseType is JSON, a class to have the object mapped to can be provided. Not necessarily a class, it could be a TypeReference, hence it's not the same as T
     * @param requestContentType
     * @param requestBody
     * @param addlHeaders
     * @return
     */
    private def <T> T rest(Method method, String path, Map query = null, responseType = ContentType.ANY,
                           def responseClass, ContentType requestContentType = ContentType.JSON, requestBody = null, Map addlHeaders = null, long contentLength = -1) {
        def originalParser
        try {
            // Let us send one thing to the server and parse it differently. But we have to careful to restore it.
            originalParser = client.parser.getAt(responseType)

            // Change the JSON parser on the fly, not thread safe since the responseClass we're using is locked to this call's argument
            if (responseClass == String) {
                // They just want us to leave the response alone
                client.parser.putAt(responseType, originalTextParser)
            } else if (responseType == ContentType.JSON && responseClass) {
                client.parser.putAt(ContentType.JSON) { HttpResponse resp ->
                    InputStream is = resp.entity.content
                    return objectMapper.readValue(is, responseClass) as T
                }
            }

            restWrapped(method, path, query, responseType, responseClass, requestContentType, requestBody, addlHeaders, contentLength)
        } finally {
            client.parser.putAt(responseType, originalParser)
        }
    }

    def cleanPath(path) {
        def fullpath = "${path}"
        // URIBuilder will try to "simplify" the url, so if there's double slashes it'll remove the first part of the uri purposefully
        if (!fullpath.startsWith('/')) {
            fullpath = "/${fullpath}"
        }
        return fullpath
    }

    private def <T> T restWrapped(Method method, String path, Map query = null, responseType = ContentType.ANY,
                                  def responseClass, ContentType requestContentType = ContentType.JSON, requestBody = null, Map addlHeaders = null, long contentLength = -1) {
        def ret

        //TODO Ensure requestContentType is not null
        def allHeaders = addlHeaders ? addlHeaders.clone() : [:]
//        if (contentLength >= 0) {
//            allHeaders << [(HTTP.CONTENT_LEN): contentLength]
//        }

        if (!isKeepAlive) {
            allHeaders.put("Connection", "close")
        }

        // responseType will be used as the type to parse (XML, JSON, or Reader), it'll also create a header for Accept
        // Artifactory typically only returns one type, so let's ANY align those two. The caller can then do the appropriate thing.
        // Artifactory returns Content-Type: application/vnd.org.jfrog.artifactory.repositories.RepositoryDetailsList+json when ANT is used.
        client.request(method, responseType) { req ->
            // There might be a conflict with the getUri above, so lets be specific and typesafe
            URIBuilder uriBuilder = delegate.uri
            uriBuilder.path = cleanPath(path)
            if (query) {
                uriBuilder.query = query
            }
            headers.putAll(allHeaders)

            if (requestBody) {
                if (requestContentType == ContentType.JSON) {
                    // Override JSON
                    send ContentType.JSON, new JsonSlurper().parseText(requestBody)
                } else {
                    send requestContentType, requestBody
                }
            } else {
                setRequestContentType(requestContentType)
            }

            response.success = { HttpResponse resp, slurped ->
                if (responseClass == String || responseType == ContentType.TEXT) {
                    // we overrode the parser to be just text, but oddly it returns an InputStreamReader instead of a String
                    ret = slurped?.text
                } else {
                    ret = slurped // Will be type according to responseType
                }
            }

            def statusCodes = ['400', '401', '402', '403', '404', '405', '409', '500', '501', '502',]

            statusCodes.each {
                response."$it" = { resp ->
                    System.err.println "ERROR: ${resp.responseBase.getStatusLine()}"
                    def body = resp.responseData
                    if (body) {
                        System.err.println "ERROR: ${body.text}"
                    }
                    throw new HttpResponseException(resp)
                }
            }
        }
        ret
    }

    def build(File dockerFile, String image, String tag = "latest", boolean quiet = false, boolean removeTempContainers = false, boolean noCache) {
        TarArchive tarArchive = new TarArchive("${dockerFile.parent}/Dockerfile.tar")
        tarArchive.addFile(dockerFile)
        tarArchive.close()

        this.post(
                "build",
                ["dockerfile": "Dockerfile",
                 "t"         : "$image:$tag",
                 "q"         : "$quiet",
                 "rm"        : "$removeTempContainers",
                 "nocache"   : "$noCache"],
                ContentType.JSON,
                null,
                ContentType.BINARY,
                tarArchive.tarFile.bytes,
                null,
                tarArchive.tarFile.size()
        )
    }

    def build(DockerFileBuilder dfb, String image, String tag = "latest", boolean quiet = false, boolean removeTempContainers = false, boolean noCache) {
        TarArchive tarArchive = new TarArchive("${dfb.folder}/Dockerfile.tar")
        for (File file : dfb.folder.listFiles()) {
            if (file.name != "Dockerfile.tar") {
                tarArchive.addFile(file)
            }
        }
        tarArchive.close()

        this.post(
                "build",
                ["dockerfile": "Dockerfile",
                 "t"         : "$image:$tag",
                 "q"         : "$quiet",
                 "rm"        : "$removeTempContainers",
                 "nocache"   : "$noCache"],
                ContentType.JSON,
                null,
                ContentType.BINARY,
                tarArchive.tarFile.bytes,
                null,
                tarArchive.tarFile.size()
        )
    }

    /**
     * Build image
     * @param dfb Can be either Dockerfile or File,
     * @param dockerImage Docker image to build
     * @param removeTempContainers Keep temp containers created during the build, default is false
     * @throws ClassCastException If dfb is not File or DockerFileBuilder ClassCastException will be thrown.
     */
    def build(def dfb, DockerImage dockerImage, boolean removeTempContainers = false, boolean noCache = false) throws ClassCastException {
        if (dfb instanceof DockerFileBuilder || dfb instanceof File) {
            build(dfb, dockerImage.getFullImageName(false), dockerImage.tag, false, removeTempContainers, noCache)
            return dockerImage
        } else {
            throw new ClassCastException("dfb object must be File or DockerFileBuilder")
        }
    }

    Map version() {
        get("version", ContentType.JSON)
    }

    boolean setKeepAlive() {
        this.isKeepAlive = true
    }

    boolean setConnectionClose() {
        this.isKeepAlive = false
    }
}
