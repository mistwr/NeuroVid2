package com.neurovid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import okhttp3.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val VIDEOS = mapOf(
        "arvore" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "floresta" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "praia" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "mar" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "oceano" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "cidade" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/2c/ROC-Taiwan-Taipei-City-Night.webm/ROC-Taiwan-Taipei-City-Night.webm.360p.webm",
        "noite" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/2c/ROC-Taiwan-Taipei-City-Night.webm/ROC-Taiwan-Taipei-City-Night.webm.360p.webm",
        "default" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGerar = findViewById<Button>(R.id.btnGerar)
        val promptInput = findViewById<EditText>(R.id.promptInput)
        val statusText = findViewById<TextView>(R.id.statusText)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnGerar.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Escreve um prompt!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnGerar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    statusText.text = "🧠 A analisar prompt..."
                    val url = escolherVideo(prompt)

                    statusText.text = "🎬 A descarregar vídeo..."
                    val clip = descarregarClip(url, prompt)

                    statusText.text = "✅ A abrir vídeo..."
                    abrirVideo(clip)

                } catch (e: Exception) {
                    statusText.text = "❌ Erro: ${e.message}"
                } finally {
                    btnGerar.isEnabled = true
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun escolherVideo(prompt: String): String {
        val p = prompt.lowercase()
        VIDEOS.forEach { (palavra, url) ->
            if (p.contains(palavra)) return url
        }
        return VIDEOS["default"]!!
    }

    private suspend fun descarregarClip(videoUrl: String, prompt: String): File =
        withContext(Dispatchers.IO) {
            // Guarda em Downloads — pasta visível
            val downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloads.mkdirs()

            val nome = "NeuroVid_${prompt.take(10)}_${System.currentTimeMillis()}.webm"
            val dest = File(downloads, nome)

            val req = Request.Builder()
                .url(videoUrl)
                .header("User-Agent", "NeuroVid/1.0")
                .build()

            client.newCall(req).execute().body!!.byteStream().use { i ->
                dest.outputStream().use { o -> i.copyTo(o) }
            }
            dest
        }

    private fun abrirVideo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/webm")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Abrir vídeo com..."))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Vídeo guardado em Downloads!",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
