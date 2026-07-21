package com.aromajoin.controllersdksample;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.annotation.Nullable;
import com.aromajoin.sdk.android.ble.AndroidBLEController;
import com.aromajoin.sdk.android.ble.ui.ASBaseActivity;
import com.aromajoin.sdk.core.device.AromaShooter;
import java.util.ArrayList;
import java.util.List;

/**
 * The screen where you can control AromaShooter BLE.
 */
public class BLEActivity extends ASBaseActivity {
  private final int DEFAULT_DURATION = 3000; // Unit: millisecond
  List<Integer> chambers = new ArrayList<>(); // chamber-to-shoot list
  private AndroidBLEController bleController;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ble);

    bleController = AndroidBLEController.getInstance();
  }

  private final int[] portIds = {
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
  private final Runnable shootTask = () -> {
    if (chambers.size() == 0) {
      return;
    }
    List<AromaShooter> aromaShooters = bleController.getConnectedDevices();
    if (aromaShooters == null
        || aromaShooters.size() == 0) { // check whether there is any connected devices.
      chambers.clear();  // Clear buffered chambers.
      return;
    }
    // Shoot scents from selected chambers of all connected devices.
    bleController.shootAllSimple(DEFAULT_DURATION, true, Utility.convertToIntArray(chambers));
    chambers.clear();
  };
}
