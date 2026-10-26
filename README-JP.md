[English](README.md) / [日本語](#jp)

# Aroma Shooter SDK (Android)

**Version 4.0.1**

[![Maven Central](https://img.shields.io/maven-central/v/com.aromajoin.sdk/android?style=flat-square&label=Maven%20Central)](https://central.sonatype.com/artifact/com.aromajoin.sdk/android)
[![License](https://img.shields.io/badge/license-PolyForm%20Noncommercial-4EB1BA.svg?style=flat-square)](LICENSE)

[Aroma Shooter](https://aromajoin.com/products/aroma-shooter)を**Bluetooth LE**および**USB**経由で接続・制御するためのAndroid向けSDKです。

> **重要 — 香りを噴射するには inner booster が必要です。** 噴射のたびに有効にしてください。シンプルAPIでは `innerBooster: true`、強度APIでは `innerBoosterIntensity > 0` を指定します。inner booster がオフの場合、香りは出ません。

利用する接続方式に応じてコントローラーを選び、そのコントローラーを直接呼び出します。

-   `AndroidBLEController.getInstance()` — BLEデバイス用。複数台を同時に保持できます
-   `new AndroidUSBController(usbManager)` — USBデバイス用。同時に1台のみ

噴射・停止のAPIは両者で共通です。異なるのは接続手順のみです。

---

<a id="jp"></a>

## 日本語

### 目次

1. [対応デバイス](#対応デバイス)
2. [前提条件](#前提条件)
3. [インストール](#インストール)
4. [2.xからの移行](#2xからの移行)
5. [使用法](#使用法)
    - [0. セットアップと検索](#0-セットアップと検索)
    - [1. シンプル噴射API (AS1, AS2)](#1-シンプル噴射api-as1-as2)
    - [2. 強度噴射API (AS2, AS3)](#2-強度噴射api-as2-as3)
    - [3. 切断と再接続](#3-切断と再接続)
6. [トラブルシューティング](#トラブルシューティング)
7. [ライセンス](#ライセンス)

---

<a id="対応デバイス"></a>

### 対応デバイス

-   Bluetooth LEまたはUSB接続のAroma Shooter
    -   シンプル噴射API：**AS1, AS2** に対応
    -   強度噴射API：**AS2, AS3**（新しいモデル）に対応

---

<a id="前提条件"></a>

### 前提条件

-   Android 4.4以降
-   BLE：Bluetooth LE対応端末。Android 12以降では `BLUETOOTH_SCAN` と `BLUETOOTH_CONNECT` の実行時パーミッションが必要です。
-   USB：USB OTG対応

---

<a id="インストール"></a>

### インストール

```gradle
dependencies {
    implementation "com.aromajoin.sdk:core:4.0.1"
    implementation "com.aromajoin.sdk:android:4.0.1"
}
```

3.1.0以降、`core` と `android` のバージョン番号は共通です。

依存関係管理ツールを使用できない場合は、[最新リリース](https://github.com/aromajoin/aromashooter-sdk-android/releases/latest)にaarとjarを添付しています。

---

<a id="2xからの移行"></a>

### 2.xからの移行

3.xでは識別子の名称が変更されています。動作は同じですが、呼び出し側の修正が必要です。

| 2.x | 3.x |
|---|---|
| `diffuse*` | `shoot*` |
| `Port` | `AromaChamber` |
| `port.getPortNumber()` | `chamber.getNumber()` |
| `ports` 引数 | `chambers` |
| `stopAllPorts()` | `stopAllChambers()` |

`stopAllChambersWithIntensity()` は3.1.0で両方のコントローラーに追加されました（それ以前はSDKから正しい強度用の停止フレームを送れませんでした）。`stopAllChambers()` との違いは送信するプロトコルフレームの長さ（21バイト/15バイト）だけで、**どちらを呼んでもデバイスは停止します**。どのAPIで開始した噴射でも止められます。

---

<a id="使用法"></a>

## 使用法

完全なアプリは[サンプルプロジェクト](https://github.com/aromajoin/aromashooter-sdk-android/tree/main/sample)を参照してください。

<a id="0-セットアップと検索"></a>

### 0. セットアップと検索

**Bluetooth LE。** 接続画面を用意する方法は3つあります。

1. `ASBaseActivity` を継承すると、デフォルトの接続画面を開くバーボタンが追加されます。
2. デフォルトの接続画面を自分で開く：

```java
Intent intent = new Intent(YourCurrentActivity.this, ASConnectionActivity.class);
startActivity(intent);
```

3. APIを直接使う：

```java
AndroidBLEController controller = AndroidBLEController.getInstance();

controller.startScan(context, discoverCallback);
controller.connect(aromaShooter, connectCallback);
```

Activity/Fragmentの停止時には、スキャンを止めることを忘れないでください。

```java
@Override
protected void onPause() {
    super.onPause();
    controller.stopScan(context);
}
```

**USB。** 同時に1台のみです。

```java
AndroidUSBController controller = new AndroidUSBController(usbManager);

controller.scan(discoverCallback);
controller.connect(aromaShooter, connectCallback);
```

いずれの場合も、接続中のデバイスは次で取得します。

```java
List<AromaShooter> aromaShooters = controller.getConnectedDevices();
```

---

<a id="1-シンプル噴射api-as1-as2"></a>

### 1. シンプル噴射API (AS1, AS2)

チャンバー番号は1～6です。噴射時間はミリ秒で、上限は10000です。

```java
// 接続中のすべてのデバイス。
controller.shootAllSimple(3000, true, 2, 5);

// 1台のみ。
controller.shootSimple(aromaShooter, 3000, true, 2, 5);
```

停止する：

```java
controller.stopAllChambers();
controller.stopAllChambers(aromaShooter);
```

---

<a id="2-強度噴射api-as2-as3"></a>

### 2. 強度噴射API (AS2, AS3)

`AromaChamber` はチャンバー番号と濃度を保持します。

```java
public class AromaChamber {
    public int getNumber();        // 1..6
    public int getConcentration(); // 0..100
}
```

ブースターやチャンバーに `0` を指定すると、そのまま維持されるのではなく**オフになります**。

```java
controller.shootAllWithIntensity(3000, 100, 0,
    new AromaChamber(2, 50), new AromaChamber(5, 100));

controller.shootWithIntensity(aromaShooter, 3000, 100, 0,
    new AromaChamber(2, 50));
```

停止する：

```java
controller.stopAllChambersWithIntensity();
controller.stopAllChambersWithIntensity(aromaShooter);
```

> `stopAllChambers()` と `stopAllChambersWithIntensity()` の違いはプロトコルフレームの長さ（15バイト/21バイト）だけです。どちらのAPIで開始した噴射も、**どちらの停止でも止まります**。

---

<a id="3-切断と再接続"></a>

### 3. 切断と再接続

```java
controller.disconnect(aromaShooter, disconnectCallback);
controller.connect(aromaShooter, connectCallback);
```

USBの場合は切断によりUSB接続を解放します。BLEの場合はGATT接続を閉じるため、他のアプリや端末から接続できるようになります。

---

<a id="トラブルシューティング"></a>

## トラブルシューティング

**Android 12以降でBLEデバイスが見つからない。** `BLUETOOTH_SCAN` と `BLUETOOTH_CONNECT` は実行時パーミッションです。マニフェストへの記載だけでなく、スキャン前に許可を取得する必要があります。

**USBデバイスが見つからない。** USB OTG対応が必要で、かつ当該デバイスへのUSB使用許可をユーザーが与える必要があります。`AndroidUSBController` はこれを例外ではなく `onFailed` で通知します。

**エラーは出ないが香りが出ない。** inner booster がオフになっています。`innerBooster: true`、または `innerBoosterIntensity` に0より大きい値を指定してください。

**噴射が止まらない。** 3.1.0へ更新してください。それ以前のバージョンでは強度用の停止フレームが不正で、`shootWithIntensity*` で開始した噴射が止まらないことがありました。3.1.0以降は `stopAllChambers()` / `stopAllChambersWithIntensity()` のどちらでも停止します。

**0を指定したのにブースターが動き続ける。** 3.1.0で修正されました。以前のバージョンでは強度0のチャンネルをコマンドに含めなかったため、デバイスが直前の指示をそのまま継続していました。

---

<a id="ライセンス"></a>

## ライセンス

非商用利用は [PolyForm Noncommercial License 1.0.0](LICENSE) のもとで無料です。Aroma Shooter® を使った自社製品・サービスの開発を含む商用利用には、Aromajoin のライセンスが必要です：contact@aromajoin.com。サンプルコードは [MIT-0](LICENSE-SAMPLES) です。4.0.0 までのバージョンは Apache-2.0 のままで、4.0.1 から適用されます。

### 商用ライセンス

この SDK を使って Aroma Shooter® 対応の製品・サービスを開発するには、商用ライセンス（月額制）が必要です。商用利用は、ライセンスのご購入を Aromajoin が確認した後にのみ認められます。お持ちの Aroma Shooter® を商用で使いたい場合もお問い合わせください。ご利用内容に合わせて条件をご案内します。会社名、製品・サービスの内容、対象プラットフォーム（Windows、Unity、iOS など）、想定台数を添えて contact@aromajoin.com までお問い合わせください。

**問題が発生したり、新機能が必要な場合は、[新しい問題](https://github.com/aromajoin/aromashooter-sdk-android/issues)を作成してください。**
