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

import com.google.gson.Gson
import org.jfrog.util.docker.DockerImage

/**
 * Create Json config. <br>
 * Used to send to docker server while creating new container. <br>
 * Created by matank on 4/19/15.
 */
class CreateConfig {
    boolean AttachStderr = false
    boolean AttachStdin = false
    boolean AttachStdout = false
    List<String> Cmd = []
    int CpuShares = 0
    int Cpuset = 0
    String Domainname = null
    List<String> Entrypoint = null
    List<String> Env = []
    Map<String, Map> ExposedPorts = [:]
    String Hostname = null
    String Image = null
    String MacAddress = null
    long Memory = 0
    long MemorySwap = 0
    boolean NetworkDisabled = false
    def OnBuild = null
    boolean OpenStdin = false
    def PortSpecs = null
    boolean StdinOnce = false
    boolean Tty = false
    String User = null
    Map<String, Map> Volumes = [:]
    String WorkingDir = null

    Map<String, Object> HostConfig = [
            Binds          : [],
            Links          : [],
            LxcConf        : [],
            PortBindings   : [:],
            PublishAllPorts: false,
            Privileged     : false,
            ReadonlyRootfs : false,
            Dns            : [],
            DnsSearch      : [],
            ExtraHosts     : null,
            VolumesFrom    : [],
            CapAdd         : [],
            CapDrop        : [],
            RestartPolicy  : [
                    Name             : "",
                    MaximumRetryCount: 0
            ],
            NetworkMode    : "bridge",
            Devices        : []
    ]

    @Deprecated
    CreateConfig env(String key, String value) {
        this.Env.add(key + "=" + value)
        return this
    }

    /**
     * Add environment to container.
     * @param key environment key
     * @param value environment key
     */
    CreateConfig addEnv(String key, String value) {
        this.Env.add(key + "=" + value)
        return this
    }

    @Deprecated
    CreateConfig env(Map<String, String> env) {
        env.each {
            this.env(it, env.get(it))
        }
        return this
    }

    /**
     * Add environments to container.
     * @param env List of variables (e.g. ["key", "value"])
     * @return
     */
    CreateConfig addEnvs(Map<String, String> env) {
        env.each { k,v ->
            this.env(k, v)
        }
        return this
    }

    /**
     * Add command to container. <br>
     * NOTE: if EntryPoint is set with "exec $@" at the end to run another command,
     * then the commands should be written as bash script and supportMultiCommand should be set as FALSE.
     * (e.g. createConfig.addCommand("ifconfig ; hostname ; env", false))
     * @param command Command to run inside container.
     * @param supportMultiCommand if TRUE then "/bin/bash -c", if FALSE then nothing is prefixed.
     */
    CreateConfig addCommand(String command, boolean supportMultiCommand = true) {
        if (supportMultiCommand) {
            if (cmd.size() == 0) {
                cmd = ["/bin/bash", "-c", ""]
            }
            this.Cmd[2] = this.Cmd[2] + command + ";"
        } else {
            this.cmd = [command]
        }
        return this
    }

    /**
     * Add command to container. <br>
     * Use this method in case /bin/bash is not available in the image, In other cases pass your command as string.
     * (e.g. createConfig.addCommand(["ping", address]))
     * @param command List of command and args.
     */
    CreateConfig addCommand(List command) {
        this.cmd = command
        return this
    }

    @Deprecated
    CreateConfig commands(List<String> commands) {
        commands.each {
            this.addCommand(it)
        }
        return this
    }

    /**
     * Add list of commands. <br>
     * All the commands set here will be prefixed with "/bin/bash -c" to be able to run them all.
     * @param commands Commands to run inside container.
     */
    CreateConfig addCommands(List<String> commands) {
        commands.each {
            this.addCommand(it)
        }
        return this
    }

    CreateConfig attachStderr(boolean attachStrerr) {
        this.AttachStderr = attachStrerr
        return this
    }

    CreateConfig attachStdin(boolean attachStdin) {
        this.AttachStdin = attachStdin
        return this
    }

    CreateConfig attachStdout(boolean attachStdout) {
        this.AttachStdout = attachStdout
        return this
    }

    CreateConfig cpuset(int cpuset) {
        this.Cpuset = cpuset
        return this
    }

    CreateConfig cpuShares(int cpuShares) {
        this.CpuShares = cpuShares
        return this
    }

    CreateConfig domainName(String domainName) {
        this.Domainname = domainName
        return this
    }

    /**
     * Set entrypoint to run when container is started. <br>
     * If entrypoint is a bash script ended with $@ then make sure to add command
     * with supportMultiCommand set to FALSE. <br>
     * http://docs.docker.com/reference/builder/#entrypoint
     * @param entryPoint command to exec or path to file to run
     */
    CreateConfig addEntryPoint(String entryPoint) {
        if (this.Entrypoint == null) {
            this.Entrypoint = []
        }
        this.Entrypoint.add(entryPoint)
        return this
    }

    /**
     * Set entrypoint to run when container is started.<br>
     * This form is used to set in exec form. <br>
     * http://docs.docker.com/reference/builder/#entrypoint
     * @param entryPoints
     * @return
     */
    CreateConfig addEntryPoint(List<String> entryPoints) {
        entryPoints.each {
            this.addEntryPoint(it)
        }
    }

    /**
     * Add port to expose when container will run. <br>
     * Use it if you would to expose port the wasn't exposed when the image was built.
     * @param port Port to expose.
     * @param protocol Protocol to expose.
     */
    CreateConfig addExposedPort(int port, String protocol) {
        this.ExposedPorts.put(port + "/" + protocol.toLowerCase(), [:])
        return this
    }

    CreateConfig hostname(String hostname) {
        this.Hostname = hostname
        return this
    }

    /**
     * Set image for the container to start from. <br>
     * @param image Image full name (e.g dockerImage.getFullImageName())
     */
    CreateConfig image(DockerImage image) {
        return image(image.getFullImageName())
    }

    /**
     * Set image for the container to start from. <br>
     * Common use will be dockerImage.getFullImageName()
     * @param image Image full name
     */
    CreateConfig image(String image) {
        this.Image = image
        return this
    }

    CreateConfig macAddress(String macAddress) {
        this.MacAddress = macAddress
        return this
    }

    CreateConfig memory(long memory) {
        this.Memory = memory
        return this
    }

    CreateConfig memorySwap(long memorySwap) {
        this.MemorySwap = memorySwap
        return this
    }

    CreateConfig networkDisabled(boolean networkDisabled) {
        this.NetworkDisabled = networkDisabled
        return this
    }

    CreateConfig onBuild(def onBuild) {
        this.OnBuild = onBuild
        return this
    }

    CreateConfig openStdin(boolean openStdin) {
        this.OpenStdin = openStdin
        return this
    }

    CreateConfig portSpecs(def portSpecs) {
        this.PortSpecs = portSpecs
        return this
    }

    CreateConfig stdinOnce(boolean stdinOnce) {
        this.StdinOnce = stdinOnce
        return this
    }

    CreateConfig tty(boolean tty) {
        this.Tty = tty
        return this
    }

    CreateConfig setUser(String user) {
        this.User = user
        return this
    }

    /**
     * Add volume to the container. <br>
     * The make the actual mounting use dockerContainer.addBinds([hostPath, volume])
     * @param volume Will be the path of the volume inside to container
     */
    CreateConfig addVolume(String volume) {
        this.Volumes.put(volume, [:])
        return this
    }

    /**
     * Add multiple volumes.
     * @param volume List of volumes to create in the container
     */
    CreateConfig addVolumes(List<String> volumes) {
        volumes.each {
            this.addVolume(it)
        }
        return this
    }

    /**
     * Add multiple hosts to /etc/hosts
     * @param hostMap Map containing hostname as key and ip as value
     */
    CreateConfig addHosts(Map<String, String> hostMap) {
        hostMap.each { host, ip ->
            this.addHost(host, ip)
        }
        return this
    }

    /**
     * Add hosts to /etc/hosts
     * @param hostname hostname to use
     * @param ip The ip to redirect to
     */
    CreateConfig addHost(String hostname, String ip) {
        if (this.HostConfig.ExtraHosts == null) {
            this.HostConfig.ExtraHosts = []
        }
        this.HostConfig.ExtraHosts.add(hostname+":"+ip)
        return this
    }

    CreateConfig workingDir(String workingDir) {
        this.WorkingDir = workingDir
        return this
    }

    String toJson() {
        Gson gson = new Gson()
        return gson.toJson(this)
    }
}
