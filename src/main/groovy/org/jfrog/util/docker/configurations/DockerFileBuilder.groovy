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

package org.jfrog.util.docker.configurations

import org.jfrog.util.docker.constants.DockerFileCommands

/**
 *
 * Created by matank on 4/29/15.
 */
class DockerFileBuilder {

    List<String> commands
    /**
     * Directory to build the Dockerfile in.
     */
    public File folder

    /**
     * @param workDir temp directory will be created in this directory to contain all the necessary files.
     */
    DockerFileBuilder(String workDir) {
        this(new File(workDir))
    }

    /**
     * @param workDir temp directory will be created in this directory to contain all the necessary files.
     */
    DockerFileBuilder(File workDir) {
        createFolder(workDir)
        commands = []
    }

    private void createFolder(File rootFolder) {
        String folderName = System.currentTimeMillis().toString()
        folder = new File("${rootFolder.path}/$folderName")
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    void close() {
        deleteFiles(this.folder)
        this.folder.delete()
    }

    void deleteFiles(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                file.delete()
            } else {
                deleteFiles(file)
            }
        }
    }

    DockerFileBuilder from(String image, String tag = null, String digest = null) {
        return this.addCommandToCommandsList(buildLine(DockerFileCommands.FROM, "$image${tag != null ? ":$tag" : ""}${digest != null ? "@$digest" : ""}"))
    }

    DockerFileBuilder maintainer(String maintainer) {
        return this.addCommandToCommandsList(buildLine(DockerFileCommands.MAINTAINER, maintainer))
    }

    DockerFileBuilder run(boolean onBuild = false, String... args) {
        return this.addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.RUN, args))
    }

    DockerFileBuilder run(boolean onBuild = false, List<String> commands) {
        for (String command : commands) {
            this.run(onBuild, command)
        }
        return this
    }

    DockerFileBuilder cmd(boolean onBuild = false, String... args) {
        return this.addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.CMD, args))
    }

    DockerFileBuilder entryPoint(boolean onBuild = false, String... args) {
        return this.addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.ENTRYPOINT, args))
    }

    DockerFileBuilder label(String key, String value = null) {
        return this.addCommandToCommandsList(buildLine(DockerFileCommands.LABEL, "$key${value != null ? "=\"$value\"" : ""}"))
    }

    DockerFileBuilder expose(boolean onBuild = false, int ... ports) {
        return this.addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.EXPOSE, ports))
    }

    DockerFileBuilder env(boolean onBuild = false, String key, String value) {
        return this.addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.ENV, key, value))
    }

    DockerFileBuilder env(boolean onBuild = false, Map<Object, String> args) {
        return this.addCommandToCommandsList(getCommandInMapForm(onBuild, DockerFileCommands.ENV, args))
    }

    DockerFileBuilder add(boolean onBuild = false, String src, String dest) {
        copyFileToDockerFileFolder(src)
        return addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.ADD, src.split(["\\/"])[-1], dest))
    }

    DockerFileBuilder add(boolean onBuild = false, List<String> src, String dest) {
        for (int i = 0; i < src.size(); i++) {
            copyFileToDockerFileFolder(src[i])
            src[i] = src[i].split(["\\/"])[-1]
        }
        def args = src + [dest]
        return addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.ADD, args.toArray(new String[args.size()])))
    }

    DockerFileBuilder copy(boolean onBuild = false, List<String> src, String dest) {
        for (int i = 0; i < src.size(); i++) {
            copyFileToDockerFileFolder(src[i])
            src[i] = src[i].split(["\\/"])[-1]
        }
        def args = src + [dest]
        return addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.COPY, args.toArray(new String[args.size()])))
    }

    DockerFileBuilder copy(boolean onBuild = false, String src, String dest) {
        copyFileToDockerFileFolder(src)
        return addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.COPY, src.split(["\\/"]).split("/")[-1], dest))
    }

    DockerFileBuilder volume(boolean onBuild = false, String... args) {
        return this.addCommandToCommandsList(getCommandInListForm(onBuild, DockerFileCommands.VOLUME, args))
    }

    DockerFileBuilder user(boolean onBuild = false, String user) {
        return addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.USER, user))
    }

    DockerFileBuilder workdir(boolean onBuild = false, String workdir) {
        return addCommandToCommandsList(buildLine(onBuild, DockerFileCommands.WORKDIR, workdir))
    }

    private DockerFileBuilder addCommandToCommandsList(String command) {
        this.commands.add(command)
        return this
    }

    private String getCommandInListForm(boolean onBuild = false, DockerFileCommands dockerFileCommand, String... args) {
        if (args.size() == 1) {
            return buildLine(onBuild, dockerFileCommand, args)
        } else {
            return buildLine(dockerFileCommand, "[${args.collect { "\"$it\"" }.join(',')}]")
        }
    }

    private String getCommandInMapForm(boolean onBuild = false, DockerFileCommands dockerFileCommand, Map<String, String> args) {
        return buildLine(onBuild, dockerFileCommand, "${args.collect { it }.join(' ')}")
    }

    private String buildLine(boolean onBuild = false, DockerFileCommands dockerFileCommand, Object... parameters) {
        return "${onBuild ? "$DockerFileCommands.ONBUILD " : ""}${dockerFileCommand} ${parameters.join(" ")}"
    }

    @Override
    public String toString() {
        return commands.join("\n")
    }

    /**
     * Get Dockerfile, if not exists, creates it and returns.
     * @return
     */
    public File getDockerfile() {
        File dockerFile = new File("${folder.path}/Dockerfile")
        if (!dockerFile.exists()) {
            this.create()
        }
        return dockerFile
    }

    public DockerFileBuilder create() {
        File dockerFile = new File("${folder.path}/Dockerfile")
        dockerFile << this.toString()
        return this
    }

    private copyFileToDockerFileFolder(def file) {
        file = file instanceof String ? new File(file) : file

        new File("${folder.path}/${file.name}").bytes = file.bytes
    }
}
