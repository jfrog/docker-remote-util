package org.jfrog.util.docker.configurations

import spock.lang.Specification


/**
 * Created by matank on 4/20/15.
 */
class StartConfigSpec extends Specification {
    def "Start Config Output"() {
        setup:
        StartConfig startConfig = new StartConfig()

        when:
        startConfig.addPortBinding(8081, "tcp", "0.0.0.0", 8081)
        startConfig.addLink("artifactory", "artifactory.local")
        startConfig.withPrivileges()
        startConfig.addBinds("a", "b")

        then:
        println startConfig.toJson()
    }

    def "Add Single Host"() {
        when:
        StartConfig config = new StartConfig()
                .addHost("www.aaa.com", "127.0.0.1")
        then:
        config.HostConfig.ExtraHosts == ["www.aaa.com:127.0.0.1"]
    }

    def "Add Multiple Hosts"() {
        when:
        StartConfig config = new StartConfig()
                .addHosts(["www.aaa.com": "127.0.0.1", "www.bbb.com": "1.1.1.1"])
        then:
        config.HostConfig.ExtraHosts == [
                "www.aaa.com:127.0.0.1",
                "www.bbb.com:1.1.1.1"
        ]
    }
}