# Play Console answer sheet: Data safety and related App content forms

Draft answers for Net Result (`org.comichao.netresult`, phone and Wear OS). They were written from the 0.2.2
manifests and code: no INTERNET permission, no advertising ID, no analytics or ads libraries, no account, no
location, contacts, camera or microphone. Re-check this sheet whenever a permission or dependency is added.

Google's definitions used below:
- **Collect** = data transmitted off the user's device to you or a third party.
- **Share** = data transferred to a third party.
- Data processed only on the device, or moved between the user's own phone and watch, is neither.

## Data safety form

### Data collection and security

| Question | Answer | Why |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | Nothing leaves the user's devices. The app has no INTERNET permission, so it cannot send anything to a server. |
| Is all of the user data collected by your app encrypted in transit? | _Not shown_ | It only appears when data is collected. If shown, answer Yes (the phone and watch link is encrypted by Google's Data Layer). |
| Do you provide a way for users to request that their data be deleted? | _Not shown_ | It only appears when data is collected. If shown, answer Yes: Settings > Reset all data, or uninstalling the app. |

With "No" on the first question, the form skips the per-data-type pages. Play then shows the store listing "No data
collected".

### If you want to be conservative

Some reviewers treat health data as always declarable, even when processed on the device. If Play asks you to
declare it, or you prefer to over-disclose, use these answers instead and mark every row **not shared**,
**processed on the device only**, **optional**:

| Data type | Category | Collected | Shared | Purpose | Optional |
|---|---|---|---|---|---|
| Heart rate | Health and fitness > Health info | Yes | No | App functionality | Yes (user can deny the sensor permission) |
| Calories burned, exercise sessions | Health and fitness > Fitness info | Yes | No | App functionality | Yes (Health Connect is opt-in) |
| Match scores, opponent names, notes | App activity > Other user-generated content | Yes | No | App functionality | No |

Use the same answers for the processing options: data is not sold, not used for advertising, not used for
profiling, and not shared with third parties. Keep the answer consistent with the privacy policy
(`docs/privacy.md`).

### Security practices

| Question | Answer |
|---|---|
| Data encrypted in transit | Yes if asked (nothing is sent over the internet; the phone and watch link uses the Wear OS Data Layer) |
| Users can request data deletion | Yes (Settings > Reset all data, or uninstall) |
| Committed to the Play Families policy | No |
| Independent security review | No |

## Other App content forms

| Form | Answer |
|---|---|
| Privacy policy | Public URL of `docs/privacy.md` (required, because the app handles health data) |
| Ads | **No**, the app contains no ads |
| App access | All functionality available without restrictions (no login) |
| Target audience | **18 and over** (or 13+). Not designed for children, so Families is off |
| News app | No |
| Government app | No |
| Financial features | None |
| COVID-19 / contact tracing | No |
| Advertising ID | **No**, the app does not use it (the merged manifest has no AD_ID permission) |
| Content rating (IARC) | Category: Reference, utility or sports tracker. No violence, sexual content, language, gambling or user-to-user communication. Expect **Everyone / PEGI 3** |

## Health apps declaration

| Question | Answer |
|---|---|
| Does the app provide health or fitness features? | Yes |
| Health features | **Activity and fitness tracking** (workouts and exercise sessions) |
| Medical or diagnostic claims | No. It is a sports score tracker and workout recorder, not a medical app |
| Uses Health Connect | Yes, **write only**: exercise sessions, heart rate, active calories burned |
| Uses Health Services on Wear OS | Yes, for the tennis exercise, heart rate and calories |

## Health Connect permissions declaration

Use the text in the chat history or write your own from this outline.

- **Permissions requested (phone):** `WRITE_EXERCISE`, `WRITE_HEART_RATE`, `WRITE_ACTIVE_CALORIES_BURNED`. No read permissions.
- **Why each is needed:** tennis session, heart-rate samples and active calories of the workout the user finished.
- **When it is used:** only after the user taps Allow in Settings > Health Connect, when a match or warm-up is saved.
- **Data handling:** not shared with third parties, not used for advertising, not sent to any server.
- **Revocation:** the user can revoke in Health Connect at any time.

## Sensor permissions (Wear OS)

| Permission | Where | Why |
|---|---|---|
| `BODY_SENSORS` | watch | Heart rate during a match or warm-up on Wear OS up to version 5 |
| `android.permission.health.READ_HEART_RATE` | watch | Replaces `BODY_SENSORS` on Wear OS 6 |
| `ACTIVITY_RECOGNITION` | watch | Required by Health Services to run the tennis exercise and compute calories |
| `WAKE_LOCK` | watch | Keeps the screen available while scoring |

## Things to double-check before submitting

- If you answer "No data collected", Play may still ask for the Health Connect declaration separately. That is
  expected: it is a different form.
- If a later version adds crash reporting, analytics, a login or a backup to a server, this sheet and the
  privacy policy both change.
- Play compares the answers with what the app does at review time. A mismatch between the Data safety
  form and the actual permissions is the usual reason for rejection.
