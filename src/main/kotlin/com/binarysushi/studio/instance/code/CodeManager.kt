package com.binarysushi.studio.instance.code

import com.binarysushi.studio.instance.clients.TopLevelDavFolders
import com.binarysushi.studio.instance.clients.WebDavClient
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipOutputStream

/**
 * Utilities for performing code related actions and interacting with the remote SFCC instance
 */
object CodeManager {

    /**
     * Creates a zip archive with the archiveName as the file name. The archive contains one directory
     * with the archiveName as the directory name with all the supplied cartridge folders as children
     *
     * archiveName.zip
     *     |-- archiveName
     *         |-- cartridge1
     *         |-- cartridge2
     *         |-- cartridge3
     *         |-- etc...
     *
     * This method cleans up the temporary directory, but does not clean up the temporary zip the
     * calling method should delete the file when done with it.
     */
    private fun createArchive(archiveName: String, dirs: List<File>): File {
        val timeFormat = SimpleDateFormat("hh-mm-ss")
        val tempDir = Paths.get(FileUtil.getTempDirectory(), "sfcc-studio-${timeFormat.format(Date())}").toFile()
        if (!tempDir.exists()) {
            FileUtil.createDirectory(tempDir)
        }

        val tempArchiveDir = Paths.get(tempDir.toString(), archiveName).toFile()
        FileUtil.createDirectory(tempArchiveDir)

        val zipFile = Paths.get(tempDir.toString(), "$archiveName.zip").toFile()

        try {
            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            for (dir in dirs) {
                if (dir.exists()) {
                    FileUtil.copyDir(dir, Paths.get(tempArchiveDir.toString(), dir.name).toFile())
                }
            }

            ZipUtil.addDirToZipRecursively(zipOutputStream, null, tempArchiveDir, archiveName, null, null)
            zipOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        FileUtil.delete(tempArchiveDir)

        return zipFile
    }

    fun zipVersion(versionName: String, cartridgeDirs: List<File>): File {
        return createArchive(versionName, cartridgeDirs)
    }

    fun zipCartridge(cartridgeDir: File): File {
        return createArchive(cartridgeDir.name, listOf(cartridgeDir))
    }

    //    fun deployVersion(davClient: WebDavClient, version: String) {}
//
//    /**
//     * Deploys a cartridge to an SFCC instance. This consists of uploading an archive, removing the previous
//     * version, unzipping the archive and removing the temporary files
//     */
    fun deployCartridge(davClient: WebDavClient, version: String, zipFile: File) {
        val serverVersionPath = "${TopLevelDavFolders.CARTRIDGES}/${version}"
        val serverZipPath = "${TopLevelDavFolders.CARTRIDGES}/${version}/${zipFile.name}.zip"

        try {
            if (!davClient.exists(serverVersionPath)) {
                davClient.createDirectory(serverVersionPath)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        try {
            davClient.put(serverZipPath, zipFile, "application/octet-stream")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            davClient.unzip(serverZipPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            davClient.delete(serverZipPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        FileUtil.delete(zipFile)
    }
//    fun listVersions(api: OCAPIClient) {}
//    fun activateVersion(api: OCAPIClient, version: String) {}
}
