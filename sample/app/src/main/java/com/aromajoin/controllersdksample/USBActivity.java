package com.aromajoin.controllersdksample;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.aromajoin.sdk.android.usb.AndroidUSBController;
import com.aromajoin.sdk.core.callback.ConnectCallback;
import com.aromajoin.sdk.core.device.AromaShooter;
import java.util.ArrayList;
import java.util.List;

/**
 * The screen where you can control AromaShooter USB.
 */
public class USBActivity extends AppCompatActivity {
  private static final String TAG = USBActivity.class.getSimpleName();

  private final int DEFAULT_DURATION = 3000; // Unit: millisecond
  List<Integer> chambers = new ArrayList<>(); // chamber-to-shoot list

  private AndroidUSBController usbController;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_usb);

    usbController = new AndroidUSBController((UsbManager) getSystemService(Context.USB_SERVICE));
  }

  @Override
  protected void onResume() {
    super.onResume();
    scanAromaShooterUSB();
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Need to clean up resources used by AndroidUSBController
    usbController.cleanUp();
  }

  private void scanAromaShooterUSB() {
    usbController.scan(aromaShooters -> {
      if (aromaShooters != null && aromaShooters.size() > 0) {
        AromaShooter aromaShooter = aromaShooters.get(0);
        checkAndRequestUSBPermission(aromaShooter);
      }
    });
  }

  /**
   * Checks and requests USB device permission.
   * Firstly, check if permission is granted or not.
   * If not, request permission, otherwise, if true
   */
  private void checkAndRequestUSBPermission(AromaShooter aromaShooter) {
    if (usbController.hasPermission(aromaShooter)) {
      connectAromaShooterUSB(aromaShooter);
    } else {
      usbController.requestDevicePermission(USBActivity.this, aromaShooter);
    }
  }

  private void connectAromaShooterUSB(AromaShooter aromaShooter) {
    usbController.connect(aromaShooter, new ConnectCallback() {
      @Override
      public void onConnected(AromaShooter aromaShooter) {
        Log.d(TAG, "Connected to: " + aromaShooter.getSerial());
      }

      @Override
      public void onFailed(AromaShooter aromaShooter, String msg) {
        Log.d(TAG, "Failed to connect: " + aromaShooter.getSerial());
      }
    });
  }

  private int[] portIds = {
      R.id.button_port1, R.id.button_port2, R.id.button_port3, R.id.button_port4, R.id.button_port5,
      R.id.button_port6
  };

  /**
   * Gets chambers and trigger diffusing scents
   */
  public void onClick(View view) {
    int viewId = view.getId();
    for (int i = 0; i < portIds.length; i++) {
      if (viewId == portIds[i]) {
        chambers.add(i + 1);
      }
    }
    new Handler().postDelayed(shootTask, 10);
  }

  /**
   * Uses runnable to send command which allows shooting from multiple chambers at the same time.
   */
  private Runnable shootTask = () -> {
    if (chambers.size() == 0) {
      return;
    }
    List<AromaShooter> aromaShooters = usbController.getConnectedDevices();
    if (aromaShooters == null || aromaShooters.size() == 0) {
      chambers.clear();
      return;
    }

    usbController.shootAllSimple(DEFAULT_DURATION, true, Utility.convertToIntArray(chambers));
    chambers.clear();
  };
}
