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

import groovy.json.JsonBuilder
import org.apache.http.entity.ContentType
import org.apache.commons.lang.StringUtils
import org.jfrog.util.docker.inspect.State

/**
 * Created by matank on 5/31/15.
 */
class DockerExec {
    DockerContainer dockerContainer

    def commands
    boolean attachStdout = true
    boolean attachStdin = false
    boolean attachStderr = false
    boolean tty = false
    boolean privileged = false
    String user = null

    String id

    State state

    DockerExec(DockerContainer dockerContainer, def commands) {
        this.dockerContainer = dockerContainer
        this.commands = (commands instanceof String || commands instanceof GString) ? commands.split(" ") : commands
        this.attachStdin = attachStdin
        this.attachStdout = attachStdout
        this.attachStderr = attachStderr
        this.tty = tty

        state = new State()
    }

    DockerExec withAttachStdin(boolean attachStdin = true) {
        this.attachStdin = attachStdin
        return this
    }

    DockerExec withAttachStdout(boolean attachStdout = true) {
        this.attachStdout = attachStdout
        return this
    }

    DockerExec withAttachStderr(boolean attachStderr = true) {
        this.attachStderr = attachStderr
        return this
    }

    DockerExec tty(boolean tty) {
        this.tty = tty
        return this
    }

    DockerExec asUser(String user) {
        this.user = user
        return this
    }

    DockerExec withPrivileged(boolean privileged = true) {
        this.privileged = privileged
        return this
    }

    /**
     * Create Exec instance, still not running, use with doStart().
     * @return DockerExec
     */
    DockerExec doCreate() {

        Map body = [
                AttachStdin : attachStdin,
                AttachStdout: attachStdout,
                AttachStderr: attachStderr,
                Tty         : tty,
                Cmd         : commands,
                Privileged  : privileged
        ]

        if (user) {
            body.put("User", user)
        }

        def response = dockerContainer.dockerClient.post(
                "containers/${dockerContainer.id ? dockerContainer.id : dockerContainer.name}/exec",
                null,
                ContentType.JSON,
                null,
                ContentType.JSON,
                new JsonBuilder(body).toPrettyString(),
                null
        )
        this.id = response.Id
        return this
    }

    /**
     * Runs the Exec instance created with doCreate().
     * @param detach - if false, running in Sync mode, else, running in async mode.
     * @param tty
     * @return the output from the exec command.
     */
    String doStart(boolean detach = false, boolean tty = false) {
        String toReturn = dockerContainer.dockerClient.post(
                "exec/${this.id}/start",
                null,
                ContentType.TEXT,
                null,
                ContentType.JSON,
                new JsonBuilder([Detach: detach, Tty: tty]).toPrettyString(),
                null
        )

        if (StringUtils.isNotEmpty(toReturn)) {
            toReturn = toReturn.replaceAll("[\u0000\u0001\u0002]", "")
        }
        return toReturn
    }

    def inspect() {
        return dockerContainer.dockerClient.get("exec/$id/json", ContentType.JSON)
    }

    /**
     * Get status code of a container. <br>
     * @return Exit code of a container, -1 if container still running.
     */
    int exitCode() {
        state.init(inspect(), true)
        if (state.isRunning()) {
            return -1
        }
        return state.exitCode
    }
}
