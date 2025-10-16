/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2023 Michael Vorburger
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ch.vorburger.exec;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

/**
 * Tests ManagedProcess.
 *
 * @author Michael Vorburger
 */
class ManagedProcessTest {

    @Test
    void onProcessCompleteInvokedOnCustomListener() throws IOException {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingExec(listener);
        exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        assertNotEquals(Integer.MIN_VALUE, listener.expectedExitValue);
        assertEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNull(listener.t);
    }

    @Test
    void onProcessFailedInvokedOnCustomListenerTraditional() throws ManagedProcessException {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingFailingExec(listener, false);
        assertThrows(
                ManagedProcessException.class,
                () -> exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000));
        assertEquals(Integer.MIN_VALUE, listener.expectedExitValue);
        assertNotEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNotNull(listener.t);
    }

    @Test
    void onProcessFailedInvokedOnCustomListenerWithExitValueChecker() throws IOException {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingFailingExec(listener, true);
        exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS) {
            assertEquals(1, listener.expectedExitValue);
        } else { // assumes linux
            assertEquals(2, listener.expectedExitValue);
        }
        assertEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNull(listener.t);
    }

    @SuppressWarnings("SimplifiableAssertion")
    @Test
    void basics() throws ManagedProcessException {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertEquals(false, p.isAlive());
        assertThrows(ManagedProcessException.class, p::destroy);
        assertThrows(ManagedProcessException.class, p::exitValue);
        // Commented out because this is a flaky not reliable test, because it's thread scheduling
        // timing dependent :( see long comment inside start() impl. for why this is so
        // assertThrows(ManagedProcessException.class, p::start);
        assertThrows(ManagedProcessException.class, p::waitForExit);
        assertThrows(ManagedProcessException.class, () -> p.waitForExitMaxMs(1234));
        assertThrows(ManagedProcessException.class, () -> p.waitForExitMaxMsOrDestroy(1234));
    }

    @Test
    void waitForMustFailIfNeverStarted() throws ManagedProcessException {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertThrows(ManagedProcessException.class, p::waitForExit);
    }

    @Test
    void startForMustFailForWrongExecutable() throws ManagedProcessException {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertThrows(ManagedProcessException.class, p::start);
    }

    @Test
    void waitForSeenMessageIfAlreadyTerminated() throws IOException {
        SomeSelfTerminatingExec exec = someSelfTerminatingExec();
        ManagedProcess p = exec.proc;
        // this process should have terminated itself faster than in 1s (1000ms),
        // but this should not cause this to hang, but must return silently:
        p.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
    }

    @Test
    void waitForWrongMessageIfAlreadyTerminated() throws ManagedProcessException {
        ManagedProcess p = someSelfTerminatingExec().proc;
        // this process should have terminated itself faster than in 1s (1000ms),
        // but this should not cause this to hang, but must throw an ManagedProcessException
        assertThrows(
                ManagedProcessException.class,
                () -> p.startAndWaitForConsoleMessageMaxMs("...", 1000));
    }

    @Test
    void selfTerminatingExec() throws IOException {
        SomeSelfTerminatingExec exec = someSelfTerminatingExec();
        ManagedProcess p = exec.proc;

        assertFalse(p.isAlive());
        p.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        // NB: We can't assertThat(p.isAlive()).isTrue(); - if p finishes too fast, that fails!

        p.waitForExit();
        p.exitValue(); // just making sure it works, don't check, as Win/NIX diff.
        assertFalse(p.isAlive());

        // It's NOT OK to call destroy() on a process which already terminated
        assertThrows(ManagedProcessException.class, p::destroy);

        String recentConsoleOutput = p.getConsole();
        assertTrue(recentConsoleOutput.length() > 10);
        assertTrue(recentConsoleOutput.contains("\n"));
        // System.out.println("Recent (default) 50 lines of console output:");
        // System.out.println(recentConsoleOutput);
    }

    static class SomeSelfTerminatingExec {
        ManagedProcess proc;
        String msgToWaitFor;
    }

    protected SomeSelfTerminatingExec someSelfTerminatingExec() throws ManagedProcessException {
        return someSelfTerminatingExec(null);
    }

    protected SomeSelfTerminatingExec someSelfTerminatingExec(
            @Nullable ManagedProcessListener listener) throws ManagedProcessException {
        SomeSelfTerminatingExec r = new SomeSelfTerminatingExec();
        if (SystemUtils.IS_OS_WINDOWS) {
            r.proc =
                    new ManagedProcessBuilder("cmd.exe")
                            .addArgument("/C")
                            .addArgument("dir")
                            .addArgument("/X")
                            .setProcessListener(listener)
                            .build();
            r.msgToWaitFor = "bytes free";
        } else if (SystemUtils.IS_OS_SOLARIS) {
            r.proc =
                    new ManagedProcessBuilder("true")
                            .addArgument("--version")
                            .setProcessListener(listener)
                            .build();
            r.msgToWaitFor = "true (GNU coreutils)";
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            r.proc =
                    new ManagedProcessBuilder("echo")
                            .addArgument(
                                    "\"Lorem ipsum dolor sit amet, consectetur adipisci elit, "
                                            + " sed eiusmod tempor incidunt ut \nlabore et dolore magna aliqua.\"")
                            .setProcessListener(listener)
                            .build();
            r.msgToWaitFor = "incidunt";
        } else {
            throw new ManagedProcessException("Unexpected Platform, improve the test dude...");
        }

        return r;
    }

    protected SomeSelfTerminatingExec someSelfTerminatingFailingExec(
            ManagedProcessListener listener, boolean setExitValueChecker)
            throws ManagedProcessException {
        ManagedProcessBuilder builder;
        SomeSelfTerminatingExec r = new SomeSelfTerminatingExec();
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            builder = new ManagedProcessBuilder("ls").addArgument("-4");
            // ls (GNU coreutils) 9.1 invoked as "ls -4" prints "ls: invalid option -- '4' \n Try
            // 'ls
            // --help' for more information."
            r.msgToWaitFor = "invalid option";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            builder =
                    new ManagedProcessBuilder("cmd.exe")
                            .addArgument("/C")
                            .addArgument("dir")
                            .addArgument("/?");
            r.msgToWaitFor = "Displays a list of files and subdirectories in a directory.";
        } else {
            throw new ManagedProcessException("Unexpected Platform, improve the test dude...");
        }

        if (setExitValueChecker) {
            builder.setIsSuccessExitValueChecker(Objects::nonNull);
        }
        r.proc = builder.setProcessListener(listener).build();

        return r;
    }

    @Test
    void mustTerminateExec() throws ManagedProcessException {
        ManagedProcessBuilder pb;
        if (SystemUtils.IS_OS_WINDOWS) {
            pb = new ManagedProcessBuilder("notepad.exe");
        } else {
            pb = new ManagedProcessBuilder("sleep");
            if (SystemUtils.IS_OS_MAC) {
                pb.addArgument("30");
            } else { // assume linux
                pb.addArgument("30s");
            }
        }

        ManagedProcess p = pb.build();
        assertFalse(p.isAlive());
        p.start();
        assertTrue(p.isAlive());
        p.waitForExitMaxMsOrDestroy(200);
        assertFalse(p.isAlive());
        // cannot: p.exitValue();
    }

    @Test
    void whoami() throws ManagedProcessException {
        if (SystemUtils.IS_OS_WINDOWS) {
            return;
        }
        ManagedProcess p = new ManagedProcessBuilder("/usr/bin/whoami").build().start();
        assertEquals(0, p.waitForExit());
        assertFalse(p.getConsole().isEmpty());
    }

    @Test
    @Disabled(
            "See https://github.com/vorburger/ch.vorburger.exec/issues/269, and fix this test somehow...")
    void whoAmI() throws ManagedProcessException {
        if (SystemUtils.IS_OS_WINDOWS) {
            return;
        }
        ManagedProcess p =
                new ManagedProcessBuilder("/usr/bin/who")
                        .addArgument("am")
                        .addArgument("i")
                        .build()
                        .start();
        assertEquals(0, p.waitForExit());
        assertFalse(p.getConsole().isEmpty());
    }

    static class TestListener implements ManagedProcessListener {
        int expectedExitValue = Integer.MIN_VALUE;
        int failureExitValue = Integer.MIN_VALUE;
        @Nullable Throwable t;

        @Override
        public void onProcessComplete(int exitValue) {
            expectedExitValue = exitValue;
        }

        @Override
        public void onProcessFailed(int exitValue, @Nullable Throwable throwable) {
            failureExitValue = exitValue;
            t = throwable;
        }
    }
}
