// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.indexing.diagnostic

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.UnindexedFileStatus
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

class ScanningStatistics(val fileSetName: String) {
  var numberOfScannedFiles: Int = 0

  /**
   * Number of files that have been scanned (iterated) by a different iterator than the one used to iterate this [fileSetName].
   * If multiple "file iterators" would iterate the same file, only one of the iterators actually "scans" the file
   * (and increments [numberOfScannedFiles] in his statistics).
   */
  var numberOfSkippedFiles: Int = 0

  var numberOfFilesForIndexing: Int = 0
  var numberOfFilesFullyIndexedByInfrastructureExtension: Int = 0
  var listOfFilesFullyIndexedByInfrastructureExtension = arrayListOf<String>()

  var scanningTime: TimeNano = 0
  var statusTime: TimeNano = 0

  var timeProcessingUpToDateFiles: TimeNano = 0
  var timeUpdatingContentLessIndexes: TimeNano = 0
  var timeIndexingWithoutContent: TimeNano = 0

  val scannedFiles = arrayListOf<ScannedFile>()

  data class ScannedFile(val portableFilePath: PortableFilePath, val isUpToDate: Boolean, val wasFullyIndexedByInfrastructureExtension: Boolean)

  fun addStatus(fileOrDir: VirtualFile, unindexedFileStatus: UnindexedFileStatus, statusTime: Long, project: Project) {
    if (fileOrDir.isDirectory) return
    numberOfScannedFiles++
    if (unindexedFileStatus.shouldIndex) {
      numberOfFilesForIndexing++
    }
    this.statusTime += statusTime

    timeProcessingUpToDateFiles += unindexedFileStatus.timeProcessingUpToDateFiles
    timeUpdatingContentLessIndexes += unindexedFileStatus.timeUpdatingContentLessIndexes
    timeIndexingWithoutContent += unindexedFileStatus.timeIndexingWithoutContent

    if (unindexedFileStatus.wasFullyIndexedByInfrastructureExtension) {
      numberOfFilesFullyIndexedByInfrastructureExtension++
      listOfFilesFullyIndexedByInfrastructureExtension.add(fileOrDir.toString())
    }
    if (IndexDiagnosticDumper.shouldDumpPathsOfIndexedFiles) {
      val portableFilePath = getIndexedFilePath(fileOrDir, project)
      scannedFiles += ScannedFile(portableFilePath, !unindexedFileStatus.shouldIndex,
                                  unindexedFileStatus.wasFullyIndexedByInfrastructureExtension)
    }
  }

  private fun getIndexedFilePath(file: VirtualFile, project: Project): PortableFilePath = try {
    PortableFilePaths.getPortableFilePath(file, project)
  }
  catch (e: Exception) {
    PortableFilePath.AbsolutePath(file.url)
  }

  fun addScanningTime(time: Long) {
    scanningTime += time
  }
}
