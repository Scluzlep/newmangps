# Tree-shake unused library code/resources ONLY — never rename symbols or apply aggressive optimizations.
-dontobfuscate
-dontoptimize
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep 100% of application classes, models, Compose UI, and reflection entry points intact.
-keep class com.scluzlep.newmangps.** { *; }

# Keep reflection-heavy library internals intact.
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class top.yukonga.miuix.** { *; }
