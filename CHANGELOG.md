# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Nothing has been released or tagged yet — all work to date sits under
[Unreleased].

## [Unreleased]

### Added

- Translations are cached on the device, so re-scanning the same page or
  re-typing the same phrase no longer costs a translation API call. The cache
  holds 500 entries for 30 days and follows the history setting: with history
  turned off nothing is cached, and clearing history clears it too.
- Image translation now skips text blocks that have nothing to translate —
  page numbers, prices, rule lines, stray marks — and translates repeated
  blocks such as running headers only once. Skipped blocks render unchanged.
- Camera screen shows a capturing indicator while the camera is working.
- Image translation via OCR, with translated text overlaid on the photo.
- Translation history, saved locally and capped at 20 entries.
- Text-to-speech playback of translations.
- Settings for default language pair, instant translate, TTS, and font size.

### Changed

- Blocked-content message is clearer, and the Spanish profanity list is
  broader.

### Fixed

- Launcher icon no longer fails resource linking. The adaptive icon moved to
  `mipmap-anydpi-v26`, and API 24-25 — which cannot render `<adaptive-icon>` —
  now gets a legacy fallback drawable.

### Known issues

- **Yiddish pairs do not work.** Azure Translator has no Yiddish support, so
  the four Yiddish entries in the language picker return an API error.
- **Hebrew OCR does not build.** `com.google.mlkit:text-recognition-hebrew`
  is not a real artifact — ML Kit ships Latin, Chinese, Devanagari, Japanese
  and Korean script recognisers only, with no Hebrew script model.
- No Gradle wrapper is checked in, and the toolchain needs Gradle 8.x
  (AGP 8.1.0 does not load under Gradle 9).
