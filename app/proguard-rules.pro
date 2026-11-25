# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room entities, DAOs, and database
-keep class com.example.data.AppRuleEntity { *; }
-keep class com.example.data.AppRuleDao { *; }
-keep class com.example.data.ScreenDeckDatabase { *; }

# Accessibility service (referenced by manifest meta-data)
-keep class com.example.service.ForegroundAppAccessibilityService { *; }

# Broadcast receivers
-keep class com.example.service.BootCompletedReceiver { *; }
-keep class com.example.service.DeckActionReceiver { *; }

# Quick Settings tile services
-keep class com.example.tile.** { *; }
