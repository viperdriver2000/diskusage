package com.google.android.diskusage;

import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.diskusage.ui.SelectActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * targetSdk 36 forces edge-to-edge and drops the opt-out, so the content has to
 * stay clear of the system bars on its own (see ui/common/EdgeToEdge.kt).
 *
 * Asserts the invariant rather than one implementation: the visible top of the
 * content root starts at or below the top system bar, and its visible bottom
 * ends at or above the bottom system bar. That holds both when the platform
 * insets the window (below API 35) and when the app pads the content itself.
 */
@RunWith(AndroidJUnit4.class)
public class EdgeToEdgeInsetsTest {

    @Test
    public void contentStaysClearOfSystemBars() throws Exception {
        try (ActivityScenario<SelectActivity> scenario =
                     ActivityScenario.launch(SelectActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();

            int[] measured = new int[4];
            for (int attempt = 0; attempt < 30; attempt++) {
                scenario.onActivity(activity -> {
                    View content = activity.findViewById(android.R.id.content);
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(content);
                    Insets bars = insets == null
                            ? Insets.NONE
                            : insets.getInsets(WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());

                    int[] location = new int[2];
                    content.getLocationInWindow(location);
                    int decorHeight = activity.getWindow().getDecorView().getHeight();

                    measured[0] = location[1] + content.getPaddingTop();                 // visible top
                    measured[1] = location[1] + content.getHeight()
                            - content.getPaddingBottom();                                // visible bottom
                    measured[2] = bars.top;                                              // required top
                    measured[3] = decorHeight - bars.bottom;                             // allowed bottom
                });
                if (measured[1] > 0) {
                    break;
                }
                Thread.sleep(100);
            }

            android.util.Log.i("DiskUsageTest", "content top=" + measured[0]
                    + " (bars.top=" + measured[2] + "), bottom=" + measured[1]
                    + " (decor-bottom=" + measured[3] + "), API="
                    + android.os.Build.VERSION.SDK_INT);

            assertTrue("Content starts under the top system bar (top=" + measured[0]
                    + " < " + measured[2] + ")", measured[0] >= measured[2] - 1);
            assertTrue("Content runs under the bottom system bar (bottom=" + measured[1]
                    + " > " + measured[3] + ")", measured[1] <= measured[3] + 1);
        }
    }
}
