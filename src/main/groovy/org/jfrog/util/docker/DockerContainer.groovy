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
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import org.jfrog.util.docker.configurations.CreateConfig
import org.jfrog.util.docker.configurations.StartConfig
import org.jfrog.util.docker.utils.TarArchive

/**
 * Created by matank on 12/22/2014.
 */
class DockerContainer {

    DockerClient dockerClient

    String id
    String name

    CreateConfig createConfig
    StartConfig startConfig

    DockerContainer(DockerClient dockerClient, String image, String id = null) {
        this.dockerClient = dockerClient
        this.createConfig = new CreateConfig()
        this.createConfig.Image = image
        this.startConfig = new StartConfig()

        this.id = id
    }

    DockerContainer doCreate(boolean throwsExceptionOnConflict = true) {

        def query = name != null ? [name: name] : null

        def response
        try {
            response = dockerClient.post(
                    "/containers/create",
                    query,
                    ContentType.JSON,
                    null,
                    ContentType.JSON,
                    this.createConfig ? this.createConfig.toJson() : null
            )
        } catch (HttpResponseException hre) {
            if (hre.getResponse().getStatusLine().statusCode == 409) {
                String errorMessage = "Container with the name '$name' already exists"
                if (throwsExceptionOnConflict) {
                    System.err.println errorMessage
                    throw hre
                } else {
                    println errorMessage
                }
            } else {
                throw hre
            }
        }

        this.id = response ? response.Id : null

//        return this.id ? this.id : this.name
        return this
    }

    /**
     * Start container and continue with the code immediately. <br>
     * If you would like to wait until it finishes use doStart(int waitExecutionToEndInSec). <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#start-a-container
     */
    DockerContainer doStart() {
        return doStart(0, null)
    }

    /**
     * Start container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#start-a-container
     * @param startConfig Pass specific startConfig without setting it in this object.
     */
    DockerContainer doStart(def startConfig) {
        return doStart(0, startConfig)
    }

    /**
     * Start container and wait until its execution finishes. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#start-a-container
     * @param waitExecutionToEndInSec Seconds to wait until execution finishes, 0 don't wait at all.
     */
    DockerContainer doStart(int waitExecutionToEndInSec) {
        return doStart(waitExecutionToEndInSec, null)
    }

    DockerContainer doStart(boolean debugOutput) {
        return doStart(0, null, debugOutput)
    }

    DockerContainer doStart(int waitExecutionToEndInSec, boolean debugOutput) {
        return doStart(waitExecutionToEndInSec, null, debugOutput)
    }

    /**
     * Start container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#start-a-container
     * @param waitExecutionToEndInSec Seconds to wait until execution finishes, 0 don't wait at all.
     * @param startConfig Pass specific startConfig without setting it in this object.
     */
    DockerContainer doStart(int waitExecutionToEndInSec, def startConfig, boolean debugOutput = false) {
        if (debugOutput) {
            println "Start container: ${name ? name : id} ${waitExecutionToEndInSec > 0 ? ", wait up to $waitExecutionToEndInSec seconds" : ""}"
        }
        try {
            def response = dockerClient.post(
                    "/containers/${id ? id : name}/start",
                    null,
                    ContentType.JSON,
                    null,
                    ContentType.JSON,
                    startConfig ? startConfig : this.startConfig.toJson()
            )
        } catch (HttpResponseException hre) {
            System.err.println( "Start container ERROR: ${hre.getMessage()}" )
            System.err.println( "Start container ERROR: ${hre.response.data.text}" )
            throw hre
        }


        while (waitExecutionToEndInSec > 0) {
            if (this.inspect().State.Running == false) {
                break
            }
            sleep(1000)
            waitExecutionToEndInSec--
        }

        return this
    }

    /**
     * Delete container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#remove-a-container
     * @param force Deletes container even it is in running state
     * @param deleteVolume Deletes all the data associated to this container
     */
    void doDelete(boolean force = false, boolean deleteVolume = false) {
        def query = [
                force: force,
                v    : deleteVolume
        ]

        def response = dockerClient.delete(
                "/containers/${id ? id : name}",
                query
        )
    }

    /**
     * Stop container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#stop-a-container
     * @param waitBeforeStopInSecs Seconds to wait before sending the stop signal.
     */
    DockerContainer doStop(int waitBeforeStopInSecs = 0) {
        def query = null
        if (waitBeforeStopInSecs > 0) {
            query = [t: waitBeforeStopInSecs]
        }
        dockerClient.post(
                "/containers/${id ? id : name}/stop",
                query,
                ContentType.JSON,
                null,
                ContentType.JSON
        )

        return this
    }

    /**
     * Exec another process inside this container, available only if the container is in Running state. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#exec-create
     * @param command Command to execute inside the container
     * @param attachStdout Collect the output of the command
     * @param attachStdin
     * @param attachStderr Collect the error output of the command
     * @param tty
     * @return DockerExec object which should be created using doCreate() and then started with doStart().
     */
    DockerExec exec(def command, boolean attachStdout = true, boolean attachStdin = false, boolean attachStderr = false, boolean tty = false) {
        assertIfEmpty()
        DockerExec dockerExec = new DockerExec(this, command)

        if (attachStdin) {
            dockerExec.attachStdin = attachStdin
        }

        if (attachStdout) {
            dockerExec.attachStdout = attachStdout
        }

        if (attachStderr) {
            dockerExec.attachStderr = attachStderr
        }

        if (tty) {
            dockerExec.tty = tty
        }

        return dockerExec
    }

    /**
     * Inspect the container config and status. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#inspect-a-container
     * @return Map as representation of the json returned.
     */
    def inspect() {
        assertIfEmpty()
        return get("${id ? id : name}/json", ContentType.JSON)
    }

    /**
     * Check if container is exists in docker server. <br>
     * @return True only if exists on server, false if isn't.
     */
    public boolean isExists() {
        assertIfEmpty()
        try {
            this.inspect()
            return true
        } catch (HttpResponseException hre) {
            if ( hre.response.getStatus() == 404) {
                return false
            } else {
                throw hre
            }
        }
    }

    /**
     * Get logs of the container.
     * @return Logs as string
     */
    String logs() {
        assertIfEmpty()
        String logs = get("${id ? id : name}/logs", ContentType.TEXT, [stdout: 1, stderr: 1])
        //Replace all special characters related to the header of each line
        //https://docs.docker.com/engine/reference/api/docker_remote_api_v1.19/#attach-to-a-container
        if (logs) {
            logs = logs.replaceAll("[\u0000\u0001\u0002]","")
        }
        return logs
    }

    /**
     * Show processes running inside container.
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#list-processes-running-inside-a-container
     * @return Map as representation of the json returned.
     */
    def top() {
        assertIfEmpty()
        return get("${id ? id : name}/top", ContentType.JSON)
    }

    /**
     * Show stats of running container. (Available from Docker remote-api v1.19)
     * @param stream Allows to stream the stats, default is false for a single request
     * https://docs.docker.com/engine/reference/api/docker_remote_api_v1.18/#get-container-stats-based-on-resource-usage
     * @return Map as representation of the json returned.
     */
    def stats(boolean stream = false) {
        assertIfEmpty()
        return get("${id ? id : name}/stats", ContentType.JSON, [stream:stream])
    }

    /**
     * Download file from the container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#copy-files-or-folders-from-a-container
     * @param fileToExtract File to download, pass full path, or relative to last working directory.
     * @param destinationFolder Folder to extract to, default is "tmp".
     * @return File and it places in project temp directory.
     */
    File copy(String fileFromContainer, String destinationFolder = null) {
        return downloadFile(fileFromContainer, destinationFolder)
    }

    /**
     * Get Map of all environment variables inside a container.
     * @return Map of variables in a container
     */
    Map<String, String> getEnvs() {
        assertIfEmpty()
        def inspectOutput = inspect()
        Map<String, String> maps = [:]

        inspectOutput.Config.Env.each {
            def (key, value) = it.split("=")
            maps.put(key,value)
        }

        return maps.size() > 0 ? maps : null
    }

    /**
     * Download file from the container. <br>
     * https://docs.docker.com/reference/api/docker_remote_api_v1.18/#copy-files-or-folders-from-a-container
     * @param fileToExtract File to download, pass full path, or relative to last working directory.
     * @param destinationFolder Folder to extract to, default is "tmp".
     * @return File and it places in project temp directory.
     */
    File downloadFile(String fileToExtract, String destinationFolder = null) {
        HttpResponseDecorator response = dockerClient.client.post(
                path: "/containers/${id ? id : name}/copy",
                body: [Resource: fileToExtract],
                requestContentType: ContentType.JSON,
                contentType: ContentType.BINARY
        )

        File tmpDir = createTempDirectory(destinationFolder)

        File downloadedTar = File.createTempFile(fileToExtract.split("/")[-1], ".tar", tmpDir)
        downloadedTar << response.getData()

        File toReturn = TarArchive.getFile(downloadedTar, fileToExtract)

        deleteTempFile(downloadedTar)

        return toReturn
    }

    private void deleteTempFile(File downloadedTar) {
        if (!downloadedTar.delete()) {
            println "[INFO] $downloadedTar.name wasn't deleted."
        }
    }

    private File createTempDirectory(String destinationFolder) {
        File tmpDir = (destinationFolder == null ? new File("tmp") : new File(destinationFolder))

        if (!tmpDir.exists()) {
            tmpDir.mkdir()
        }
        tmpDir
    }

    def get(String relativePath, ContentType contentType, Map<String, String> query = null) {
        dockerClient.get("containers/$relativePath", contentType, query)
    }

    @Override
    public String toString() {
        return this.id
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        DockerContainer that = (DockerContainer) o

        if (createConfig != that.createConfig) return false
        if (dockerClient != that.dockerClient) return false
        if (id != that.id) return false
        if (startConfig != that.startConfig) return false

        return true
    }

    private void assertIfEmpty() {
        if (id == null && name == null) {
            throw new NullPointerException("Id and Name are null, make sure the container is created to inspect it")
        }
    }
}
