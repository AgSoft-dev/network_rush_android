# Gson reflects on these DTOs (see GameResultRepositoryImpl); keep field names and generic signatures.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.triviamap.data.repository.GameResultRepositoryImpl$GeoPointDto { *; }
