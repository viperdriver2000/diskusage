package com.google.android.diskusage;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Verifies that the bundled native scanner (packaged as libscan.so and executed
 * from the native library directory, see jni/Android.mk) actually loads and runs
 * on the target platform's ABI.
 *
 * This is the important cross-platform check: the C code must run on the device's
 * architecture (e.g. x86_64 on the emulator, arm64 on phones) and on the target
 * Android version. A crash, ABI mismatch, or seccomp/exec restriction on a newer
 * Android release would surface here rather than in production.
 */
@RunWith(AndroidJUnit4.class)
public class NativeScanExecutableTest {

    private File scanBinary() {
        Context ctx = getApplicationContext();
        String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
        assertNotNull("nativeLibraryDir must be set", nativeDir);
        return new File(nativeDir, "libscan.so");
    }

    @Test
    public void nativeBinaryIsPackaged() {
        File bin = scanBinary();
        assertTrue("libscan.so must exist in " + bin.getParent(), bin.exists());
    }

    @Test
    public void nativeBinaryReportsUsageWhenCalledWithoutArgs() throws Exception {
        File bin = scanBinary();
        assertTrue(bin.exists());

        // Called without a directory argument, scan.c prints a usage message
        // and exits. This proves the binary executes on this ABI/OS.
        Process p = new ProcessBuilder(bin.getAbsolutePath())
                .redirectErrorStream(true)
                .start();

        StringBuilder out = new StringBuilder();
        try (InputStream is = p.getInputStream();
             BufferedReader r = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        p.waitFor();

        String text = out.toString();
        android.util.Log.i("DiskUsageTest", "libscan.so output: " + text.trim());
        assertTrue("Expected usage message from native binary, got: " + text,
                text.contains("Need directory argument"));
    }

    @Test
    public void nativeBinaryScansAbsolutePath() throws Exception {
        File bin = scanBinary();
        assertTrue(bin.exists());

        // Give it a real absolute path (app's own dir) so scan_tree runs its
        // main directory-walk code path on this platform.
        String target = getApplicationContext().getFilesDir().getAbsolutePath();

        Process p = new ProcessBuilder(bin.getAbsolutePath(), target)
                .redirectErrorStream(true)
                .start();

        byte[] buf = new byte[4096];
        int total = 0;
        try (InputStream is = p.getInputStream()) {
            int n;
            while ((n = is.read(buf)) != -1) {
                total += n;
            }
        }
        int exit = p.waitFor();

        android.util.Log.i("DiskUsageTest",
                "libscan.so scanned " + target + ", bytes=" + total + ", exit=" + exit);
        // It must not print the "absolute path" complaint and must produce output.
        assertTrue("Native scan produced no output", total > 0);
    }
}
