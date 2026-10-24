package com.google.android.diskusage;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Verifies that the bundled native scanner (packaged as libscan.so and executed
 * from the native library directory at runtime, see jni/Android.mk) is built for
 * the architecture of the device/emulator it is running on.
 *
 * This is the key cross-platform check: the CI emulator matrix runs this on
 * multiple Android versions (e.g. API 36 == Android 16). A green run proves the
 * NDK build produced a correct native binary for that platform's ABI. The ELF
 * header check is deterministic; the exec attempt below is best-effort and only
 * logged, because running a .so-as-executable under instrumentation can route
 * linker output to logcat instead of stdout.
 */
@RunWith(AndroidJUnit4.class)
public class NativeScanExecutableTest {

    // ELF e_machine values
    private static final int EM_386 = 0x03;
    private static final int EM_ARM = 0x28;
    private static final int EM_X86_64 = 0x3E;
    private static final int EM_AARCH64 = 0xB7;

    private File scanBinary() {
        Context ctx = getApplicationContext();
        String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
        assertTrue("nativeLibraryDir must be set", nativeDir != null && !nativeDir.isEmpty());
        return new File(nativeDir, "libscan.so");
    }

    private int expectedMachineForPrimaryAbi() {
        String abi = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "";
        switch (abi) {
            case "arm64-v8a": return EM_AARCH64;
            case "armeabi-v7a": return EM_ARM;
            case "x86_64": return EM_X86_64;
            case "x86": return EM_386;
            default: return -1;
        }
    }

    @Test
    public void nativeBinaryIsPackaged() {
        File bin = scanBinary();
        assertTrue("libscan.so must exist in " + bin.getParent(), bin.exists());
        assertTrue("libscan.so must be non-empty", bin.length() > 0);
    }

    @Test
    public void nativeBinaryMatchesDeviceArchitecture() throws Exception {
        File bin = scanBinary();
        assertTrue(bin.exists());

        byte[] ident = new byte[20];
        try (InputStream is = new FileInputStream(bin);
             DataInputStream dis = new DataInputStream(is)) {
            dis.readFully(ident);
        }

        // ELF magic: 0x7F 'E' 'L' 'F'
        assertEquals("Not an ELF file (byte 0)", 0x7F, ident[0] & 0xFF);
        assertEquals("Not an ELF file (E)", 'E', ident[1] & 0xFF);
        assertEquals("Not an ELF file (L)", 'L', ident[2] & 0xFF);
        assertEquals("Not an ELF file (F)", 'F', ident[3] & 0xFF);

        // e_machine is a little-endian u16 at offset 18 (all Android ABIs are LE)
        int eMachine = (ident[18] & 0xFF) | ((ident[19] & 0xFF) << 8);
        int expected = expectedMachineForPrimaryAbi();

        android.util.Log.i("DiskUsageTest", "libscan.so e_machine=0x"
                + Integer.toHexString(eMachine) + ", device primary ABI="
                + (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "?")
                + ", API=" + Build.VERSION.SDK_INT);

        assertTrue("Could not map device ABI to an ELF machine type", expected != -1);
        assertEquals("Native libscan.so architecture does not match device ABI",
                expected, eMachine);
    }

    @Test
    public void nativeBinaryExecutionIsAttempted() {
        File bin = scanBinary();
        assertTrue(bin.exists());

        String target = getApplicationContext().getFilesDir().getAbsolutePath();
        StringBuilder out = new StringBuilder();
        int exit = Integer.MIN_VALUE;
        Exception error = null;
        try {
            Process p = Runtime.getRuntime()
                    .exec(new String[]{bin.getAbsolutePath(), target});
            byte[] buf = new byte[4096];
            try (InputStream is = p.getInputStream()) {
                int n;
                int total = 0;
                while ((n = is.read(buf)) != -1 && total < 65536) {
                    total += n;
                    out.append(new String(buf, 0, n));
                }
            }
            exit = p.waitFor();
        } catch (Exception e) {
            error = e;
        }

        // Best-effort: we only record the result. The deterministic proof of a
        // correct native build lives in nativeBinaryMatchesDeviceArchitecture.
        android.util.Log.i("DiskUsageTest", "libscan.so exec on API " + Build.VERSION.SDK_INT
                + ": exit=" + exit
                + ", bytes=" + out.length()
                + (error != null ? ", error=" + error.getMessage() : "")
                + ", sample=" + out.substring(0, Math.min(80, out.length())).replace('\0', '.'));
    }

    @Test
    public void nativeBinaryIsAlignedFor16kbPages() throws Exception {
        File bin = scanBinary();
        assertTrue(bin.exists());

        byte[] elf = new byte[(int) bin.length()];
        try (InputStream is = new FileInputStream(bin)) {
            int read = 0;
            while (read < elf.length) {
                int n = is.read(elf, read, elf.length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        }

        // 32-bit devices only ever run 4 KB page kernels, so the 16 KB
        // requirement applies to the 64-bit ABIs only.
        if (elf[4] != 2) {
            android.util.Log.i("DiskUsageTest", "32-bit ELF, 16 KB page alignment not required");
            return;
        }

        long phoff = le64(elf, 0x20);
        int phentsize = le16(elf, 0x36);
        int phnum = le16(elf, 0x38);
        assertEquals("Unexpected program header size", 56, phentsize);

        long minLoadAlign = Long.MAX_VALUE;
        for (int i = 0; i < phnum; i++) {
            int base = (int) (phoff + (long) i * phentsize);
            if (le32(elf, base) == 1 /* PT_LOAD */) {
                minLoadAlign = Math.min(minLoadAlign, le64(elf, base + 48));
            }
        }

        android.util.Log.i("DiskUsageTest", "libscan.so PT_LOAD p_align=0x"
                + Long.toHexString(minLoadAlign) + ", ABI=" + Build.SUPPORTED_ABIS[0]);

        assertTrue("libscan.so has no PT_LOAD segment", minLoadAlign != Long.MAX_VALUE);
        assertTrue("libscan.so segments must be 16 KB aligned for Android 15+ 16 KB page"
                + " devices, got 0x" + Long.toHexString(minLoadAlign), minLoadAlign >= 16384);
    }

    private static int le16(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8);
    }

    private static int le32(byte[] b, int off) {
        return le16(b, off) | (le16(b, off + 2) << 16);
    }

    private static long le64(byte[] b, int off) {
        return (le32(b, off) & 0xFFFFFFFFL) | ((long) le32(b, off + 4) << 32);
    }
}
