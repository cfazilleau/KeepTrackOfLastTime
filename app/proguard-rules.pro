# Room and Compose ship their own consumer rules; add app-specific keep rules here if needed.

# Glance creates widget action callbacks by class name, through their no-argument constructor.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Glance renders widgets in a WorkManager worker. WorkManager creates its input mergers and workers
# by reflection; without these, R8 strips their constructors and widgets stay on the loading spinner.
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
