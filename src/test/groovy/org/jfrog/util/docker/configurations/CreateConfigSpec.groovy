package org.jfrog.util.docker.configurations

import spock.lang.Specification

/**
 * Created by matank on 4/19/15.
 */
class CreateConfigSpec extends Specification {

    def "Create Config Output"() {
        when:
        CreateConfig containerConfig = new CreateConfig().
                attachStderr(true).
                attachStdin(true).
                attachStdout(true).
                addCommand("hostname").
                addCommand("ifconfig").
                cpuset(1).
                cpuShares(1).
                domainName("aaa").
                addEntryPoint("entrypoint").
                env("PATH", "/usr/bin").
                addExposedPort(8081, "tcp").
                hostname("test_hostname").
                image("ubuntu:14.04").
                macAddress("aa:bb:cc:dd:ee:ff").
                memory(0).
                memorySwap(0).
                networkDisabled(false).
                openStdin(false).
                stdinOnce(false).
                tty(true).
                setUser("test_user").
                addVolume("/tmp").
                workingDir("/tmp/working_dir").
                addLinkToNetwork("test_container")

        then:
        println containerConfig.toJson()
    }

    def "Add Single Host"() {
        when:
        CreateConfig config = new CreateConfig()
                .addHost("www.aaa.com", "127.0.0.1")
        then:
        config.HostConfig.ExtraHosts == ["www.aaa.com:127.0.0.1"]
    }

    def "Add Multiple Hosts"() {
        when:
        CreateConfig config = new CreateConfig()
                .addHosts(["www.aaa.com": "127.0.0.1", "www.bbb.com": "1.1.1.1"])
        then:
        config.HostConfig.ExtraHosts == [
                "www.aaa.com:127.0.0.1",
                "www.bbb.com:1.1.1.1"
        ]
    }
}