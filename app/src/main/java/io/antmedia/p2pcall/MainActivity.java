package io.antmedia.p2pcall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.RendererCommon;
import org.webrtc.VideoTrack;

import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class MainActivity extends AppCompatActivity {
    private SurfaceViewRenderer remoteRenderer;
    private EditText serverUrlInput;
    private EditText streamIdInput;
    private TextView statusText;
    private Button joinButton;
    private IWebRTCClient webRTCClient;
    private String activeStreamId;
    private boolean joined;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                        boolean microphoneGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                        if (cameraGranted && microphoneGranted) {
                            joinPeer();
                        } else {
                            statusText.setText(R.string.status_permission_required);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        remoteRenderer = findViewById(R.id.remote_renderer);
        remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        remoteRenderer.setMirror(false);
        serverUrlInput = findViewById(R.id.server_url_input);
        streamIdInput = findViewById(R.id.stream_id_input);
        statusText = findViewById(R.id.status_text);
        joinButton = findViewById(R.id.join_button);
        findViewById(R.id.close_button).setOnClickListener(view -> finishAndRemoveTask());

        joinButton.setOnClickListener(view -> {
            if (joined) leavePeer(); else requestPermissionsOrJoin();
        });
    }

    private void requestPermissionsOrJoin() {
        boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
        if (cameraGranted && microphoneGranted) {
            joinPeer();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        }
    }

    private void joinPeer() {
        String serverUrl = serverUrlInput.getText().toString().trim();
        String streamId = streamIdInput.getText().toString().trim();
        if (serverUrl.isEmpty() || streamId.isEmpty()) {
            statusText.setText(R.string.error_empty_fields);
            return;
        }
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            statusText.setText(R.string.error_invalid_url);
            return;
        }

        setInputsEnabled(false);
        activeStreamId = streamId;
        webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setServerUrl(serverUrl)
                .addRemoteVideoRenderer(remoteRenderer)
                .setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                .setWebRTCListener(createListener())
                .setReconnectionEnabled(false)
                .build();
        statusText.setText(R.string.status_connecting);
        webRTCClient.join(activeStreamId);
    }

    @NonNull
    private DefaultWebRTCListener createListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onNewVideoTrack(VideoTrack track, String trackId) {
                super.onNewVideoTrack(track, trackId);
                remoteRenderer.post(() -> {
                    remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                    remoteRenderer.setEnableHardwareScaler(false);
                    remoteRenderer.setMirror(false);
                    remoteRenderer.requestLayout();
                });
            }

            @Override
            public void onJoinAttempt(String streamId) {
                super.onJoinAttempt(streamId);
                runOnUiThread(() -> statusText.setText(R.string.status_connecting));
            }

            @Override
            public void onJoined(String streamId) {
                super.onJoined(streamId);
                runOnUiThread(() -> {
                    joined = true;
                    statusText.setText(R.string.status_waiting);
                    joinButton.setText(R.string.leave_peer);
                });
            }

            @Override
            public void onIceConnected(String streamId) {
                super.onIceConnected(streamId);
                runOnUiThread(() -> statusText.setText(R.string.status_connected));
            }

            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                runOnUiThread(() -> statusText.setText(R.string.status_disconnected));
            }

            @Override
            public void onLeft(String streamId) {
                super.onLeft(streamId);
                runOnUiThread(() -> resetUi(R.string.status_left));
            }

            @Override
            public void onError(String description, String streamId) {
                super.onError(description, streamId);
                runOnUiThread(() -> {
                    statusText.setText(getString(R.string.error_prefix, description));
                    resetControls();
                });
            }
        };
    }

    private void leavePeer() {
        if (webRTCClient != null && activeStreamId != null) webRTCClient.stop(activeStreamId);
        resetUi(R.string.status_left);
    }

    private void resetUi(int statusResId) {
        statusText.setText(statusResId);
        resetControls();
    }

    private void resetControls() {
        joined = false;
        activeStreamId = null;
        webRTCClient = null;
        joinButton.setText(R.string.join_peer);
        setInputsEnabled(true);
    }

    private void setInputsEnabled(boolean enabled) {
        serverUrlInput.setEnabled(enabled);
        streamIdInput.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        if (webRTCClient != null && activeStreamId != null) webRTCClient.stop(activeStreamId);
        super.onDestroy();
    }
}
