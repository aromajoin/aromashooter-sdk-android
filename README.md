[English](#en) / [日本語](README-JP.md)

# Aroma Shooter SDK (Android)

**Version 3.1.0**

[![Maven Central](https://img.shields.io/maven-central/v/com.aromajoin.sdk/android?style=flat-square&label=Maven%20Central)](https://central.sonatype.com/artifact/com.aromajoin.sdk/android)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

An Android SDK for connecting to and controlling [Aroma Shooter devices](https://aromajoin.com/products/aroma-shooter) over **Bluetooth LE** and/or **USB**.

> **Important — the internal booster is required for scent to be emitted.** Enable it on every shoot: pass `internalBooster: true` (simple API) or `internalBoosterIntensity > 0` (intensity API). With the internal booster off, no scent comes out.

Your app picks the controller for the transport it needs and talks to that controller directly:

-   `AndroidBLEController.getInstance()` — for BLE devices, and it can hold several at once
-   `new AndroidUSBController(usbManager)` — for USB devices, one device at a time

Both expose the same shooting and stopping API; only connection setup differs.

---

<a id="en"></a>

## English

### Table of Contents

1. [Supported devices](#supported-devices)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Upgrading from 2.x](#upgrading-from-2x)
5. [Usage](#usage)
    - [0. Setup / discovery](#0-setup--discovery)
    - [1. Simple shooting API (AS1, AS2)](#1-simple-shooting-api-as1-as2)
    - [2. Intensity shooting API (AS2, AS3)](#2-intensity-shooting-api-as2-as3)
    - [3. Disconnecting / reconnecting](#3-disconnecting--reconnecting)
6. [Troubleshooting](#troubleshooting)
7. [License](#license)

---

<a id="supported-devices"></a>

### Supported devices

-   Aroma Shooter over Bluetooth LE or USB
    -   Simple shooting API: compatible with **AS1, AS2**
    -   Intensity shooting API: compatible with **AS2, AS3** (newer models)

---

<a id="prerequisites"></a>

### Prerequisites

-   Android 4.4 and later
-   BLE: a Bluetooth LE capable device. On Android 12 and later, the `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions are required.
-   USB: USB OTG support

---

<a id="installation"></a>

### Installation

```gradle
dependencies {
    implementation "com.aromajoin.sdk:core:3.1.0"
    implementation "com.aromajoin.sdk:android:3.1.0"
}
```

`core` and `android` share a version number from 3.1.0 onwards.

If you cannot use a dependency manager, the aar and jar are attached to the [latest release](https://github.com/aromajoin/aromashooter-sdk-android/releases/latest).

---

<a id="upgrading-from-2x"></a>

### Upgrading from 2.x

3.x renames a number of identifiers. Behaviour is unchanged, but calling code has to be updated.

| 2.x | 3.x |
|---|---|
| `diffuse*` | `shoot*` |
| `Port` | `AromaChamber` |
| `port.getPortNumber()` | `chamber.getNumber()` |
| `ports` parameter | `chambers` |
| `stopAllPorts()` | `stopAllChambers()` |

`stopAllChambersWithIntensity()` is new in 3.1.0, on both controllers. It stops a shoot started with the intensity API; `stopAllChambers()` sends the shorter command, which does not stop it.

---

<a id="usage"></a>

## Usage

For a complete app, see the [sample project](https://github.com/aromajoin/aromashooter-sdk-android/tree/main/sample).

<a id="0-setup--discovery"></a>

### 0. Setup / discovery

**Bluetooth LE.** There are three ways to get a connection screen:

1. Extend `ASBaseActivity`, which adds a bar button opening the default connection screen.
2. Open the default connection screen yourself:

```java
Intent intent = new Intent(YourCurrentActivity.this, ASConnectionActivity.class);
startActivity(intent);
```

3. Drive the API directly:

```java
AndroidBLEController controller = AndroidBLEController.getInstance();

controller.startScan(context, discoverCallback);
controller.connect(aromaShooter, connectCallback);
```

Stop scanning when the activity or fragment pauses:

```java
@Override
protected void onPause() {
    super.onPause();
    controller.stopScan(context);
}
```

**USB.** One device at a time.

```java
AndroidUSBController controller = new AndroidUSBController(usbManager);

controller.scan(discoverCallback);
controller.connect(aromaShooter, connectCallback);
```

Either way, the connected devices are:

```java
List<AromaShooter> aromaShooters = controller.getConnectedDevices();
```

---

<a id="1-simple-shooting-api-as1-as2"></a>

### 1. Simple shooting API (AS1, AS2)

Chambers are numbered 1 to 6. Duration is in milliseconds, capped at 10000.

```java
// Every connected device.
controller.shootAllSimple(3000, true, 2, 5);

// One device.
controller.shootSimple(aromaShooter, 3000, true, 2, 5);
```

Stop:

```java
controller.stopAllChambers();
controller.stopAllChambers(aromaShooter);
```

---

<a id="2-intensity-shooting-api-as2-as3"></a>

### 2. Intensity shooting API (AS2, AS3)

`AromaChamber` carries a chamber number and a concentration:

```java
public class AromaChamber {
    public int getNumber();        // 1..6
    public int getConcentration(); // 0..100
}
```

A booster or chamber given `0` is switched off, not left as it was.

```java
controller.shootAllWithIntensity(3000, 100, 0,
    new AromaChamber(2, 50), new AromaChamber(5, 100));

controller.shootWithIntensity(aromaShooter, 3000, 100, 0,
    new AromaChamber(2, 50));
```

Stop:

```java
controller.stopAllChambersWithIntensity();
controller.stopAllChambersWithIntensity(aromaShooter);
```

> The two APIs send different commands, and **each needs its own stop**. `stopAllChambers()` will not stop a shoot started with `shootWithIntensity*`.

---

<a id="3-disconnecting--reconnecting"></a>

### 3. Disconnecting / reconnecting

```java
controller.disconnect(aromaShooter, disconnectCallback);
controller.connect(aromaShooter, connectCallback);
```

For USB, disconnecting releases the USB connection; for BLE it closes the GATT connection so the device can be reached by another app or phone.

---

<a id="troubleshooting"></a>

## Troubleshooting

**No devices found over BLE on Android 12 or later.** `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` are runtime permissions and have to be granted before scanning, not just declared in the manifest.

**No devices found over USB.** USB OTG must be supported, and the user has to grant USB permission for the device. `AndroidUSBController` reports this through `onFailed` rather than throwing.

**Nothing comes out, but no error.** The internal booster is off. Pass `internalBooster: true`, or an `internalBoosterIntensity` above 0.

**The scent does not stop.** You are probably calling the stop that belongs to the other API. Use `stopAllChambersWithIntensity()` after `shootWithIntensity*`, and `stopAllChambers()` after `shootSimple*`.

**A booster keeps running after you asked for 0.** Fixed in 3.1.0. Earlier versions left a channel untouched when its intensity was 0, and the device carried on with its previous instruction.

---

<a id="license"></a>

## License

Please check the [LICENSE](/LICENSE.md) file for the details.

**If you get any issues or require any new features, please create a [new issue](https://github.com/aromajoin/aromashooter-sdk-android/issues).**
