# R8 rules for 2048 Elemental Sprites.
# Most libraries (Compose, Firebase, Play services, Credential Manager, Room, WorkManager) ship their own
# consumer rules; these cover the places where the app itself relies on reflection.

# Readable crash reports in Crashlytics (the mapping file is uploaded by the Crashlytics plugin).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Gson needs generic signatures for TypeToken and the field names of the classes it reads and writes.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.avfusionapps.game_2048.model.TimeAttackScore { *; }

# Firestore maps documents onto this model through its no-arg constructor, getters and setters.
-keep class com.avfusionapps.game_2048.data.model.** { *; }

# Credential Manager loads the Play services provider by class name.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
