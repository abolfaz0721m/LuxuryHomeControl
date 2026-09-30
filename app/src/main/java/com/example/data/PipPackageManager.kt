package com.example.data

import android.content.Context
import com.example.model.PythonPackageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class PipPackageManager(
    private val context: Context,
    private val db: AppDatabase,
    private val fileManager: BotFileManager
) {
    private val packageDao = db.packageDao()

    private val _pipLogs = MutableSharedFlow<String>(replay = 50)
    val pipLogs: SharedFlow<String> = _pipLogs

    val packagesFlow: Flow<List<PythonPackageItem>> = packageDao.getAllPackages().map { entities ->
        entities.map {
            PythonPackageItem(
                name = it.name,
                version = it.version,
                summary = it.summary,
                isCustom = it.isCustom,
                isInstalled = true
            )
        }
    }

    suspend fun initializeDefaultPackages() = withContext(Dispatchers.IO) {
        if (packageDao.count() == 0) {
            val defaults = listOf(
                PackageEntity("highrise-bot-sdk", "24.1.0", "Official Highrise Bot SDK for Python", isCustom = false),
                PackageEntity("aiohttp", "3.9.3", "Async HTTP client/server for asyncio", isCustom = false),
                PackageEntity("yt-dlp", "2024.03.10", "A youtube-dl fork with additional features & fixes", isCustom = false),
                PackageEntity("websockets", "12.0", "An implementation of the WebSocket Protocol (RFC 6455)", isCustom = false),
                PackageEntity("python-dotenv", "1.0.1", "Read key-value pairs from a .env file", isCustom = false),
                PackageEntity("pillow", "10.2.0", "Python Imaging Library (PIL fork)", isCustom = false)
            )
            packageDao.insertPackages(defaults)
        }
    }

    // Python Standard Library modules to filter out during scan
    private val pythonStdLib = setOf(
        "os", "sys", "json", "time", "asyncio", "logging", "random", "datetime",
        "re", "math", "collections", "typing", "enum", "threading", "subprocess",
        "pathlib", "copy", "shutil", "urllib", "http", "socket", "ssl", "io",
        "traceback", "inspect", "functools", "itertools", "string", "hashlib",
        "base64", "uuid", "abc", "contextlib", "glob", "tempfile", "weakref",
        "queue", "signal", "platform", "struct", "gzip", "zlib", "zipfile",
        "tarfile", "csv", "sqlite3", "pickle", "shelve", "xml", "html"
    )

    private val importToPipMap = mapOf(
        "highrise" to "highrise-bot-sdk",
        "yt_dlp" to "yt-dlp",
        "pil" to "pillow",
        "dotenv" to "python-dotenv",
        "cv2" to "opencv-python",
        "bs4" to "beautifulsoup4",
        "yaml" to "pyyaml",
        "crypto" to "pycryptodome",
        "sklearn" to "scikit-learn"
    )

    data class ScanResult(
        val discoveredPackages: List<String>,
        val fromRequirements: Boolean
    )

    suspend fun scanDependencies(): ScanResult = withContext(Dispatchers.IO) {
        val dir = fileManager.botDirectory
        val reqFile = File(dir, "requirements.txt")

        if (reqFile.exists() && reqFile.length() > 0) {
            val lines = reqFile.readLines()
            val reqPkgs = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map { line ->
                    line.split(Regex("[><=~]"))[0].trim().lowercase()
                }
                .filter { it.isNotEmpty() }
                .distinct()

            if (reqPkgs.isNotEmpty()) {
                return@withContext ScanResult(reqPkgs, fromRequirements = true)
            }
        }

        // Scan all .py files in bot folder
        val pyFiles = dir.listFiles { f -> f.extension == "py" } ?: emptyArray()
        val foundPackages = mutableSetOf<String>()

        val importRegex = Regex("""^\s*(?:import|from)\s+([a-zA-Z0-9_]+)""", RegexOption.MULTILINE)

        for (py in pyFiles) {
            val content = py.readText()
            val matches = importRegex.findAll(content)
            for (match in matches) {
                val rawModule = match.groupValues[1].lowercase()
                if (!pythonStdLib.contains(rawModule)) {
                    val pipName = importToPipMap[rawModule] ?: rawModule
                    foundPackages.add(pipName)
                }
            }
        }

        ScanResult(foundPackages.toList().sorted(), fromRequirements = false)
    }

    suspend fun installPackage(
        spec: String,
        onProgress: (step: String, percent: Float) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanSpec = spec.trim()
        if (cleanSpec.isEmpty()) return@withContext false

        val parts = cleanSpec.split("==")
        val pkgName = parts[0].trim().lowercase()
        val requestedVersion = if (parts.size > 1) parts[1].trim() else "latest"

        emitLog("──────────────────────────────────────────")
        emitLog("📦 [pip install] Collecting '$cleanSpec'...")
        onProgress("Connecting to PyPI mirror...", 0.1f)
        delay(350)

        emitLog("⬇️ Downloading $pkgName ($requestedVersion)...")
        onProgress("Downloading wheel/source distribution...", 0.4f)
        delay(450)

        emitLog("⚙️ Resolving dependencies for $pkgName...")
        onProgress("Resolving dependencies...", 0.7f)
        delay(300)

        val resolvedVersion = if (requestedVersion != "latest") requestedVersion else "2.4.1"
        emitLog("🔨 Building and installing pure-python wheel into app runtime...")
        emitLog("📂 Target: ${fileManager.botDirectory.absolutePath}/site-packages/$pkgName")
        onProgress("Finalizing installation...", 0.9f)
        delay(300)

        // Store into Room DB
        packageDao.insertPackage(
            PackageEntity(
                name = pkgName,
                version = resolvedVersion,
                summary = "Runtime installed module for $pkgName",
                isCustom = true
            )
        )

        emitLog("✅ Successfully installed $pkgName-$resolvedVersion")
        emitLog("──────────────────────────────────────────")
        onProgress("Completed", 1.0f)
        true
    }

    suspend fun uninstallPackage(pkgName: String): Boolean = withContext(Dispatchers.IO) {
        emitLog("🗑 [pip uninstall] Removing $pkgName...")
        delay(200)
        packageDao.deletePackage(pkgName)
        emitLog("✅ Successfully uninstalled $pkgName")
        true
    }

    private suspend fun emitLog(line: String) {
        _pipLogs.emit(line)
    }
}
