package com.neurovid

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    // Vídeos CC0 públicos — sem API key, sem login
    private val VIDEO_DB = mapOf(
        "nature" to listOf(
            "https://download.samplelib.com/mp4/sample-5s.mp4",
            "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
        ),
        "ocean" to listOf(
            "https://download.samplelib.com/mp4/sample-10s.mp4"
        ),
        "city" to listOf(
            "https://download.samplelib.com/mp4/sample-5s.mp4"
        ),
        "forest" to listOf(
            "https://download.samplelib.com/mp4/sample-10s.mp4"
        ),
        "default" to listOf(
            "https://download.samplelib.com/mp4/sample-5s.mp4"
        )
    )

    private val TRADUCOES = mapOf(
        "floresta" to "forest", "praia" to "ocean",
        "cidade" to "city", "montanha" to "nature",
        "oceano" to "ocean", "pôr do sol" to "nature",
        "amanhecer" to "nature", "chuva" to "nature",
        "natureza" to "nature", "rio" to "nature",
        "pessoas" to "city", "estrada" to "city"
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
                    val categoria = detectarCategoria(prompt)

                    statusText.text = "🎬 A descarregar vídeo..."
                    val clip = descarregarClip(categoria)

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

    private fun detectarCategoria(prompt: String): String {
        val p = prompt.lowercase()
        TRADUCOES.forEach { (pt, en) ->
            if (p.contains(pt)) return en
        }
        return "default"
    }

    private suspend fun descarregarClip(categoria: String): File =
        withContext(Dispatchers.IO) {
            val lista = VIDEO_DB[categoria] ?: VIDEO_DB["default"]!!
            val url = lista.random()

            val dest = File(
                getExternalFilesDir(null),
                "neurovid_${System.currentTimeMillis()}.mp4"
            )
            URL(url).openStream().use { i ->
                dest.outputStream().use { o -> i.copyTo(o) }
            }
            dest
        }
}
