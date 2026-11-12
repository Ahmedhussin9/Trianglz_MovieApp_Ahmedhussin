# 08: Theme and design system (T0.8)

> T0.7 (Room core) was merged into T1.3, so this doc is numbered 08 to keep the doc numbers matching the task IDs.

## 1. What was built
The visual foundation from PLAN.md § 5 (R32, R33):
- **"Cinema" color schemes** for light and dark mode: a warm amber primary, a deep teal tertiary, and cool slate neutrals. All 36 Material 3 color roles are defined in the theme layer only, and dynamic color is off.
- **Typography:** bundled **Outfit** for display, headline and title styles, and **Inter** for body and label styles, keeping the Material 3 type scale.
- **Shapes** (4/8/16/24/32dp corners) and a **spacing scale** (`Spacing.xs`–`xxl` on a 4dp grid).
- **Shared components:** `LoadingView`, `EmptyView`, `ErrorView` (which takes a `DomainError`), `RetryButton` and `RatingBadge`.
- **`@ThemePreviews`:** one annotation that renders every component in Light, Dark and RTL (Arabic).
- **Window theme:** the window background matches the Compose background in both modes, so no white flash appears on launch in dark mode.
- **Cleanup:** the template theme package, unused colors and the redundant manifest attributes are gone. The app name is now "Movie App" / "تطبيق الأفلام".

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Design system | `core/designsystem/theme/Color.kt` | New. `LightColors` and `DarkColors` with all roles. |
| Design system | `core/designsystem/theme/Type.kt` | New. Outfit and Inter font families, `MovieTypography`. |
| Design system | `core/designsystem/theme/Shape.kt` | New. `MovieShapes`. |
| Design system | `core/designsystem/theme/Spacing.kt` | New. The spacing scale. |
| Design system | `core/designsystem/theme/Theme.kt` | New. `MovieAppTheme`, replacing the template's `Trianglz_MovieApp_AhmedhussinTheme`. |
| Design system | `core/designsystem/preview/ThemePreviews.kt` | New. The Light/Dark/RTL multipreview. |
| Design system | `core/designsystem/component/StatusLayout.kt` | New (internal). The shared empty/error layout. |
| Design system | `core/designsystem/component/{LoadingView, EmptyView, ErrorView, RatingBadge}.kt` | New. `ErrorView.kt` also contains `RetryButton`. |
| Resources | `res/font/{outfit_medium, outfit_semibold, outfit_bold, inter_regular, inter_medium, inter_semibold}.ttf` | New. Official Google Fonts static TTFs. |
| Resources | `res/drawable/ic_{star, cloud_off, error_outline, movie, refresh}.xml` | New. Material icon vectors. |
| Resources | `values/strings.xml`, `values-ar/strings.xml` | App name, `loading`, `action_retry`, `rating_content_description`. |
| Resources | `values/colors.xml`, `values-night/colors.xml` | Only `window_background` (the template colors were removed). |
| Resources | `values/themes.xml`, `values-night/themes.xml` | `Theme.MovieApp` (light and dark window themes). |
| App | `AndroidManifest.xml` | Uses `Theme.MovieApp`. The redundant activity label and theme were removed. |
| App | `MainActivity.kt` | Uses `MovieAppTheme` with a temporary `EmptyView` until T0.9. |
| Removed | `ui/theme/{Color, Type, Theme}.kt` | Template theme. |
| Docs | `docs/licenses/OFL-{Inter,Outfit}.txt` | Font licenses (SIL Open Font License 1.1). |
| Tests | `test/.../core/designsystem/theme/ColorContrastTest.kt` | New. WCAG AA contrast for 15 role pairs per scheme. |

## 3. Data flow walkthrough
There's no data flow yet. This is how screens will use the design system:

```kotlin
when (val status = state.status) {
    Loading -> LoadingView()
    is Error -> ErrorView(error = status.error, onRetry = { onAction(Retry) })   // no mapping logic in the screen
    is Content -> MovieGrid(…)                                                   // RatingBadge(rating = movie.ratingText)
}
```

`ErrorView` chooses both the icon (a cloud-off icon for `NoInternet`/`Timeout`, an error icon for everything else) and the localized message (`DomainError.messageRes()` from T0.6). Every screen therefore shows errors the same way, with no logic of its own (R21).

## 4. Decisions made, and alternatives considered
- **Custom palette instead of dynamic color.** Material You would make the app look different on every device and would weaken the "polished, branded" requirement (R33). The palette uses M3 tonal conventions (tone 40 for light, tone 80 for dark), so components still look native. A theme switch is extra E1.
- **Contrast is tested, not assumed.** `ColorContrastTest` computes WCAG contrast ratios for 15 text/background pairs in each scheme and fails below 4.5:1, so a future color tweak can't quietly hurt readability.
- **Bundled static fonts instead of downloadable fonts.** Downloadable Google Fonts need Play Services and a network connection on first use, and would show fallback text until then. Static files work offline and on API 24–25, where variable-font weights aren't supported. Only the weights in use were added (three per family).
  - *Cost:* Inter adds about 1 MB (3 × 325 KB). Subsetting the fonts or turning on R8 resource shrinking for release builds could reduce this if size matters.
- **No Arabic font bundled.** Neither Outfit nor Inter has Arabic glyphs. Android falls back to its system Arabic font automatically, which is well tuned for Arabic text.
- **Vector drawables instead of `material-icons-extended`.** The extended icon library is very large and is no longer bundled with Material 3. Five small XML vectors cover current needs.
- **`ErrorView` takes a `DomainError`.** This couples the design system to the domain error type, but it keeps error presentation in one place and screens free of logic (R21). The type is pure Kotlin, so the layering stays clean.
- **`RatingBadge` takes formatted text.** Number formatting, such as one decimal or the locale's digits, belongs in the presentation mappers, which can be unit-tested. Accessibility: TalkBack reads "Rated 7.8 out of 10" instead of "star, 7.8".
- **Window background per mode.** The XML window theme matches the Compose background, which avoids a white flash when a dark-mode user launches the app.
- **App name:** "Movie App" / "تطبيق الأفلام" replaces the template's project name in the launcher.

## 5. How it is tested
- **`ColorContrastTest`** (2 tests): all 15 pairs pass at least 4.5:1 in both the light and dark schemes.
- **Previews:** `EmptyView`, `ErrorView`, `LoadingView` and `RatingBadge` each render in Light, Dark and RTL (Arabic) through `@ThemePreviews` (open them in Android Studio's Split/Design view).
- **Build checks:** `assembleDebug` passes, and 40 of 40 unit tests pass. `lintDebug` reports 0 errors and 2 warnings, both deliberate (targetSdk 36 and AGP 9.2.1). The earlier unused-resource and redundant-label warnings are gone.
- **On a device:** the launcher shows "Movie App" ("تطبيق الأفلام" in Arabic). The app opens on a themed placeholder (a movie icon in an amber-tinted circle, with the title in Outfit), and the window background matches the theme in light and dark mode.

## 6. How to build it from scratch
1. Download the static TTFs for the weights you need from Google Fonts' CSS API (`fonts.googleapis.com/css2?family=…`, which lists `fonts.gstatic.com` URLs), check that each file starts with `00 01 00 00`, and copy them to `res/font/` with lowercase names. Keep the OFL license files.
2. Define `LightColors` and `DarkColors` with `lightColorScheme(...)` and `darkColorScheme(...)`, filling every role, including the `surfaceContainer*` roles.
3. Build `FontFamily`s from `Font(R.font.x, weight)`, then copy the baseline `Typography()` styles with the new family and weight.
4. Define `Shapes` and a `Spacing` object.
5. Write `MovieAppTheme(darkTheme) { MaterialTheme(colorScheme, typography, shapes, content) }` without dynamic color.
6. Create a multipreview annotation with Light, Dark (`UI_MODE_NIGHT_YES`) and `locale = "ar"` previews.
7. Build the shared components with semantics (content descriptions, decorative icons with `null` descriptions), and add strings in both locales.
8. Set the XML window background to match the Compose background in `values/` and `values-night/`, and point the manifest at the new theme.
9. Write `ColorContrastTest`, and adjust colors until every pair passes.
