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

    private val client = OkHttpClient()

    private val TRADUCOES = mapOf(
        "floresta" to "forest", "praia" to "beach",
        "cidade" to "city", "montanha" to "mountain",
        "oceano" to "ocean", "pôr do sol" to "sunset",
        "amanhecer" to "sunrise", "chuva" to "rain",
        "natureza" to "nature", "rio" to "river",
        "pessoas" to "people", "estrada" to "road",
        "fogo" to "fire", "neve" to "snow",
        "flores" to "flowers", "animais" to "animals",
        "mar" to "sea", "campo" to "field",
        "céu" to "sky", "noite" to "night"
    )

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

            btnGerar.isEnabled = false
            progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    statusText.text = "🧠 A analisar prompt..."
                    val query = traduzirPrompt(prompt)

                    statusText.text = "🔍 A procurar no Wikimedia..."
                    val videoUrl = procurarWikimedia(query)

                    statusText.text = "🎬 A descarregar vídeo..."
                    val clip = descarregarClip(videoUrl)

                    statusText.text = "✅ Vídeo pronto: ${clip.name}"
                    Toast.makeText(
                        this@MainActivity,
                        "Guardado: ${clip.absolutePath}",
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
        val p = prompt.lowercase()
        TRADUCOES.forEach { (pt, en) ->
            if (p.contains(pt)) return en
        }
        // Se não encontrar tradução usa o prompt original
        return p.split(" ").take(3).joinToString(" ")
    }

    private suspend fun procurarWikimedia(query: String): String =
        withContext(Dispatchers.IO) {
            // Wikimedia Commons API — sem API key, 100% grátis
            val url = "https://commons.wikimedia.org/w/api.php" +
                "?action=query" +
                "&generator=search" +
                "&gsrsearch=$query" +
                "&gsrnamespace=6" +
                "&gsrlimit=10" +
                "&prop=videoinfo" +
                "&viprop=url|mime" +
                "&format=json"

            val req = Request.Builder().url(url).build()
            val body = client.newCall(req).execute().body!!.string()
            val json = JSONObject(body)

            val pages = json
                .getJSONObject("query")
                .getJSONObject("pages")

            // Procura primeiro vídeo .webm ou .ogv
            val keys = pages.keys()
            while (keys.hasNext()) {
                val page = pages.getJSONObject(keys.next())
                val title = page.optString("title", "")
                if (title.endsWith(".webm", true) || title.endsWith(".ogv", true)) {
                    val videoinfo = page
                        .getJSONArray("videoinfo")
                        .getJSONObject(0)
                    val mime = videoinfo.optString("mime", "")
                    if (mime.startsWith("video/")) {
                        return@withContext videoinfo.getString("url")
                    }
                }
            }
            throw Exception("Nenhum vídeo encontrado para: $query")
        }

    private suspend fun descarregarClip(videoUrl: String): File =
        withContext(Dispatchers.IO) {
            val extensao = if (videoUrl.contains(".webm")) "webm" else "ogv"
            val dest = File(
                getExternalFilesDir(null),
                "neurovid_${System.currentTimeMillis()}.$extensao"
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
