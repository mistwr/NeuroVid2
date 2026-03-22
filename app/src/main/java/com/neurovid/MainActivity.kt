package com.neurovid

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
        "arvore" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/1f/2013-07-23_Himeji_Castle_and_surroundings_from_Seiho-en_garden.webm/2013-07-23_Himeji_Castle_and_surroundings_from_Seiho-en_garden.webm.360p.webm",
        "floresta" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/c/c7/The_Jungle_Book_opening.webm/The_Jungle_Book_opening.webm.360p.webm",
        "praia" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "mar" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "oceano" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
        "cidade" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/2c/ROC-Taiwan-Taipei-City-Night.webm/ROC-Taiwan-Taipei-City-Night.webm.360p.webm",
        "noite" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/2c/ROC-Taiwan-Taipei-City-Night.webm/ROC-Taiwan-Taipei-City-Night.webm.360p.webm",
        "natureza" to "https://upload.wikimedia.org/wikipedia/commons/transcoded/8/87/Waves_at_Acheron.ogv/Waves_at_Acheron.ogv.360p.webm",
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
                    val clip = descarregarClip(url)

                    statusText.text = "✅ Guardado: ${clip.name}"
                    Toast.makeText(
                        this@MainActivity,
                        "Vídeo em: ${clip.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()

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

    private suspend fun descarregarClip(videoUrl: String): File =
        withContext(Dispatchers.IO) {
            val dest = File(
                getExternalFilesDir(null),
                "neurovid_${System.currentTimeMillis()}.webm"
            )
            val req = Request.Builder()
                .url(videoUrl)
                .header("User-Agent", "NeuroVid/1.0")
                .build()
            client.newCall(req).execute().body!!.byteStream().use { i ->
                dest.outputStream().use { o -> i.copyTo(o) }
            }
            dest
        }
}
