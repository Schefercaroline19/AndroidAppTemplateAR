package com.ifpr.androidapptemplatear.ui

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.R
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.io.IOException
import java.io.File\nimport kotlin.math.sin

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var collectButton: ExtendedFloatingActionButton
    private lateinit var scoreText: TextView
    private lateinit var sharedViewModel: SharedModelViewModel

    private var currentModelFile = "hello_kitty.glb"
    private var ambientTrack: AudioTrack? = null
    private var modelReady = false
    private var score = 0
    private var toneGenerator: ToneGenerator? = null

    private val cameraPermissionCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        currentModelFile = findInitialSanrioModel()\n\n        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        placeButton = view.findViewById(R.id.place)
        collectButton = view.findViewById(R.id.collect)
        scoreText = view.findViewById(R.id.scoreText)

        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        updateScore()

        placeButton.setOnClickListener {
            if (modelReady) {
                modelNode.anchor()
                modelNode.isEditable = true
                modelNode.isScaleEditable = false
                sceneView.planeRenderer.isVisible = false
            } else {
                Toast.makeText(requireContext(), "Aguarde o carregamento do modelo.", Toast.LENGTH_SHORT).show()
            }
        }

        collectButton.isEnabled = false
        collectButton.setOnClickListener {
            collectCurrentModel()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedModelViewModel::class.java]
        sharedViewModel.selectedModel.observe(viewLifecycleOwner) { fileName ->
            currentModelFile = fileName
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                loadModel(fileName)
            }
        }

        checkCameraPermission()
    }

    override fun onDestroyView() {
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroyView()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                cameraPermissionCode
            )
        } else {
            loadModel(currentModelFile)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadModel(currentModelFile)
        } else if (requestCode == cameraPermissionCode) {
            Toast.makeText(requireContext(), "Permissão de câmera negada.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadModel(fileName: String) {
        modelReady = false
        collectButton.isEnabled = false

        if (::modelNode.isInitialized) {
            try {
                sceneView.removeChild(modelNode)
            } catch (_: Exception) {
                // O modelo anterior pode já ter sido removido.
            }
        }

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = getFilePath(requireContext(), fileName),
                centerOrigin = Position(0f, 0f, 1f)
            ) {
                modelReady = true
                collectButton.isEnabled = true
                sceneView.planeRenderer.isVisible = true
            }
            isEditable = true
            isScaleEditable = false
        }

        sceneView.addChild(modelNode)
    }

    /**
     * A coleta é uma ação do jogador: remove o objeto exibido,
     * soma um ponto e reproduz um efeito sonoro.
     */
    private fun collectCurrentModel() {
        if (!modelReady) return

        sceneView.removeChild(modelNode)
        modelReady = false
        collectButton.isEnabled = false

        score++
        updateScore()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)

        val nome = currentModelFile
            .removeSuffix(".glb")
            .replace("_", " ")
        Toast.makeText(requireContext(), "$nome coletado! +1 ponto", Toast.LENGTH_SHORT).show()
    }

    private fun findInitialSanrioModel(): String {\n        val files = requireContext().assets.list("")?.filter { it.endsWith(".glb", true) }.orEmpty()\n        val preferred = listOf("hello_kitty.glb", "hello_kitty (1).glb", "my_melody.glb", "kuromi.glb", "cinamon.glb", "pompompurin.glb")\n        return preferred.firstOrNull { it in files } ?: files.firstOrNull() ?: "hello_kitty.glb"\n    }\n\n    // Trilha ambiente suave sintetizada localmente; não exige arquivo de áudio externo.\n    private fun startAmbientSound() {\n        if (ambientTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return\n        val sampleRate = 22050\n        val durationSeconds = 4\n        val sampleCount = sampleRate * durationSeconds\n        val samples = ShortArray(sampleCount)\n        for (i in samples.indices) {\n            val t = i.toDouble() / sampleRate\n            val fade = minOf(1.0, t / 0.5, (durationSeconds - t) / 0.5).coerceAtLeast(0.0)\n            val wave = sin(2.0 * Math.PI * 220.0 * t) * 0.35 +\n                sin(2.0 * Math.PI * 277.18 * t) * 0.25 +\n                sin(2.0 * Math.PI * 329.63 * t) * 0.20\n            samples[i] = (wave * fade * 900.0).toInt().toShort()\n        }\n        val bufferBytes = samples.size * 2\n        val track = AudioTrack.Builder()\n            .setAudioAttributes(android.media.AudioAttributes.Builder()\n                .setUsage(android.media.AudioAttributes.USAGE_GAME)\n                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())\n            .setAudioFormat(AudioFormat.Builder()\n                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)\n                .setSampleRate(sampleRate)\n                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())\n            .setBufferSizeInBytes(bufferBytes)\n            .setTransferMode(AudioTrack.MODE_STATIC)\n            .build()\n        track.write(samples, 0, samples.size)\n        track.setLoopPoints(0, samples.size, -1)\n        track.setVolume(0.12f)\n        track.play()\n        ambientTrack = track\n    }\n\n    private fun stopAmbientSound() {\n        ambientTrack?.let { track ->\n            try {\n                track.pause()\n                track.flush()\n                track.release()\n            } catch (_: Exception) { }\n        }\n        ambientTrack = null\n    }\n\n    private fun updateScore() {
        scoreText.text = "Pontos: $score"
    }

    private fun getFilePath(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).close()
            fileName
        } catch (_: IOException) {
            val internalFile = File(context.filesDir, fileName)
            if (internalFile.exists()) {
                Uri.fromFile(internalFile).toString()
            } else {
                throw IllegalArgumentException("Modelo 3D não encontrado: $fileName")
            }
        }
    }
}
