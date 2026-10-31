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

Format: **`YYYYMMDDNN`**, the release date followed by a two-digit counter. Compared as numbers it sorts from
oldest to newest, which is what Play needs. It stays below Play's limit of 2,100,000,000 until the year 2099.

| Build | Counter | Example (31 Oct 2026) |
|---|---|---|
| Phone | `01`-`49`, first release that day is `01` | `2026103101` |
| Watch, uploaded to Play | `51`-`99`, first release that day is `51` | `2026103151` |

Rules:

1. **A code can be used once, ever.** Play keeps every code of every upload, even if you discard the draft or the
   bundle. Re-uploading changed code with the same code fails with "Version code N has already been used".
2. **Codes only go up.** A device is only offered an update whose code is higher than the installed one.
3. **Phone and watch must differ.** Play counts codes across the whole app, so one code cannot be used by both
   form factors. This is why the watch has its own counter range (51-99).
4. **The date is the release date,** not the commit date. If you release twice in a day, use `02`, `03`, ...
   If a release is rebuilt after an upload to Play, the code must go up even though the version name stays.
5. **Never use a code that is lower than one already released.** Codes `10213` and `2` are old test builds still
   in Play; everything today is far above them.

### Where the codes live

- Phone: `versionCode` in `app/build.gradle.kts`. `make bump` adds 1 to it, so **after every bump set it by
  hand** to today's date plus the counter.
- Watch for GitHub: the APK takes the phone's code, because `make publish` checks that every APK matches
  `app/build.gradle.kts`.
- Watch for Play: `playWearVersionCode` at the top of `wear/build.gradle.kts`. Change it by hand whenever you
  upload a new watch bundle, to the same date and a counter from 51 up.

### Codes already used (never reuse)

`2`, `5`, `6`, `7`, `10213` (earlier builds); `26103101` and `1026103101` (first Play test uploads, replaced by the
scheme above). Add to this list when a code is spent without shipping.

## Releasing

Start with a clean, committed and pushed tree: `make publish` refuses to run otherwise.

1. Bump the name, then set the phone code and, if you upload the watch to Play, `playWearVersionCode`.
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
   **Wear OS > Closed testing - Alpha**. Add the release notes, then send both for review together.

Checks before pressing send in Play: the version name and code match this file, the bundle is signed with the
release key (`keytool -printcert -jarfile <aab>` shows SHA-256 starting `C9:0C:D6:D3`), and nothing from an
older draft is still in the track.

## Don'ts

- Don't plan a release by reusing a code, or by guessing: look at the table above and at Play's bundle explorer.
- Don't pick a version name, code scheme or upload target without agreeing it first.
- Don't move the watch to a different range without updating the table and rule 3.
