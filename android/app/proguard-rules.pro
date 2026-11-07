# Wault — ProGuard / R8 rules.
#
# Phase 1 ships with minification disabled (release buildType), so this file is
# intentionally minimal. Notes for later phases:
#
# Phase 2 (whatsmeow via gomobile): keep the generated JNI bridge classes.
# -keep class go.** { *; }
# -keep class com.wault.app.domain.engine.** { *; }
#
# Phase 7 enables R8 for release builds; keep rules for Hilt and Compose are
# already consumer rules shipped inside those libraries.
