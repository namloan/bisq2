package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.DmgImageMounter
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit


abstract class ExtractElectrumAppFromDmgFile : DefaultTask() {

    companion object {
        private const val MOUNT_DIR = "/Volumes/Electrum"
        private const val ELECTRUM_APP = "Electrum.app"
        private const val MOUNTED_ELECTRUM_APP_PATH = "$MOUNT_DIR/$ELECTRUM_APP"

        private const val CMD_TIMEOUT: Long = 25
    }

    @get:InputFile
    abstract val dmgFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    val electrumAppDestinationFile: Provider<Directory>
        get() = outputDirectory.dir(ELECTRUM_APP)

    @TaskAction
    fun extract() {
        val electrumAppFile = electrumAppDestinationFile.get().asFile
        if (electrumAppFile.exists()) {
            deleteElectrumAppFile()
        }

        electrumAppFile.mkdirs()

        val dmgImageMounter = DmgImageMounter(dmgFile.get().asFile, File(MOUNT_DIR))
        dmgImageMounter.use {
            dmgImageMounter.mount()
            copyElectrumAppToOutputDirectory()
        }
    }

    private fun deleteElectrumAppFile() {
        val electrumAppFilePath = electrumAppDestinationFile.get().asFile.toPath()
        Files.walk(electrumAppFilePath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete)
    }

    private fun copyElectrumAppToOutputDirectory() {
        val destinationDir = outputDirectory.get().asFile.absolutePath
        val copyProcess: Process = ProcessBuilder(
            "cp", "-r", MOUNTED_ELECTRUM_APP_PATH, destinationDir
        ).start()
        val isSuccess: Boolean = copyProcess.waitFor(CMD_TIMEOUT, TimeUnit.SECONDS)
        if (!isSuccess) {
            throw IllegalStateException("Could not copy Electrum.app to output directory.")
        }
    }
}