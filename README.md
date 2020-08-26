# Magisk Stub Finder

Find the Magisk stub in various ways

## Activity scanner

This scan for applications that have the sames activity names as the stub

In this example the strings are hardcoded but a more advanced version could  
download the stub online and update it's activity names automatically

The solution to fix this exploit is to change the activity id on repackages  
(With AppComponentFactory it shouldn't be hard)

## AppComponentFactory scanner

This scan for applications that have the sames AppComponentFactory name as the stub

Android allow getting this value with `packageInfo.applicationInfo.appComponentFactory`

The solution to fix this exploit is to change the AppComponentFactory id on repackages  
but since it's directly accessed bytecode modification on repackages will be required

## WTF GMS scanner

Scan if the APK has a gms meta but no `resources.arsc`

Google store it's metadata version in the android resource system  
For now this check only scan for the presence of `resources.arsc`  
but could be modified to check if it call the `@integer/google_play_services_version`  
of the `com.google.android.gms:play-services-basement` library (Core GMS lib)

The solution to fix this is to add a value named `google_play_services_version` and  
make the meta data `com.google.android.gms.version` call `google_play_services_version`  

Also the stub should always have the file `resources.arsc` since the play library  
use resource for the `com.google.android.gms.version` meta data and an absence of
`resources.arsc` could only mean the gms library is fake (Eg: MagiskStub, MagiskManager)

## Null byte name scanner

Scan if the APK label contains null byte (Aka `\0`, `\u0000`, `0x00`, `0`)

This is due to the repackage doing a hex patch instead of modifying the XML file properly

The solution to fix this is to change how the xml file is modified to allow changing  
the size of fields and resources instead of using hex patch
