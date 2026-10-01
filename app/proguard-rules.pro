# Gson reflects over these classes for Room TypeConverters and QR payloads.
# Keep field names so serialized JSON stays stable across releases.
-keep class com.joecode.brokemon.data.model.** { *; }
-keep class com.joecode.brokemon.share.QrPayload { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
