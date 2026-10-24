package com.google.android.diskusage;

import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.diskusage.ui.SelectActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * targetSdk 36 forces edge-to-edge and drops the opt-out, so the activity
 * content has to stay clear of the system bars by applying their insets as
 * padding (see ui/common/EdgeToEdge.kt). This asserts the observable effect:
 * the content root is not laid out underneath the status/navigation bars.
 */
@RunWith(AndroidJUnit4.class)
public class EdgeToEdgeInsetsTest {

    @Test
    public void contentRootIsInsetFromSystemBars() throws Exception {
        try (ActivityScenario<SelectActivity> scenario =
                     ActivityScenario.launch(SelectActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            int[] padding = new int[2];
            for (int attempt = 0; attempt < 30; attempt++) {
                scenario.onActivity(activity -> {
                    View content = activity.findViewById(android.R.id.content);
                    padding[0] = content.getPaddingTop();
                    padding[1] = content.getPaddingBottom();
                });
                if (padding[0] > 0 && padding[1] > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            android.util.Log.i("DiskUsageTest", "content padding top=" + padding[0]
                    + " bottom=" + padding[1] + " (API " + android.os.Build.VERSION.SDK_INT + ")");

            assertTrue("Content must not start under the status bar (paddingTop="
                    + padding[0] + ")", padding[0] > 0);
            assertTrue("Content must not run under the navigation bar (paddingBottom="
                    + padding[1] + ")", padding[1] > 0);
        }
    }
}
