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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

/**
 * Tests ManagedProcess.
 *
 * @author Michael Vorburger
 */
public class ManagedProcessTest {

    @Test
    public void onProcessCompleteInvokedOnCustomListener() throws Exception {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingExec(listener);
        exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        assertNotEquals(Integer.MIN_VALUE, listener.expectedExitValue);
        assertEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNull(listener.t);
    }

    @Test
    public void onProcessFailedInvokedOnCustomListenerTraditional() throws Exception {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingFailingExec(listener, false);
        assertThrows(ManagedProcessException.class, () -> {
            exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        });
        assertEquals(Integer.MIN_VALUE, listener.expectedExitValue);
        assertNotEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNotNull(listener.t);
    }

    @Test
    public void onProcessFailedInvokedOnCustomListenerWithExitValueChecker() throws Exception {
        TestListener listener = new TestListener();
        SomeSelfTerminatingExec exec = someSelfTerminatingFailingExec(listener, true);
        exec.proc.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        assertEquals(2, listener.expectedExitValue);
        assertEquals(Integer.MIN_VALUE, listener.failureExitValue);
        assertNull(listener.t);
    }

    @Test
    public void basics() throws Exception {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertEquals(false, p.isAlive());
        assertThrows(ManagedProcessException.class, () -> p.destroy());
        assertThrows(ManagedProcessException.class, () -> p.exitValue());
        // Commented out because this is a flaky not reliable test, because it's thread scheduling
        // timing dependent :( see long comment inside start() impl. for why this is so
        // assertThrows(ManagedProcessException.class, () -> {
        //     p.start();
        // });
        assertThrows(ManagedProcessException.class, () -> p.waitForExit());
        assertThrows(ManagedProcessException.class, () -> p.waitForExitMaxMs(1234));
        assertThrows(ManagedProcessException.class, () -> p.waitForExitMaxMsOrDestroy(1234));
    }

    @Test
    public void waitForMustFailIfNeverStarted() throws Exception {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertThrows(ManagedProcessException.class, () -> p.waitForExit());
    }

    @Test
    public void startForMustFailForWrongExecutable() throws Exception {
        ManagedProcess p = new ManagedProcessBuilder("someExec").build();
        assertThrows(ManagedProcessException.class, () -> p.start());
    }

    @Test
    public void waitForSeenMessageIfAlreadyTerminated() throws Exception {
        SomeSelfTerminatingExec exec = someSelfTerminatingExec();
        ManagedProcess p = exec.proc;
        // this process should have terminated itself faster than in 1s (1000ms),
        // but this should not cause this to hang, but must return silently:
        p.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
    }

    @Test
    public void waitForWrongMessageIfAlreadyTerminated() throws Exception {
        ManagedProcess p = someSelfTerminatingExec().proc;
        // this process should have terminated itself faster than in 1s (1000ms),
        // but this should not cause this to hang, but must throw an
        // ManagedProcessException
        assertThrows(ManagedProcessException.class, () -> p.startAndWaitForConsoleMessageMaxMs("...", 1000));
    }
    //

    @Test
    public void selfTerminatingExec() throws Exception {
        SomeSelfTerminatingExec exec = someSelfTerminatingExec();
        ManagedProcess p = exec.proc;

        assertEquals(false, p.isAlive());
        p.startAndWaitForConsoleMessageMaxMs(exec.msgToWaitFor, 1000);
        // can't assertThat(p.isAlive(), is(true)); - if p finishes too fast, this fails
        // -
        // unreliable
        // test :(
        //

        p.waitForExit();
        p.exitValue(); // just making sure it works, don't check, as Win/NIX diff.
        assertEquals(false, p.isAlive());

        // It's NOT OK to call destroy() on a process which already terminated
        assertThrows(ManagedProcessException.class, () -> p.destroy());

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

    protected SomeSelfTerminatingExec someSelfTerminatingExec(@Nullable ManagedProcessListener listener)
            throws ManagedProcessException {
        SomeSelfTerminatingExec r = new SomeSelfTerminatingExec();
        if (SystemUtils.IS_OS_WINDOWS) {
            r.proc = new ManagedProcessBuilder("cmd.exe").addArgument("/C").addArgument("dir").addArgument("/X")
                    .setProcessListener(listener).build();
            r.msgToWaitFor = "bytes free";
        } else if (SystemUtils.IS_OS_SOLARIS) {
            r.proc = new ManagedProcessBuilder("true").addArgument("--version").setProcessListener(listener).build();
            r.msgToWaitFor = "true (GNU coreutils)";
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            r.proc = new ManagedProcessBuilder("echo")
                    .addArgument("\"Lorem ipsum dolor sit amet, consectetur adipisci elit, "
                            + " sed eiusmod tempor incidunt ut \nlabore et dolore magna aliqua.\"")
                    .setProcessListener(listener).build();
            r.msgToWaitFor = "incidunt";
        } else {
            throw new ManagedProcessException("Unexpected Platform, improve the test dude...");
        }

        return r;
    }

    protected SomeSelfTerminatingExec someSelfTerminatingFailingExec(ManagedProcessListener listener,
            boolean setExitValueChecker)
            throws ManagedProcessException {
        ManagedProcessBuilder builder;
        SomeSelfTerminatingExec r = new SomeSelfTerminatingExec();
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            builder = new ManagedProcessBuilder("ls").addArgument("-4");
            // ls (GNU coreutils) 9.1 invoked as "ls -4" prints "ls: invalid option -- '4'
            // \n Try 'ls --help' for more information."
            r.msgToWaitFor = "invalid option";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            //
            builder = new ManagedProcessBuilder("dir").addArgument("/?");
            r.msgToWaitFor = "Displays a list of files and subdirectories in a directory.";
        } else {
            throw new ManagedProcessException("Unexpected Platform, improve the test dude...");
        }

        if (setExitValueChecker) {
            builder.setIsSuccessExitValueChecker(exitValue -> exitValue != null);
        }
        r.proc = builder.setProcessListener(listener).build();

        return r;
    }

    @Test
    public void mustTerminateExec() throws Exception {
        ManagedProcessBuilder pb;
        if (SystemUtils.IS_OS_WINDOWS) {
            pb = new ManagedProcessBuilder("notepad.exe");
        } else {
            pb = new ManagedProcessBuilder("sleep");
            pb.addArgument("30s");
        }

        ManagedProcess p = pb.build();
        assertEquals(false, p.isAlive());
        p.start();
        assertEquals(true, p.isAlive());
        p.waitForExitMaxMsOrDestroy(200);
        assertEquals(false, p.isAlive());
        // cannot: p.exitValue();
    }

    static class TestListener implements ManagedProcessListener {
        int expectedExitValue = Integer.MIN_VALUE;
        int failureExitValue = Integer.MIN_VALUE;
        Throwable t;

        @Override
        public void onProcessComplete(int exitValue) {
            expectedExitValue = exitValue;
        }

        @Override
        public void onProcessFailed(int exitValue, Throwable throwable) {
            failureExitValue = exitValue;
            t = throwable;
        }
    }
}
