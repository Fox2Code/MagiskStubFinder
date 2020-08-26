package com.fox2code.stubfinder;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class AppScanner extends Thread {
    private final MainActivity mainActivity;
    private final PackageManager packageManager;
    private final Map<PackageInfo, String> labelCache;

    public AppScanner(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.packageManager = mainActivity.getPackageManager();
        this.labelCache = new IdentityHashMap<>();
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES|PackageManager.GET_META_DATA);

        List<PackageInfo> activities = findWithActivities(packageInfos);
        List<PackageInfo> factory = findWithAppComponentFactory(packageInfos);
        List<PackageInfo> wtf_gms = findWithWTF_GMS(packageInfos);
        List<PackageInfo> null_byte_name = findWithNullByteName(packageInfos);

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Activity scanner: (").append(activities.size()).append(")\n");
        this.appendResults(stringBuilder, activities);
        stringBuilder.append("AppComponentFactory scanner: (").append(factory.size()).append(")\n");
        this.appendResults(stringBuilder, factory);
        stringBuilder.append("WTF GMS scanner: (").append(wtf_gms.size()).append(")\n");
        this.appendResults(stringBuilder, wtf_gms);
        stringBuilder.append("Null byte name scanner: (").append(wtf_gms.size()).append(")\n");
        this.appendResults(stringBuilder, null_byte_name);
        stringBuilder.append("Scanned in ").append(System.currentTimeMillis() - start).append("ms");

        final String text = stringBuilder.toString();
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) mainActivity.findViewById(R.id.scanner_display)).setText(text);
            }
        });
    }

    private void appendResults(StringBuilder stringBuilder, List<PackageInfo> packageInfos) {
        for (PackageInfo packageInfo : packageInfos) {
            stringBuilder.append(packageInfo.packageName).append(" (")
                    .append(getLabel(packageInfo)).append(")\n");
        }
    }

    private String getLabel(PackageInfo packageInfo) {
        String label = labelCache.get(packageInfo);
        if (label == null) {
            label = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
            labelCache.put(packageInfo, label);
        }
        return label;
    }

    private List<PackageInfo> findWithActivities(List<PackageInfo> packageInfos) {
        List<PackageInfo> found = new LinkedList<>();
        for (PackageInfo packageInfo : packageInfos) {
            ActivityInfo[] activities = packageInfo.activities;
            if (activities != null && activities.length == 3 && "u2y.NDw".equals(activities[0].name)
                    && "piM.XPX".equals(activities[1].name)&& "i.ZeP".equals(activities[2].name)) {
                found.add(packageInfo);
            }
        }
        return found;
    }

    private List<PackageInfo> findWithAppComponentFactory(List<PackageInfo> packageInfos) {
        List<PackageInfo> found = new LinkedList<>();
        for (PackageInfo packageInfo : packageInfos) {
            if ("a.e".equals(packageInfo.applicationInfo.appComponentFactory)) {
                found.add(packageInfo);
            }
        }
        return found;
    }

    private List<PackageInfo> findWithWTF_GMS(List<PackageInfo> packageInfos) {
        List<PackageInfo> found = new LinkedList<>();
        for (PackageInfo packageInfo : packageInfos) {
            Bundle metaData = packageInfo.applicationInfo.metaData;
            if (metaData == null) continue;
            int gms = metaData.getInt("com.google.android.gms.version", -1);
            if (gms != -1) {
                File apk = getApkFromPackageInfo(packageInfo);
                if (apk == null) continue; // Skip split apks
                try(ZipFile zipFile = new ZipFile(apk)) {
                    if (zipFile.getEntry("resources.arsc") == null) {
                        found.add(packageInfo);
                    }
                } catch (IOException ignored) {}
            }
        }
        return found;
    }

    private static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";
    private List<PackageInfo> findWithNullByteName(List<PackageInfo> packageInfos) {
        List<PackageInfo> found = new LinkedList<>();
        for (PackageInfo packageInfo : packageInfos) {
            try {
                Resources resources =
                        packageManager.getResourcesForApplication(packageInfo.applicationInfo);
                XmlResourceParser manifestParser = resources.getAssets()
                        .openXmlResourceParser("AndroidManifest.xml");
                int event;
                while ((event = manifestParser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG &&
                            manifestParser.getName().equals("application")) {
                        String appName = manifestParser
                                .getAttributeValue(ANDROID_SCHEMA, "label");
                        if (appName != null && appName.endsWith("\0")) {
                            found.add(packageInfo);
                        }
                        break;
                    }
                    manifestParser.next();
                }
                manifestParser.close();
            } catch (Exception ignored) {}
        }
        return found;
    }

    private static File getApkFromPackageInfo(PackageInfo packageInfo) {
        String[] splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs;
        if (splitSourceDirs == null || splitSourceDirs.length == 0) {
            return new File(packageInfo.applicationInfo.sourceDir);
        } else {
            return null;
        }
    }
}
