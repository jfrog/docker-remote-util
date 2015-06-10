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

package org.jfrog.util.docker.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.utils.IOUtils

/**
 * Created by matank on 5/17/15.
 */
class TarArchive {

    File tarFile
    OutputStream fOut
    OutputStream bOut
    GzipCompressorOutputStream gzOut
    TarArchiveOutputStream tOut

    TarArchive(def tarFile) {
        tarFile = tarFile instanceof File ? tarFile : new File(tarFile)
        this.tarFile = tarFile
        fOut = new FileOutputStream(tarFile)
        bOut = new BufferedOutputStream(fOut)
        gzOut = new GzipCompressorOutputStream(bOut)
        tOut = new TarArchiveOutputStream(gzOut)
    }

    void close() {
        tOut.finish();
        tOut.close();
        gzOut.close();
        bOut.close();
        fOut.close();
    }

    void addFile(File f, String base = "") throws IOException {
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) {
            IOUtils.copy(new FileInputStream(f), tOut);
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFile(new File(child.getAbsolutePath()), entryName + "/");
                }
            }
        }
    }

    static File getFile(File tarFile, String fileToExtract = null) {
        /* Read TAR File into TarArchiveInputStream */
        TarArchiveInputStream myTarFile = new TarArchiveInputStream(new FileInputStream(tarFile));
        if (fileToExtract != null) {
            fileToExtract = fileToExtract.split("/")[-1]
        }
        /* To read individual TAR file */
        TarArchiveEntry entry = null;
        String individualFile;
        int offset;
        FileOutputStream outputFile = null;
        /* Create a loop to read every single entry in TAR file */
        while ((entry = myTarFile.getNextTarEntry()) != null) {
            /* Get the name of the file */
            individualFile = entry.getName();
            if (fileToExtract == null || fileToExtract == individualFile) {
                /* Get Size of the file and create a byte array for the size */
                byte[] content = new byte[(int) entry.getSize()];
                offset = 0;
                /* Read file from the archive into byte array */
                myTarFile.read(content, offset, content.length - offset);
                /* Define OutputStream for writing the file */
                outputFile = new FileOutputStream(new File(tarFile.parent, individualFile));
                /* Use IOUtiles to write content of byte array to physical file */
                outputFile << content
                /* Close Output Stream */
                outputFile.close();
            }
        }
        /* Close TarAchiveInputStream */
        myTarFile.close();

        if (fileToExtract != null) {
            return new File(tarFile.parent, fileToExtract)
        } else {
            return new File(tarFile.parent)
        }
    }
}