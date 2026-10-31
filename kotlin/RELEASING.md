# Releasing Net Result

Two apps ship together from this repo: the phone app (`:app`) and the Wear OS app (`:wear`). They share the
package name `org.comichao.netresult` and the same release key. Releases go to GitHub (APKs) and Google Play
(app bundles). Read this before touching a version number: Play never forgets a version code.

## Version name

`versionName` is `MAJOR.MINOR.PATCH` and is the same for the phone and the watch. It lives in
`app/build.gradle.kts`; `wear/build.gradle.kts` reads it from there. Change it with:

```bash
make bump PART=patch          # or minor, major, or an exact X.Y.Z
```

## Version code (the number Play sorts updates by)

A **plain counter**. `make bump` adds 1 to the phone's `versionCode` in `app/build.gradle.kts`, and that is all you
do. Do not encode dates in it.

| Build | Version code | Example |
|---|---|---|
| Phone (and the watch APK on GitHub) | the counter itself | `10214` |
| Watch bundle uploaded to Play | the counter plus 10000 | `20214` |

The watch offset is automatic: `wear/build.gradle.kts` adds 10000 when the bundle is built with `-PplayBundle`
(see Releasing, step 4). It exists because Play counts version codes across the whole app, so the phone and the
watch bundle cannot share one. The two ranges cannot meet until the counter reaches 10000 more releases from now.

Rules:

1. **A code can be used once, ever.** Play keeps every code of every upload, even if you discard the release
   or the bundle. Uploading a changed build with a code already used fails with "Version code N has already been
   used". If a rebuilt bundle needs uploading, the code must go up.
2. **Codes only go up.** A device is only offered an update whose code is higher than the installed one.
   Android also refuses to install an APK whose code is lower than the installed one.
3. **Keep the jump small.** Play's release check flags a code far above the previous one ("significantly higher")
   as an error and warns it will exhaust Play's limit of 2,100,000,000. Counting up by 1 never trips it. This is
   why we do not use dates or large offsets.
4. **Never reuse a code from the list below,** and add to the list whenever a code is spent without shipping.

### Where the codes live

- Phone: `versionCode` in `app/build.gradle.kts`.
- Watch on GitHub: the APK takes the phone's code, because `make publish` checks that every APK matches
  `app/build.gradle.kts`.
- Watch on Play: `playWearVersionCode` at the top of `wear/build.gradle.kts` (phone code + 10000).

### Codes already used in Play (never reuse)

`1`, `2`, `6`, `7`, `10213` (earlier builds), and the test uploads `26103101`, `1026103101`, `2026103101` and
`2026103151`. Also `10214` (phone, shipped as 0.2.5) and `20214` (the watch bundle of 0.2.5, which Play marked
"superseded" and never published, so the code is spent). Next free phone code: `10215` (watch `20215`).
Update this list after each Play upload.

### History: why it is a counter

The first Play uploads used `YYYYMMDDNN`-style codes (for example `2026103101`) so they would sort by date. Play's
release check rejected that scheme as "significantly higher than your previous version code", and the date
codes also sit close to Play's ceiling. They were replaced with the plain counter above, which starts at `10214`
because an early build already used `10213`. GitHub release `v0.2.4` was built with `2026103101`, so a phone with that
APK installed cannot be updated to a lower code. Reinstall from the newer APK after exporting your data
(Settings > Export data) if that ever matters.

## Releasing

Start with a clean, committed and pushed tree: `make publish` refuses to run otherwise.

1. `make bump PART=patch` (or minor, major). That moves the name and the code together.
2. Commit and push.
3. GitHub: from `kotlin/`, run `make publish`. It builds both signed APKs, checks name, code and signing key,
   and creates the `v<versionName>` release with `SHA256SUMS`. The signing variables come from `env.mk`.
4. Google Play: build the bundles. Run it through `make`, so the signing variables from `env.mk` are loaded:

   ```bash
   make --eval='bundle: ; $(GRADLE) :app:bundleRelease :wear:bundleRelease -PplayBundle -q' bundle
   ```

   The files are `app/build/outputs/bundle/release/netresult-<version>-release.aab` and
   `wear/build/outputs/bundle/release/netresult-wear-<version>-release.aab`. Copy them to
   `releases/play-upload-<version>/` (hidden from git) before uploading.
5. In Play Console, upload the phone bundle in **Closed testing - Alpha** and the watch bundle under
   **Wear OS > Closed testing - Alpha**. Add the release notes, then **Next** (Preview and confirm), start the
   rollout, and send both for review together from Publishing overview.

Play Console notes:

- A bundle Play has already seen cannot be uploaded again, even after you discard the release. Attach it with
  **Add from library** instead.
- A release can only be discarded while it is the latest one on its track. Discard the newer draft first.
- Saving a tester or country change only stages it. It is sent when you submit changes in Publishing overview.
- **Wear OS must be opted in**, once per app: Test and release > Advanced settings > Form factors > Wear OS must
  show all steps done, including "Opt-in to Wear OS and agree to the review policy". Until then, watch releases
  are accepted and even "published", but the Wear OS track stays Inactive and each release reads "Superseded by
  another release · Not published", with no rollout button. Opting in sends the watch through Google's Wear OS
  quality review. (Done on 2026-10-31.)
- The same bundle can be used on several tracks. Attach it with Add from library; it does not use a new code.

### Tracks in use

| Track | Form factor | Serves | Testers |
|---|---|---|---|
| Closed testing - Alpha | Phone | `10214` (0.2.5) | Google Groups `briscolai-testers`, `testcircle-testers` |
| Closed testing - Alpha | Wear OS | `20214` (0.2.5); `20215` (0.2.6) in review | same groups |
| Internal testing | Phone | `10214` (0.2.5); replaced `1` (0.0.3, target SDK 35) that caused the target API warning | internal testers |

Keep this table up to date after each Play release. Any old build left active on any track (internal included)
counts for Play's target API check.

Checks before pressing send in Play: the version name and code match this file, the bundle is signed with the
release key (`keytool -printcert -jarfile <aab>` shows SHA-256 starting `C9:0C:D6:D3`), and nothing from an
older draft is still in the track.

## Don'ts

- Don't plan a release by reusing a code, or by guessing: look at the list above and at Play's bundle explorer.
- Don't pick a version name, code scheme or upload target without agreeing it first.
- Don't put dates in the code or add big offsets; Play will flag the jump.
