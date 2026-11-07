# Add project specific ProGuard rules here.
-keepclassmembers class com.scluzlep.newmangps.data.model.** { <fields>; }
-keep class com.scluzlep.newmangps.data.model.** { *; }

# Room creates WorkManager's database by reflectively calling this no-arg constructor.
# R8 must retain it even though no direct bytecode reference exists.
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}

# WorkManager stores this class name for periodic work and recreates it reflectively.
-keep class com.scluzlep.newmangps.data.push.AlarmPollingWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

