# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\android-projeler\are-we-okay-app\android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and syntax by referring to
# http://proguard.sourceforge.net/manual/rules.html

# Keep Compose/Kotlin serialization classes if needed (already handled by gradle plugin usually)

# WorkManager's Room-generated WorkDatabase is instantiated via reflection at runtime
# (pulled in transitively by play-services-ads). Without these, R8 shrinking can strip
# or rename it, causing a startup crash in androidx.startup.InitializationProvider
# ("Failed to create an instance of androidx.work.impl.WorkDatabase") before the app's
# own code ever runs, so it can't be caught.
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.work.impl.WorkDatabase
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.work.**
