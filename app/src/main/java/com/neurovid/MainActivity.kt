package com.neurovid

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val PEXELS_KEY = "COLOCA_AQUI_KEY_DO_PEXELS"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnGerar = findViewById<Button>(R.id.btnGerar)
        val promptInput = findViewById<EditText>(R.id.promptInput)
        val statusText = findViewById<TextView>(R.id.statusText)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val radio6s = findViewById<RadioButton>(R.id.radio6s)

        btnGerar.setOnClickListener {
            val prompt = promptInput.text.toString().trim()
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Escreve um prompt!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val duracao = if (radio6s.isChecked) 6 else 10
            btnGerar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    statusText.text = "🧠 A analisar prompt..."
                    val query = traduzirPrompt(prompt)

                    statusText.text = "🎬 A descarregar clip..."
                    val clip = descarregarClip(query)

                    statusText.text = "✅ Vídeo guardado: ${clip.name}"
                    Toast.makeText(
                        this@MainActivity,
                        "Guardado em: ${clip.absolutePath}",
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

    private fun traduzirPrompt(prompt: String): String {
        val dic = mapOf(
            "floresta" to "forest", "praia" to "beach",
            "cidade" to "city", "montanha" to "mountain",
            "oceano" to "ocean", "pôr do sol" to "sunset",
            "amanhecer" to "sunrise", "chuva" to "rain",
            "natureza" to "nature", "rio" to "river",
            "pessoas" to "people", "estrada" to "road"
        )
        var q = prompt.lowercase()
        dic.forEach { (pt, en) -> q = q.replace(pt, en) }
        return q.take(50)
    }

    private suspend fun descarregarClip(query: String): File =
        withContext(Dispatchers.IO) {
            val url = "https://api.pexels.com/videos/search" +
                "?query=${query}&per_page=1&size=small"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", PEXELS_KEY)
                .build()

            val body = client.newCall(req).execute().body!!.string()
            val json = JSONObject(body)
            val videos = json.getJSONArray("videos")
            if (videos.length() == 0) throw Exception("Sem resultados para: $query")

            val files = videos.getJSONObject(0).getJSONArray("video_files")
            val videoUrl = files.getJSONObject(0).getString("link")

            val dest = File(
                getExternalFilesDir(null),
                "neurovid_${System.currentTimeMillis()}.mp4"
            )
            URL(videoUrl).openStream().use { i ->
                dest.outputStream().use { o -> i.copyTo(o) }
            }
            dest
        }
}
