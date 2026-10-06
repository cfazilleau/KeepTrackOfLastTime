# Room and Compose ship their own consumer rules; add app-specific keep rules here if needed.

# Glance creates widget action callbacks by class name, through their no-argument constructor.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
