package com.ifpr.androidapptemplatear.ui

import androidx.fragment.app.Fragment
import com.ifpr.androidapptemplatear.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Config
import com.ifpr.androidapptemplatear.MainActivity
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import okio.IOException
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var placeButton: ExtendedFloatingActionButton
    private lateinit var damageButton: ExtendedFloatingActionButton
    private lateinit var livesText: TextView

    private val CAMERA_PERMISSION_CODE = 1001
    private var currentModelFile: String = "neutrofilo.glb"

    // Estado da partida: três vidas e no mínimo um segundo entre danos.
    private var vidas = 3
    private var jogoEncerrado = false
    private var ultimoDano: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        sceneView = view.findViewById<ArSceneView>(R.id.sceneView).apply {
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        placeButton = view.findViewById(R.id.place)
        damageButton = view.findViewById(R.id.damage)
        livesText = view.findViewById(R.id.livesText)

        atualizarVidas()

        placeButton.setOnClickListener {
            if (!jogoEncerrado) placeModel()
        }

        // Controle de teste para validar a regra de derrota no emulador/celular.
        // Quando houver obstáculos reais, chame perderVida() no evento de colisão.
        damageButton.setOnClickListener {
            perderVida()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCameraPermission()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun placeModel() {
        if (jogoEncerrado) return
        modelNode.anchor()
        modelNode.isEditable = true
        modelNode.isScaleEditable = false
        sceneView.planeRenderer.isVisible = false
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initAR()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            initAR()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_LONG).show()
        }
    }

    private fun initAR() {
        if (!jogoEncerrado) loadModel(currentModelFile)
    }

    private fun loadModel(fileName: String) {
        if (jogoEncerrado) return
        try {
            sceneView.removeChild(modelNode)
        } catch (_: Exception) {
            // Ainda não existe modelo carregado.
        }

        modelNode = ArModelNode(sceneView.engine, PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = getFilePath(requireContext(), fileName),
                centerOrigin = Position(0f, 0f, 1f)
            ) {
                if (!jogoEncerrado) sceneView.planeRenderer.isVisible = true
            }
            isEditable = true
            isScaleEditable = false
        }
        sceneView.addChild(modelNode)
    }

    private fun perderVida() {
        if (jogoEncerrado) return

        val agora = SystemClock.elapsedRealtime()
        val anterior = ultimoDano
        if (anterior != null && agora - anterior < 1000L) return

        ultimoDano = agora
        vidas = (vidas - 1).coerceAtLeast(0)
        atualizarVidas()

        if (vidas == 0) {
            encerrarJogo("Suas vidas acabaram.")
        }
    }

    private fun atualizarVidas() {
        if (::livesText.isInitialized) {
            livesText.text = "Vidas: $vidas"
        }
    }

    private fun encerrarJogo(motivo: String) {
        if (jogoEncerrado) return
        jogoEncerrado = true

        // Bloqueia novas interações e oculta a cena AR enquanto a partida termina.
        placeButton.isEnabled = false
        damageButton.isEnabled = false
        sceneView.visibility = View.GONE

        val tela = Intent(requireContext(), GameOverActivity::class.java)
            .putExtra(GameOverActivity.EXTRA_MOTIVO, motivo)
        startActivity(tela)
        requireActivity().finish()
    }

    private fun getFilePath(context: Context?, fileName: String): String {
        try {
            context?.assets?.open(fileName)?.close()
            return fileName
        } catch (_: IOException) {
            val internalFile = File(context?.filesDir, fileName)
            return if (internalFile.exists()) {
                Uri.fromFile(internalFile).toString()
            } else {
                "Arquivo não encontrado em assets nem em filesDir: $fileName"
            }
        }
    }
}
