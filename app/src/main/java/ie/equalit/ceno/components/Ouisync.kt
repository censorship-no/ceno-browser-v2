package ie.equalit.ceno.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.equalitie.ouisync.Repository
import org.equalitie.ouisync.Session
import org.equalitie.ouisync.ShareToken
import org.equalitie.ouisync.File as ouiFile
import java.io.File
import java.nio.charset.Charset


class Ouisync (
    context : Context
) {
    lateinit var session : Session
    lateinit var writeToken : ShareToken
    private var sessionError by mutableStateOf<String?>(null)
    private var protocolVersion by mutableStateOf<Int>(0)
    private val rootDir : File? = context.filesDir
    private var configDir : String = "$rootDir/config"
    private var storeDir : String = "$rootDir/store"
    private var repositories by mutableStateOf<Map<String, Repository>>(mapOf())

    fun createSession() {
        try {
            session = Session.create(configDir)
            sessionError = null
        } catch (e: Exception) {
            Log.e(TAG, "Session.create failed", e)
            sessionError = e.toString()
        } catch (e: java.lang.Error) {
            Log.e(TAG, "Session.create failed", e)
            sessionError = e.toString()
        }
    }

    suspend fun getProtocolVersion() : Int {
        session.let {
            protocolVersion = it.currentProtocolVersion()
        }
        return protocolVersion
    }

    suspend fun createRepository(name: String, token: String = "") {
        val session = this.session ?: return

        if (repositories.containsKey(name)) {
            Log.e(TAG, "repository named \"$name\" already exists")
            return
        }

        var shareToken: ShareToken? = null

        if (token.isNotEmpty()) {
            shareToken = ShareToken.fromString(session, token)
        }

        val repo = Repository.create(
            session,
            "$storeDir/$name.$DB_EXTENSION",
            readSecret = null,
            writeSecret = null,
            shareToken = shareToken,
        )

        writeToken = repo.createShareToken(name = "cenoProfile")

        Log.d(TAG, writeToken.toString())

        repositories = repositories + (name to repo)
    }

    suspend fun openRepositories() {
        val session = this.session
        val files = File(storeDir).listFiles() ?: arrayOf()

        for (file in files) {
            if (file.name.endsWith(".$DB_EXTENSION")) {
                try {
                    val name = file
                        .name
                        .substring(0, file.name.length - DB_EXTENSION.length - 1)
                    val repo = Repository.open(session, file.path)

                    Log.i(TAG, "Opened repository $name")

                    repositories = repositories + (name to repo)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open repository at ${file.path}")
                    continue
                }
            }
        }
    }

    suspend fun writeToRepo(contentW : String, charset: Charset = Charsets.UTF_8) {
        val session = this.session
        val files = File(storeDir).listFiles() ?: arrayOf()

        for (file in files) {
            if (file.name.endsWith(".$DB_EXTENSION")) {
                try {
                    val name = file
                        .name
                        .substring(0, file.name.length - DB_EXTENSION.length - 1)
                    val repo = Repository.open(session, file.path)

                    Log.i(TAG, "Opened repository $name")

                    val fileW = ouiFile.create(repo, "prefs.txt")
                    fileW.write(0, contentW.toByteArray(charset))
                    fileW.flush()
                    fileW.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open repository at ${file.path}")
                    continue
                }
            }
        }
    }

    companion object {
        private const val TAG = "OUISYNC"
        private val DB_EXTENSION = "ouisyncdb"
    }
}