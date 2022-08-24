package com.amazonivsreactnativebroadcast.IVSBroadcastCameraView;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class IVSBroadcastCameraView extends LinearLayout implements LifecycleEventListener {
  public static final String START_COMMAND_NAME = "START";
  public static final String STOP_COMMAND_NAME = "STOP";
  @Deprecated
  public static final String SWAP_CAMERA_COMMAND_NAME = "SWAP_CAMERA";

  public enum Events {
    ON_BROADCAST_ERROR("onBroadcastError"),
    ON_BROADCAST_STATE_CHANGED("onBroadcastStateChanged"),
    ON_BROADCAST_QUALITY_CHANGED("onBroadcastQualityChanged"),
    ON_NETWORK_HEALTH_CHANGED("onNetworkHealthChanged"),
    ON_BROADCAST_AUDIO_STATS("onBroadcastAudioStats"),
    ON_BROADCAST_SESSION_ID("onBroadcastSessionId"),
    ON_IS_BROADCAST_READY("onIsBroadcastReady"),
    ON_ERROR("onError");

    private String title;

    Events(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  private String STREAM_KEY;
  private String RTMPS_URL;
  private IVSBroadcastSessionService ivsBroadcastSession;

  private void addCameraPreview(@NonNull View preview) {
    preview.layout(0, 0, getRight(), getBottom());
    addView(preview);
  }

  private void onReceiveCameraPreviewHandler(View preview) {
    removeAllViews();
    addCameraPreview(preview);
  }

  private void sendEvent(String eventName, @Nullable WritableMap eventPayload) {
    ThemedReactContext reactContext = (ThemedReactContext) super.getContext();
    RCTEventEmitter eventEmitter = reactContext.getJSModule(RCTEventEmitter.class);

    eventEmitter.receiveEvent(getId(), eventName, eventPayload);
  }

  private void onBroadcastEventHandler(IVSBroadcastSessionService.Events event, @Nullable WritableMap eventPayload) {
    switch (event) {
      case ON_ERROR: {
        sendEvent(Events.ON_BROADCAST_ERROR.toString(), eventPayload);
        break;
      }
      case ON_STATE_CHANGED: {
        sendEvent(Events.ON_BROADCAST_STATE_CHANGED.toString(), eventPayload);
        break;
      }
      case ON_QUALITY_CHANGED: {
        sendEvent(Events.ON_BROADCAST_QUALITY_CHANGED.toString(), eventPayload);
        break;
      }
      case ON_NETWORK_HEALTH_CHANGED: {
        sendEvent(Events.ON_NETWORK_HEALTH_CHANGED.toString(), eventPayload);
        break;
      }
      case ON_AUDIO_STATS: {
        sendEvent(Events.ON_BROADCAST_AUDIO_STATS.toString(), eventPayload);
        break;
      }
      case ON_SESSION_ID: {
        sendEvent(Events.ON_BROADCAST_SESSION_ID.toString(), eventPayload);
        break;
      }
    }
  }

  private void sendErrorEvent(String errorMessage) {
    WritableMap eventPayload = Arguments.createMap();
    eventPayload.putString("message", errorMessage);

    sendEvent(Events.ON_ERROR.toString(), eventPayload);
  }

  private void initBroadcastSession() {
    if (ivsBroadcastSession.isInitialized()) return;

    try {
      ivsBroadcastSession.setEventHandler(this::onBroadcastEventHandler);
      ivsBroadcastSession.init();
      ivsBroadcastSession.getCameraPreviewAsync((cameraPreview) -> {
        onReceiveCameraPreviewHandler(cameraPreview);

        WritableMap eventPayload = Arguments.createMap();
        eventPayload.putBoolean("isReady", ivsBroadcastSession.isReady());
        sendEvent(Events.ON_IS_BROADCAST_READY.toString(), eventPayload);
      });
    } catch (RuntimeException error) {
      sendErrorEvent(error.toString());
    }
  }

  public IVSBroadcastCameraView(ThemedReactContext reactContext) {
    super(reactContext);
    reactContext.addLifecycleEventListener(this);
    ivsBroadcastSession = new IVSBroadcastSessionService(reactContext);
  }

  protected void start(ReadableArray args) {
    ReadableMap options = args.getMap(0);
    String rtmpsUrl = options.getString("rtmpsUrl");
    String streamKey = options.getString("streamKey");

    String finalRtmpsUrl = rtmpsUrl != null ? rtmpsUrl : RTMPS_URL;
    String finalStreamKey = streamKey != null ? streamKey : STREAM_KEY;

    if (finalRtmpsUrl == null) {
      sendErrorEvent("'rtmpsUrl' is empty.");
      return;
    }

    if (finalStreamKey == null) {
      sendErrorEvent("'streamKey' is empty.");
      return;
    }

    try {
      ivsBroadcastSession.start(finalRtmpsUrl, finalStreamKey);
    } catch (RuntimeException error) {
      sendErrorEvent(error.toString());
    }
  }

  protected void stop() {
    try {
      ivsBroadcastSession.stop();
    } catch (RuntimeException error) {
      sendErrorEvent(error.toString());
    }
  }

  @Deprecated
  protected void swapCamera() {
    try {
      ivsBroadcastSession.swapCamera(this::onReceiveCameraPreviewHandler);
    } catch (RuntimeException error) {
      sendErrorEvent(error.toString());
    }
  }

  protected void cleanUp() {
    removeAllViews();
    ivsBroadcastSession.deinit();
  }

  protected void setIsMuted(boolean isMuted) {
    ivsBroadcastSession.setIsMuted(isMuted);
  }

  protected void setIsCameraPreviewMirrored(boolean isCameraPreviewMirrored) {
    ivsBroadcastSession.setIsCameraPreviewMirrored(isCameraPreviewMirrored, this::onReceiveCameraPreviewHandler);
  }

  protected void setCameraPosition(String cameraPosition) {
    ivsBroadcastSession.setCameraPosition(cameraPosition, this::onReceiveCameraPreviewHandler);
  }

  protected void setCameraPreviewAspectMode(String cameraPreviewAspectMode) {
    ivsBroadcastSession.setCameraPreviewAspectMode(cameraPreviewAspectMode, this::onReceiveCameraPreviewHandler);
  }

  protected void setRtmpsUrl(String rtmpsUrl) {
    RTMPS_URL = rtmpsUrl;
  }

  protected void setStreamKey(String streamKey) {
    STREAM_KEY = streamKey;
  }

  protected void setSessionLogLevel(String sessionLogLevel) {
    ivsBroadcastSession.setSessionLogLevel(sessionLogLevel);
  }

  protected void setLogLevel(String logLevel) {
    ivsBroadcastSession.setLogLevel(logLevel);
  }

  protected void setConfigurationPreset(String configurationPreset) {
    ivsBroadcastSession.setConfigurationPreset(configurationPreset);
  }

  protected void setVideoConfig(ReadableMap videoConfig) {
    ivsBroadcastSession.setVideoConfig(videoConfig);
  }

  protected void setAudioConfig(ReadableMap audioConfig) {
    ivsBroadcastSession.setAudioConfig(audioConfig);
  }

  @Override
  public void onHostResume() {
  }

  @Override
  public void onHostPause() {
  }

  @Override
  public void onHostDestroy() {
    cleanUp();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    initBroadcastSession();
  }
}
