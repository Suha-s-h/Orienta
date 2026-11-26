# Security & Privacy Policy

## Privacy Guarantee

**Orienta** is designed from the ground up as a local, offline-only Android utility:

- **Zero Network Access:** Orienta does **not** request `android.permission.INTERNET` or `ACCESS_NETWORK_STATE`. It cannot send data anywhere.
- **Zero Analytics or Ads:** No third-party crash trackers, telemetry SDKs, or ad libraries are included.
- **Local-Only Persistence:** Your per-app rotation rules and preferences stay strictly on-device inside Room (`ScreenDeckDatabase`) and Preferences DataStore (`DeckPreferencesStore`).
- **Accessibility Service Scope:** `ForegroundAppAccessibilityService` listens solely to `TYPE_WINDOW_STATE_CHANGED` / `TYPE_WINDOWS_CHANGED` events to detect which app is in the foreground (for per-app rules) and whether a soft keyboard is visible (for smart keyboard yield). It never reads typed passwords, screen text, notifications, or personal content.

---

## Supported Versions

Security and compatibility patches are applied to the latest release on `main`.

| Version | Supported |
| ------- | --------- |
| `1.x` (`main`) | Yes |

---

## Reporting a Vulnerability

If you discover a security vulnerability—such as unintended intent exposure, exported component hijacking, or overlay privilege abuse—please report it responsibly:

1. **Do not** open a public GitHub Issue with exploit details.
2. Open a **GitHub Security Advisory** via the repository's **Security -> Advisories -> Report a vulnerability** tab (or contact the maintainer directly via GitHub profile email).
3. Include affected Android API levels, reproduction steps, and potential impact.

We aim to acknowledge reports promptly and release a patch as soon as possible.
