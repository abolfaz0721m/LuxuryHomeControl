package com.example.wirelessmic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.wirelessmic.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AudioStreamViewModel by viewModels()

    private var service: AudioStreamService? = null
    private var isBound = false

    private val requiredPermissions: Array<String>
        get() = buildList {
            add(android.Manifest.permission.RECORD_AUDIO)
            add(android.Manifest.permission.MODIFY_AUDIO_SETTINGS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(android.Manifest.permission.BLUETOOTH_CONNECT)
                add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startAndBindService()
        } else {
            Toast.makeText(this, R.string.permission_rationale, Toast.LENGTH_LONG).show()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioStreamService.LocalBinder
            service = localBinder.getService()
            isBound = true
            viewModel.attachService(service)
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            viewModel.attachService(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupControls()
        ensurePermissionsThenStart()
    }

    private fun ensurePermissionsThenStart() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startAndBindService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startAndBindService() {
        val intent = Intent(this, AudioStreamService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun setupControls() {
        // Bluetooth connection itself is automatic (see BluetoothRouteHelper) —
        // this button just forces an immediate re-check, useful right after
        // turning on a headset/speaker that's already paired.
        binding.btnConnectBluetooth.setOnClickListener {
            service?.refreshBluetoothState()
        }

        binding.btnToggleStream.setOnClickListener {
            val currentlyStreaming = service?.uiState?.value?.isStreaming == true
            if (currentlyStreaming) {
                service?.stopStreaming()
            } else {
                val started = service?.startStreaming() ?: false
                if (!started) {
                    Toast.makeText(this, R.string.error_audio_init, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.switchNoiseSuppression.setOnCheckedChangeListener { _, checked ->
            viewModel.setNoiseSuppression(checked)
        }
        binding.switchAgc.setOnCheckedChangeListener { _, checked ->
            viewModel.setAgc(checked)
        }

        binding.seekGain.setOnSeekBarChangeListener(simpleSeekListener { viewModel.setManualGainFromSeek(it) })
        binding.seekBassBoost.setOnSeekBarChangeListener(simpleSeekListener { viewModel.setBassBoostFromSeek(it) })
        binding.seekLoudness.setOnSeekBarChangeListener(simpleSeekListener { viewModel.setLoudnessFromSeek(it) })
    }

    private fun simpleSeekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private fun observeServiceState() {
        val svc = service ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                svc.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: StreamUiState) {
        binding.tvBtStatus.text = if (state.isBluetoothConnected) {
            getString(R.string.status_bt_connected, state.connectedDeviceName ?: "?")
        } else {
            getString(R.string.status_bt_disconnected)
        }
        binding.btnToggleStream.text = if (state.isStreaming) {
            getString(R.string.btn_stop_stream)
        } else {
            getString(R.string.btn_start_stream)
        }
        binding.btnToggleStream.isEnabled = state.isBluetoothConnected

        state.errorMessage?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            service?.clearError()
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }
}
