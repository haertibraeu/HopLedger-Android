# HopLedger Android

Android app for managing inventory and accounting at a micro-brewery. (Requires a running HopLedger-Backend)

> ⚠️ **Warning: Opinionated & Vibe-Coded**
>
> This project is **heavily opinionated**, a bit **over-engineered**, and **vibe-coded**. It is **not localized** and **not safe for production use** at all. Use at your own risk!

## Design Philosophy

HopLedger is **container-centric**: the physical container (bottle, keg) is the central unit, not the beer inside it. Prices (selling price, self-consumption price, deposit fee) are defined on the **container type**, not on the beer — because in a small brewery, the bottle format determines the price, not the recipe.

All actions (sell, self-consume, return) operate on containers. Accounting uses a **split-the-bills** model (like Splid/Splitwise), not traditional double-entry. There is **no local database** — all state lives on the backend; the app only stores the backend URL and API key.

See the [Backend README](https://github.com/haertibraeu/HopLedger-Backend#design-philosophy) for a more detailed explanation.

## Features

- 🍺 **Inventory** — Track bottles and kegs by type, beer, location and reservation status. Sell, self-consume, batch-fill and manage returns.
- 💰 **Finanzen** — Split-the-bills accounting (Splid/Splitwise-style). Tracks who paid what and shows who owes whom to break even. Supports manual entries with configurable categories.
- ⚙️ **Einstellungen** — Configure backend URL, brewers, beers, locations, container types and entry categories.

## Install

### Get it on Obtainium

[<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="54">](https://obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fadd%2Fhttps%3A%2F%2Fgithub.com%2Fhaertibraeu%2FHopLedger-Android)

Add this app in Obtainium by pointing it at:

```
https://github.com/haertibraeu/HopLedger-Android
```

### Manual download

Grab the latest `.apk` from the [Releases](https://github.com/haertibraeu/HopLedger-Android/releases) page and install it directly.

## Build

Requires Android Studio or JDK 17 + Android SDK.

```bash
./gradlew assembleDebug        # debug build
./gradlew assembleRelease      # release build
```

## CI / CD

Pushes to `main` and pull requests build a debug APK (artifact uploaded).  
Pushing a version tag (e.g. `v1.2.0`) triggers a **GitHub Release** with the APK attached automatically.

### Signing (optional)

For signed release builds, add these repository secrets:

|       Secret        |                         Description                         |
|---------------------|-------------------------------------------------------------|
| `KEYSTORE_BASE64`   | Base64-encoded `.jks` keystore (`base64 -w 0 keystore.jks`) |
| `KEYSTORE_PASSWORD` | Keystore password                                           |
| `KEY_ALIAS`         | Key alias                                                   |
| `KEY_PASSWORD`      | Key password                                                |

Without these secrets the pipeline produces an **unsigned** release APK which can still be installed manually.

## Backend

Requires a running [HopLedger-Backend](https://github.com/haertibraeu/HopLedger-Backend) instance. Configure the URL in the app's **Einstellungen** tab.
