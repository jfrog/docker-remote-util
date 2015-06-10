package org.jfrog.util.docker.utils

import spock.lang.Specification


/**
 * Created by matank on 5/17/15.
 */
class TarUtilsSpec extends Specification {

    File archiveFile = new File("archive.tar")

    def "Create Tar Archive From Single File"() {
        setup:
        File a = new File(this.getClass().getResource("a/b/c/a.txt").path)
        TarArchive tarArchive = new TarArchive(archiveFile.path)

        when:
        tarArchive.addFile(a)
        and:
        tarArchive.close()
        then:
        new File(archiveFile.path).exists()

        cleanup:
        archiveFile.delete()
    }

    def "Create Tar Archive From Multiple File"() {
        setup:
        File a = new File(this.getClass().getResource("a/b/c/a.txt").path)
        File b = new File(this.getClass().getResource("a/b/c/b.txt").path)
        TarArchive tarArchive = new TarArchive(archiveFile.path)

        when:
        tarArchive.addFile(a)
        tarArchive.addFile(b)
        and:
        tarArchive.close()
        then:
        new File(archiveFile.path).exists()

        cleanup:
        archiveFile.delete()
    }

    def "Create Tar Archive From Folder"() {
        setup:
        File a = new File(this.getClass().getResource("a").path)
        TarArchive tarArchive = new TarArchive(archiveFile.path)

        when:
        tarArchive.addFile(a)
        and:
        tarArchive.close()
        then:
        new File(archiveFile.path).exists()

        cleanup:
        archiveFile.delete()
    }
}