# Nova Bank for iOS (TestFlight)

A native iOS app around the Nova Bank web app in [`../bank-app`](../bank-app),
built with [Capacitor](https://capacitorjs.com/). The web app is copied into the
Xcode project, so the app runs offline and opens like any other iPhone app.

- Bundle ID: `com.katyafedorova.novabank` (change it in Xcode if Apple says it is taken)
- iPhone only, portrait, iOS 14+
- `ITSAppUsesNonExemptEncryption = NO` is set, so TestFlight does not ask about export compliance

## 1. Once, on your Mac

1. Install **Xcode** from the Mac App Store and open it once so it finishes installing.
2. Install **Node.js LTS** from <https://nodejs.org>.
3. In Xcode, open **Xcode → Settings → Accounts**, click **+**, and sign in with your Apple Developer account.

## 2. Get the project and open it in Xcode

In **Terminal**:

```sh
git clone https://github.com/KatyaFedorova/last-train.git
cd last-train/bank-ios
npm install
npx cap sync ios
npx cap open ios
```

Xcode opens the **App** project. The first time, it downloads the Capacitor packages; wait until the progress bar at the top finishes.

## 3. Signing

1. In the left sidebar, click the blue **App** project, then the **App** target.
2. Open the **Signing & Capabilities** tab.
3. Tick **Automatically manage signing** and choose your team under **Team**.
4. If you see "bundle identifier is not available", change **Bundle Identifier** to something unique, for example `com.yourname.novabank`.

Optional check: plug in your iPhone, pick it at the top of the window, and press **▶** to run the app on it.

## 4. Create the app in App Store Connect

1. Go to <https://appstoreconnect.apple.com> → **Apps** → **+** → **New App**.
2. Fill in:
   - Platforms: **iOS**
   - Name: a unique name, e.g. **Nova Bank Demo** (testers see this; it must be unique on the App Store)
   - Primary language: Ukrainian or English
   - Bundle ID: pick the one from step 3 (if it is missing, run the app once from Xcode or wait a few minutes)
   - SKU: anything, e.g. `novabank001`
   - User access: **Full Access**
3. Click **Create**.

## 5. Upload a build

1. In Xcode, at the top, choose the device **Any iOS Device (arm64)**.
2. Menu **Product → Archive**. Wait for the build to finish; the **Organizer** window opens.
3. Select the new archive and click **Distribute App**.
4. Choose **TestFlight Internal Only** (or **App Store Connect**), then **Distribute**. Keep the default options.
5. Wait for "Upload succeeded".

Apple then processes the build for 5–30 minutes and emails you when it is ready.

## 6. Add yourself as a tester

1. In App Store Connect, open your app → **TestFlight** tab.
2. Under **Internal Testing**, click **+**, name the group (e.g. "Me"), and create it.
3. In the group, under **Testers**, click **+** and add yourself (your App Store Connect user).
4. Under **Builds**, add the processed build to the group.

Internal testers don't need Apple review.

## 7. Install on your iPhone

1. Install **TestFlight** from the App Store on your iPhone.
2. Open the invitation email from TestFlight on the iPhone and tap **View in TestFlight**, or open TestFlight and sign in with the same Apple ID.
3. Tap **Install** next to Nova Bank.

The app appears on your home screen with an orange dot next to its name, which marks TestFlight builds.

## Uploading a new version

After changing the web app in `../bank-app`:

```sh
cd last-train/bank-ios
npx cap sync ios
npx cap open ios
```

In Xcode, raise **Build** on the **General** tab (1 → 2 → 3…), then repeat step 5. TestFlight offers the update on your iPhone.
