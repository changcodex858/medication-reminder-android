# Room and Compose ship their own consumer rules. Keep the database implementation
# names stable so schema diagnostics stay readable in release builds.
-keep class * extends androidx.room.RoomDatabase { *; }

