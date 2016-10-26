package org.jfrog.util.docker.utils

import com.github.zafarkhaja.semver.Version

/**
 * Created by matank on 26/10/2016.
 */
class VersionHelper {

    Version versionA
    Version versionB

    VersionHelper(String versionA, String versionB) {
        this.versionA = parseVersion(versionA)
        this.versionB = parseVersion(versionB)
    }

    boolean isAgreaterThanB() {
        return versionA.greaterThan(versionB)
    }

    boolean isBgreaterThanA() {
        return versionB.greaterThan(versionA)
    }

    boolean isAequalsB() {
        return versionA.equals(versionB)
    }

    /**
     * @return negative number if a less than b,
     * 0 if equals,
     * positive number if a greater than b.
     */
    int compareAtoB() {
        return versionA.compareTo(versionB)
    }

    Version parseVersion(String version) {
        List versions = version.split("[.]").toList()

        if (versions.size() < 3) {
            versions.add("0")
        }

        return Version.forIntegers(
                versions[0] ? versions[0].toInteger() : 0,
                versions[1] ? versions[1].toInteger() : 0,
                versions[2] ? versions[2].toInteger() : 0 )
    }
}
