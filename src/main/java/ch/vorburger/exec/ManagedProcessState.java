/*
 * #%L
 * MariaDB4j
 * %%
 * Copyright (C) 2012 - 2014 Michael Vorburger
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

public interface ManagedProcessState {

    boolean startAndWaitForConsoleMessageMaxMs(String messageInConsole,
            long maxWaitUntilReturning) throws ManagedProcessException;

    void destroy() throws ManagedProcessException;

    boolean isAlive();

    void notifyProcessHalted();

    int exitValue() throws ManagedProcessException;

    int waitForExit() throws ManagedProcessException;

    int waitForExitMaxMs(long maxWaitUntilReturning) throws ManagedProcessException;

    void waitForExitMaxMsOrDestroy(long maxWaitUntilDestroyTimeout)
            throws ManagedProcessException;

    String getConsole();

    String getLastConsoleLines();

    boolean watchDogKilledProcess();

    String getProcLongName();
}
