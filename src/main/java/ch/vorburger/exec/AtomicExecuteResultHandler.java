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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ExecuteResultHandler} which uses a single
 * {@link java.util.concurrent.atomic.AtomicReference}
 * instead of three separate volatile fields like the
 * original {@link org.apache.commons.exec.DefaultExecuteResultHandler} did.
 *
 * @see <a href="https://github.com/vorburger/ch.vorburger.exec/issues/108">Issue #108</a>
 */
public class AtomicExecuteResultHandler implements ExecuteResultHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AtomicExecuteResultHandler.class);

    /** The interval polling the result */
    private static final int SLEEP_TIME_MS = 50;

    private static class Holder {
        /** The exit value of the finished process */
        private final Integer exitValue;

        /** Any offending exception */
        private final ExecuteException exception;

        Holder(int exitValue) {
            this.exitValue = exitValue;
            this.exception = null;
        }

        Holder(ExecuteException e) {
            this.exception = e;
            this.exitValue = null;
        }

        @Override
        public String toString() {
            return "exitValue=" + exitValue + ", exception=" + exception;
        }
    }

    private final AtomicReference<Holder> holder = new AtomicReference<>();

    @Override
    public void onProcessComplete(int exitValue) {
        Holder witness = holder.compareAndExchange(null, new Holder(exitValue));
        if (witness != null) {
            LOG.error("onProcessComplete({}) will throw IllegalStateException, already set: {}", exitValue, witness);
            throw new IllegalStateException(
                    "Ignoring exit value " + exitValue + " because result already set: " + witness);
        }
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        Holder witness = holder.compareAndExchange(null, new Holder(e));
        if (witness != null) {
            LOG.error("onProcessFailed({}) will throw IllegalStateException, already set: {}", e, witness);
            throw new IllegalStateException("Ignoring Exception, because result already set: " + witness, e);
        }
    }

    public Optional<Integer> getExitValue() {
        Holder current = holder.get();
        return current != null ? Optional.ofNullable(current.exitValue) : Optional.empty();
    }

    public Optional<Exception> getException() {
        Holder current = holder.get();
        return current != null ? Optional.ofNullable(current.exception) : Optional.empty();
    }

    public void waitFor() throws InterruptedException {
        while (holder.get() == null) {
            Thread.sleep(SLEEP_TIME_MS);
        }
    }

    public void waitFor(long timeoutInMS) throws InterruptedException {
        final long until = System.currentTimeMillis() + timeoutInMS;
        while (holder.get() == null && System.currentTimeMillis() < until) {
            Thread.sleep(SLEEP_TIME_MS);
        }
    }
}
