# 28: Error, validation and accessibility review (T4.1)

## 1. What was built
A device review of every failure the app can show, on every screen, plus the accessibility basics and a right-to-left pass (R21, R22, R32, R36, R37). It found four real defects, all now fixed:

1. **Long titles or large text pushed the details title up over the backdrop**, where dark text on a dark image is unreadable. The title now starts below the backdrop and grows downwards.
2. **Full-screen states (error, empty) could not scroll.** In landscape with large text the end of the message and the Retry button were cut off and unreachable. They now scroll when they do not fit, and stay centred when they do.
3. **In landscape, the search bar and the movie grid went under the side navigation bar.** Only the top and bottom insets were applied. Both now respect the horizontal safe-drawing insets.
4. **The details text (poster row and sections) had the same side-inset problem.** The backdrop still runs edge to edge, but text and poster now stay clear.

A regression test (`StatusLayoutTest`) now keeps defect 2 from coming back.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Design system | `core/designsystem/component/StatusLayout.kt` | Scrollable when content does not fit; still centred otherwise |
| Presentation | `feature/details/presentation/component/DetailsHeader.kt` | Title grows downwards from below the backdrop; horizontal insets |
| Presentation | `feature/details/presentation/DetailsScreen.kt` | Horizontal insets on the sections |
| Presentation | `feature/movies/presentation/MoviesScreen.kt` | Horizontal insets on the search bar and content |
| Tests | `androidTest/.../core/designsystem/component/StatusLayoutTest.kt` | New regression test |
| Docs | `docs/PLAN.md` | T4.1 ticked; tracker updated |

## 3. Review checklist and results
Device: Pixel 4 emulator, API 29, with a real TMDb token unless noted. ✅ means checked and correct, 🔧 means a defect was found and fixed (then re-checked), and 🧪 means covered by automated tests only.

### 3.1 Errors (R21)

| Error | How it was produced | List | Details | Search |
|---|---|---|---|---|
| **No internet**, nothing cached | Airplane mode after clearing app data | ✅ full error + Retry; Retry after reconnecting loads the list | ✅ full error + Retry, Back works | ✅ full error + Retry |
| **No internet**, cached data | Airplane mode with a filled cache | ✅ cached list + "Showing saved movies" notice (T1.5/T2.6 checks) | ✅ cached details + snackbar with Retry | n/a (search is not cached) |
| **Unauthorized (401)**, nothing cached | Build with an invalid token, app data cleared | ✅ full error "Couldn't connect… Check the TMDb access token." | n/a | ✅ same message + Retry |
| **Unauthorized (401)**, cached data | Same build, cache filled beforehand | ✅ cached list + notice (error icon) | ✅ cached details + snackbar with the token message | n/a |
| **Timeout** | Wi-Fi off, emulator mobile network delayed by 25 s (beyond the 15 s connect timeout) | 🧪 | ✅ "The server is taking too long to respond." + Retry; log shows `→ Timeout` | 🧪 |
| **Not found (404)** | Cannot be triggered from the UI (IDs come from TMDb lists) | 🧪 | 🧪 (`MovieDetailsRemoteDataSourceTest`, invalid-ID use-case test) | 🧪 |
| **Rate limited (429), Server (5xx), Unknown** | Cannot be triggered on demand against TMDb | 🧪 (`ErrorMapperTest` covers every status) | 🧪 | 🧪 |

The token swap left `local.properties` unchanged: it was backed up, restored byte for byte, and the backup deleted. Logs never contained the token.

### 3.2 Validation (R22)

| Check | Result |
|---|---|
| Invalid TMDb items (no ID or title, bad dates, out-of-range ratings, blank text, duplicate genres) | 🧪 mapper tests (T1.2, T2.2) |
| Search input (blank, extra spaces, over 100 characters) | ✅ on device (T3.4) + 🧪 validator tests |
| Repeated movies across search pages | 🧪 `SearchPagingSourceTest`; no crash while scrolling broad searches on device |

### 3.3 Accessibility

| Check | How | Result |
|---|---|---|
| Every tappable element has a label | UI hierarchy dump of list and details | ✅ cards read "Title, year, Rated X out of 10"; icon buttons "Settings", "Back", "Clear search"; text buttons by their text |
| Decorative images are silent | Code review | ✅ posters and backdrops have no description (the title names the movie) |
| Touch targets ≥ 48 dp | Bounds from the hierarchy dump | ✅ settings, back, clear, Read more and cards all ≥ 48 dp |
| Rating ring reads as one sentence | Hierarchy dump / `DetailsScreenTest` | ✅ "Rated 8.5 out of 10, 37,326 votes" |
| Large text (200 %), portrait | System font scale 2.0 | ✅ list, settings, search empty state. 🔧 Details title over backdrop (defect 1) |
| Large text (200 %), landscape | Font scale 2.0, rotated | 🔧 error/empty states cut off (defect 2), side insets (defects 3, 4) |

### 3.4 Right-to-left and Arabic

| Screen | Result |
|---|---|
| List, details, settings | ✅ checked in T2.6 (mirrored layout, Arabic content, consistent digits) |
| Search bar | ✅ icon on the right, clear on the left, mixed Latin text in an RTL field |
| Search empty state | ✅ "لا توجد نتائج لـ “zzqxw”" with the quotes in the right order |
| Search offline error | ✅ Arabic message, mirrored Retry |

### 3.5 Open item
- **Digit style with a per-app Arabic language on a physical Android 13+ phone set to English.** The emulator (API 29) shows consistent Arabic-Indic digits; the user's phone shows Latin digits. On the list these were consistent (no mixing) in the user's screenshot. The `MainActivity` locale sync (commit `8335e1d`) makes all formatting use the resource locale, but whether the details screen still mixes styles on that phone is not verified. It will be listed under known limitations in the README (T4.3) unless re-checked on the phone.

## 4. Decisions made, and alternatives considered
- **Title below the backdrop, not over it with a scrim.** A dark scrim behind the title would keep it readable over the image, but it would dim the backdrop and still fail for very long titles. Growing downwards works for any title length and text size.
- **The scrollable status layout keeps its centring.** `heightIn(min = maxHeight)` inside `verticalScroll` centres the content when it fits and scrolls only when it does not, so normal screens look the same as before.
- **Horizontal insets per element.** As the `edge-to-edge` skill recommends, insets are applied where content would collide (search bar, grid area, details text), not to a parent that would also shrink the full-width backdrop.
- **Timeout by network delay.** The emulator image has no root, so the hosts file could not point TMDb at an unreachable address. The emulator console's network delay only applies to its mobile network, so Wi-Fi was turned off for that check.
- **No automatic retry on reconnect.** After the network returns, the error screen waits for Retry. Watching connectivity is extra E11, kept out of the required scope (R41).

## 5. How it is tested
- **On the emulator:** everything in section 3 marked ✅ or 🔧.
- **Afterwards:** the emulator was restored to portrait, normal text size and System default language, with Wi-Fi and data back on.
- **Automated:** `StatusLayoutTest` reproduces defect 2 (a 320×160 dp box) and passes with the fix. The full instrumented suite (57 tests) and unit suite (181 tests) pass, and `assembleDebug` succeeds.

## 6. How to repeat this review
1. **No internet:**
   - clear the app data, turn on airplane mode and launch: check the list's full error;
   - reconnect and tap Retry;
   - open a cached movie and an uncached one offline;
   - search offline.
2. **Unauthorized:**
   - back up `local.properties`, set `TMDB_ACCESS_TOKEN` to an invalid value, and install;
   - check the list (cached and after clearing data), cached details and search;
   - restore the backup and reinstall.
3. **Timeout:**
   - turn off Wi-Fi and run `adb emu network delay 25000`;
   - open an uncached movie and wait about 20 s;
   - then run `adb emu network delay none`.
4. **Accessibility:**
   - dump the hierarchy (`adb shell uiautomator dump`) on each screen and check every clickable node has a label;
   - set `adb shell settings put system font_scale 2.0`, check every screen in portrait and landscape (`adb emu rotate`), then reset it to 1.0.
5. **Arabic:** switch to العربية in Settings and repeat the search, empty and error checks.
6. **Tests:** run `gradlew.bat connectedDebugAndroidTest` and `testDebugUnitTest`.
