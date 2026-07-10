package com.google.android.diskusage;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.diskusage.ui.SelectActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Launches the real launcher Activity on the device to prove the app actually
 * starts (inflates layout, wires up the view model) without crashing on the
 * target platform.
 */
@RunWith(AndroidJUnit4.class)
public class LauncherSmokeTest {

    @Test
    public void launcherActivityStartsWithoutCrashing() {
        try (ActivityScenario<SelectActivity> scenario =
                     ActivityScenario.launch(SelectActivity.class)) {
            scenario.onActivity(activity -> {
                if (activity == null) {
                    throw new AssertionError("SelectActivity was not created");
                }
            });
        }
    }

    @Test
    public void launcherIntentResolvesToAnActivity() {
        Context ctx = getApplicationContext();
        Intent launch = ctx.getPackageManager()
                .getLaunchIntentForPackage(ctx.getPackageName());
        if (launch == null) {
            throw new AssertionError("No launch intent for package");
        }
        ResolveInfo info = ctx.getPackageManager()
                .resolveActivity(launch, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null) {
            throw new AssertionError("Launch intent does not resolve on this device");
        }
    }
}
