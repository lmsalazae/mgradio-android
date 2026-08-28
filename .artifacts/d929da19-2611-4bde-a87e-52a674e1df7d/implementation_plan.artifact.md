# Fix KSP Configuration Error

The project fails to build because KSP1 is no longer supported with the current Kotlin version (2.2.10). The build is explicitly disabled for KSP2 in `gradle.properties`, which causes a configuration error.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///home/mick/Projects/radio-android/gradle.properties)
- Change `ksp.useKSP2=false` to `ksp.useKSP2=true` (or remove it) to enable the required KSP2 engine.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify the build completes successfully.
