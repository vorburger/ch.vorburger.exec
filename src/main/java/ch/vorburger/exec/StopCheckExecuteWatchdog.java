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

import org.apache.commons.exec.ExecuteWatchdog;

public class StopCheckExecuteWatchdog extends ExecuteWatchdog {
    private volatile boolean stopped = false;

    /**
     * Creates a new watchdog with a given timeout.
     *
     * @param timeout the timeout for the process in milliseconds. It must be greater than 0 or
     *     'INFINITE_TIMEOUT'
     */
    @SuppressWarnings(
            "deprecation") // TODO https://github.com/vorburger/ch.vorburger.exec/issues/189
    public StopCheckExecuteWatchdog(long timeout) {
        super(timeout);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop() {
        super.stop();
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }
}
