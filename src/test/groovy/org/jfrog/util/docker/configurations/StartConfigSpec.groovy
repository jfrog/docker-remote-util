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
}