package com.google.android.diskusage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic environment sanity checks that run on the target device/emulator.
 * Confirms the app package is installed and reports the running Android API
 * level (e.g. 36 == Android 16), so a green run proves the APK deploys and
 * resolves on that platform.
 */
@RunWith(AndroidJUnit4.class)
public class AppContextTest {

    @Test
    public void packageNameIsCorrect() {
        Context ctx = ApplicationProvider.getApplicationContext();
        assertEquals("com.google.android.diskusage", ctx.getPackageName());
    }

    @Test
    public void appIsInstalledOnDevice() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info = pm.getPackageInfo(ctx.getPackageName(), 0);
        assertNotNull("PackageInfo must be resolvable on this device", info);
    }

    @Test
    public void runsOnSupportedApiLevel() {
        int api = Build.VERSION.SDK_INT;
        // App declares minSdk 21; the test simply records the level it runs on.
        assertTrue("Unexpectedly low API level: " + api, api >= 21);
        android.util.Log.i("DiskUsageTest", "Running instrumented tests on API level " + api
                + " (" + Build.VERSION.RELEASE + ")");
    }
}
