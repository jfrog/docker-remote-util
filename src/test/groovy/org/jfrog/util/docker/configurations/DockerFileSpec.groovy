package org.jfrog.util.docker

import org.jfrog.util.docker.configurations.DockerFileBuilder
import spock.lang.Specification

/**
 * Created by matank on 4/29/15.
 */
class DockerFileSpec extends Specification {

    DockerFileBuilder dockerFileBuilder
    String command

    def "FROM command"() {
        when:
        dockerFileBuilder.from "ubuntu"
        then:
        dockerFileBuilder.commands[-1] == "FROM ubuntu"

        when:
        dockerFileBuilder.from "ubuntu", "latest"
        then:
        dockerFileBuilder.commands[-1] == "FROM ubuntu:latest"

        when:
        dockerFileBuilder.from "ubuntu", null, "123"
        then:
        dockerFileBuilder.commands[-1] == "FROM ubuntu@123"

        when:
        dockerFileBuilder.from "ubuntu", "latest", "123"
        then:
        dockerFileBuilder.commands[-1] == "FROM ubuntu:latest@123"
    }

    def "MAINTAINER command"() {
        when: "Run in /bin/bash -c form"
        dockerFileBuilder.maintainer "autotest@autotest.com"
        then:
        dockerFileBuilder.commands[-1] == "MAINTAINER autotest@autotest.com"
    }

    def "RUN command"() {
        when: "Run in /bin/bash -c form"
        dockerFileBuilder.run "apt-get update && apt-get install -y wget"
        then:
        dockerFileBuilder.commands[-1] == "RUN apt-get update && apt-get install -y wget"

        when: "Run in /bin/bash -c form"
        dockerFileBuilder.run true, "apt-get update && apt-get install -y wget"
        then:
        dockerFileBuilder.commands[-1] == "ONBUILD RUN apt-get update && apt-get install -y wget"

        when: "Run in exec -c form"
        dockerFileBuilder.run "apt-get", "update", "&&", "apt-get", "install", "-y", "wget"
        then:
        dockerFileBuilder.commands[-1] == "RUN [\"apt-get\",\"update\",\"&&\",\"apt-get\",\"install\",\"-y\",\"wget\"]"
    }

    def "CMD command"() {
        when: "Run in /bin/bash -c form"
        dockerFileBuilder.cmd "apt-get update && apt-get install -y wget"
        then:
        dockerFileBuilder.commands[-1] == "CMD apt-get update && apt-get install -y wget"

        when: "Run in exec -c form"
        dockerFileBuilder.cmd "apt-get", "update", "&&", "apt-get", "install", "-y", "wget"
        then:
        dockerFileBuilder.commands[-1] == "CMD [\"apt-get\",\"update\",\"&&\",\"apt-get\",\"install\",\"-y\",\"wget\"]"
    }

    def "LABEL command"() {
        when:
        dockerFileBuilder.label "label-without-value"
        then:
        dockerFileBuilder.commands[-1] == "LABEL label-without-value"

        when:
        dockerFileBuilder.label "label-with-value", "value"
        then:
        dockerFileBuilder.commands[-1] == "LABEL label-with-value=\"value\""
    }

    def "EXPOSE command"() {
        when:
        dockerFileBuilder.expose 8080
        then:
        dockerFileBuilder.commands[-1] == "EXPOSE 8080"

        when:
        dockerFileBuilder.expose 8080, 80, 443
        then:
        dockerFileBuilder.commands[-1] == "EXPOSE 8080 80 443"

        when:
        dockerFileBuilder.expose true, 8080, 80, 443
        then:
        dockerFileBuilder.commands[-1] == "ONBUILD EXPOSE 8080 80 443"
    }

    def "ENV command"() {
        when:
        dockerFileBuilder.env "aaa", "123"
        then:
        dockerFileBuilder.commands[-1] == "ENV aaa 123"

        when:
        dockerFileBuilder.env([aaa: "123", bbb: "456"])
        then:
        dockerFileBuilder.commands[-1] == "ENV aaa=123 bbb=456"
    }

    def "ADD command"() {
        when:
        dockerFileBuilder.add this.getClass().getResource("a/a.txt").path, "/destDir/"
        then:
        dockerFileBuilder.commands[-1] == "ADD a.txt /destDir/"

        when:
        dockerFileBuilder.add([this.getClass().getResource("a/a.txt").path, this.getClass().getResource("b.txt").path], "/destDir/")
        then:
        dockerFileBuilder.commands[-1] == "ADD [\"a.txt\",\"b.txt\",\"/destDir/\"]"
    }

    def "COPY command"() {
        when:
        dockerFileBuilder.copy this.getClass().getResource("a/a.txt").path, "/destDir/"
        then:
        dockerFileBuilder.commands[-1] == "COPY a.txt /destDir/"

        when:
        dockerFileBuilder.copy([this.getClass().getResource("a/a.txt").path, this.getClass().getResource("b.txt").path], "/destDir/")
        then:
        dockerFileBuilder.commands[-1] == "COPY [\"a.txt\",\"b.txt\",\"/destDir/\"]"
    }

    def "ENTRYPOINT command"() {
        when: "Run in /bin/bash -c form"
        dockerFileBuilder.entryPoint "./run_script.sh a b c"
        then:
        dockerFileBuilder.commands[-1] == "ENTRYPOINT ./run_script.sh a b c"

        when: "Run in exec -c form"
        dockerFileBuilder.entryPoint "./run_script.sh", "a", "b", "c"
        then:
        dockerFileBuilder.commands[-1] == "ENTRYPOINT [\"./run_script.sh\",\"a\",\"b\",\"c\"]"
    }

    def "VOLUME command"() {
        when:
        dockerFileBuilder.volume "/new_volume"
        then:
        dockerFileBuilder.commands[-1] == "VOLUME /new_volume"

        when: "Run in exec -c form"
        dockerFileBuilder.volume "/new_volume", "/new_volume2"
        then:
        dockerFileBuilder.commands[-1] == "VOLUME [\"/new_volume\",\"/new_volume2\"]"
    }

    def "USER command"() {
        when:
        dockerFileBuilder.user "tester"
        then:
        dockerFileBuilder.commands[-1] == "USER tester"
    }

    def "WORKDIR command"() {
        when:
        dockerFileBuilder.workdir "/path/to/workdir"
        then:
        dockerFileBuilder.commands[-1] == "WORKDIR /path/to/workdir"
    }

    def "Dockerfile example"() {
        when:
        dockerFileBuilder.from("ubuntu")
                .maintainer("tester@tester.com")
                .label("Description", 'This image is used to start the foobar executable" Vendor="ACME Products" Version="1.0"')
                .run("apt-get update && apt-get install -y x11vnc xvfb firefox")
                .run("mkdir ~/.vnc")
                .run("x11vnc -storepasswd 1234 ~/.vnc/passwd")
                .run("bash -c 'echo \"firefox\" >> /.bashrc'")
                .expose(5900)
                .cmd("x11vnc", "-forever", "-usepw", "-create")

        then:
        String expected = "FROM ubuntu\n" +
                "MAINTAINER tester@tester.com\n" +
                "LABEL Description=\"This image is used to start the foobar executable\" Vendor=\"ACME Products\" Version=\"1.0\"\"\n" +
                "RUN apt-get update && apt-get install -y x11vnc xvfb firefox\n" +
                "RUN mkdir ~/.vnc\n" +
                "RUN x11vnc -storepasswd 1234 ~/.vnc/passwd\n" +
                "RUN bash -c 'echo \"firefox\" >> /.bashrc'\n" +
                "EXPOSE 5900\n" +
                "CMD [\"x11vnc\",\"-forever\",\"-usepw\",\"-create\"]"
        dockerFileBuilder.toString() == expected
    }

    def "Dockerfile creation"() {
        when:
        File dockerFile = dockerFileBuilder.from("ubuntu")
                .maintainer("tester@tester.com")
                .label("Description", 'This image is used to start the foobar executable" Vendor="ACME Products" Version="1.0"')
                .run("apt-get update && apt-get install -y x11vnc xvfb firefox")
                .run("mkdir ~/.vnc")
                .run("x11vnc -storepasswd 1234 ~/.vnc/passwd")
                .run("bash -c 'echo \"firefox\" >> /.bashrc'")
                .expose(5900)
                .cmd("x11vnc", "-forever", "-usepw", "-create")
                .getDockerfile()

        then:
        dockerFile.exists()
    }

    def setup() {
        dockerFileBuilder = new DockerFileBuilder(new File(this.getClass().getResource("").path))
    }

    def cleanup() {
        dockerFileBuilder.close()
        println dockerFileBuilder
        println "--------------------------------------------"
    }

}