package com.google.android.diskusage;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.diskusage.databinding.ProgressBinding;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The scan dialog shows the percentage and the current path on one row. When
 * the layout was converted to ConstraintLayout both views were centered with
 * wrap_content, so a long path slid underneath the percentage.
 *
 * Lays the real layout out at a dialog width and asserts the two texts stay in
 * their own horizontal slot.
 */
@RunWith(AndroidJUnit4.class)
public class ScanProgressLayoutTest {

    @Test
    public void percentAndPathDoNotOverlap() {
        Context ctx = ApplicationProvider.getApplicationContext();
        ProgressBinding binding = ProgressBinding.inflate(LayoutInflater.from(ctx));
        binding.progressPercent.setText("100%");
        binding.progressDetails.setText(
                "/storage/emulated/0/DCIM/Camera/2026/07/IMG_20260718_123456.jpg");

        int width = (int) (320 * ctx.getResources().getDisplayMetrics().density);
        View root = binding.getRoot();
        root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());

        View percent = binding.progressPercent;
        View details = binding.progressDetails;

        android.util.Log.i("DiskUsageTest", "percent=[" + percent.getLeft() + ".."
                + percent.getRight() + "] details=[" + details.getLeft() + ".."
                + details.getRight() + "] root=" + root.getWidth());

        assertTrue("Percentage must start left of the path (percent="
                        + percent.getLeft() + ", details=" + details.getLeft() + ")",
                percent.getLeft() < details.getLeft());
        assertTrue("Path must start right of the percentage (percent right="
                        + percent.getRight() + ", details left=" + details.getLeft() + ")",
                percent.getRight() <= details.getLeft());
        assertTrue("Path must not run past the dialog edge (details right="
                        + details.getRight() + ", root=" + root.getWidth() + ")",
                details.getRight() <= root.getWidth());
    }
}
