package com.ifpr.androidapptemplatear.ui

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
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
import java.io.File
import java.io.IOException
import kotlin.math.sin

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var collectButton: ExtendedFloatingActionButton
    private lateinit var scoreText: TextView
    private lateinit var sharedViewModel: SharedModelViewModel

    private var currentModelFile = "hello_kitty.glb"
    private var modelReady = false
    private var score = 0
    private var toneGenerator: ToneGenerator? = null
    private var ambientTrack: AudioTrack? = null

    private val cameraPermissionCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)
        currentModelFile = findInitialSanrioModel()

        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
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
        collectButton.setOnClickListener { collectCurrentModel() }
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

    override fun onResume() {
        super.onResume()
        startAmbientSound()
    }

    override fun onPause() {
        stopAmbientSound()
        super.onPause()
    }

    override fun onDestroyView() {
        stopAmbientSound()
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

    private fun findInitialSanrioModel(): String {
        val files = requireContext().assets.list("")?.filter { it.endsWith(".glb", true) }.orEmpty()
        val preferred = listOf(
            "hello_kitty.glb", "hello_kitty (1).glb",
            "my_melody.glb", "kuromi.glb", "cinamon.glb", "pompompurin.glb"
        )
        return preferred.firstOrNull { it in files } ?: files.firstOrNull() ?: "hello_kitty.glb"
    }

    private fun loadModel(fileName: String) {
        val available = requireContext().assets.list("")?.toSet().orEmpty()
        if (fileName !in available) {
            modelReady = false
            collectButton.isEnabled = false
            Toast.makeText(
                requireContext(),
                "Modelo não encontrado: $fileName. Confira a pasta app/src/main/assets.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        currentModelFile = fileName
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

    private fun collectCurrentModel() {
        if (!modelReady) return
        sceneView.removeChild(modelNode)
        modelReady = false
        collectButton.isEnabled = false
        score++
        updateScore()
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)

        val nome = currentModelFile.substringBeforeLast(".")
            .replace(Regex(" \\(\\d+\\)$"), "")
            .replace("_", " ")
        Toast.makeText(requireContext(), "$nome coletado! +1 ponto", Toast.LENGTH_SHORT).show()
    }

    // Trilha ambiente suave sintetizada localmente, sem arquivo externo.
    private fun startAmbientSound() {
        if (ambientTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return

        val sampleRate = 22050
        val durationSeconds = 4
        val samples = ShortArray(sampleRate * durationSeconds)
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val fade = minOf(1.0, t / 0.5, (durationSeconds - t) / 0.5).coerceAtLeast(0.0)
            val wave = sin(2.0 * Math.PI * 220.0 * t) * 0.35 +
                sin(2.0 * Math.PI * 277.18 * t) * 0.25 +
                sin(2.0 * Math.PI * 329.63 * t) * 0.20
            samples[i] = (wave * fade * 900.0).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_GAME)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.setLoopPoints(0, samples.size, -1)
        track.setVolume(0.12f)
        track.play()
        ambientTrack = track
    }

    private fun stopAmbientSound() {
        ambientTrack?.let { track ->
            try {
                track.pause()
                track.flush()
                track.release()
            } catch (_: Exception) {
                // A faixa pode já ter sido liberada pelo sistema.
            }
        }
        ambientTrack = null
    }

    private fun updateScore() {
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
