/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2016 - 2023 Michael Vorburger
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

import org.jspecify.annotations.Nullable;
import org.slf4j.event.Level;

/**
 * Dispatcher of STDOUT vs STDIN output to SLF4j logger levels.
 *
 * <p>This allows to customize a ManagedProcess' default behavior of sending STDOUT to INFO and
 * STDERR to ERROR. In particular, this can be used to tune "noisy" processes which output too much
 * stuff to STDERR which isn't really meant for an error log in a Java application, by filtering
 * based on actual output line content (useful e.g. if the process uses its own text to distinguish
 * log levels in it's output).
 *
 * <p>It also allows to suppress logging (some) STDOUT and/or STDERR all together, by permitting to
 * return <code>null</code>. This is useful e.g. for processes which emit some lines you might never
 * want to see in any log at all, e.g. for security.
 *
 * @author Michael Vorburger
 */
public class OutputStreamLogDispatcher {

    /**
     * Determines the logging level for a given line of output from a managed process.
     *
     * <p>This method can be overridden by subclasses to implement custom logging logic. For
     * example, it can be used to parse the line content to determine a more appropriate log level,
     * or to suppress logging for certain lines entirely.
     *
     * <p>The {@code line} parameter is not used in this default implementation but is provided for
     * subclasses that may need to inspect the line content for dispatching.
     *
     * @param type The type of output stream ({@link ch.vorburger.exec.OutputStreamType#STDOUT
     *     STDOUT} or {@link ch.vorburger.exec.OutputStreamType#STDERR STDERR}) from which the line
     *     originated.
     * @param line The actual line of text output by the process.
     * @return The SLF4J {@link org.slf4j.event.Level} at which this line should be logged.
     *     Returning {@code null} will cause this line to not be logged at all. The default
     *     implementation returns {@link org.slf4j.event.Level#INFO} for STDOUT and {@link
     *     org.slf4j.event.Level#ERROR} for STDERR.
     */
    @SuppressWarnings("unused")
    public @Nullable Level dispatch(OutputStreamType type, String line) {
        return type == OutputStreamType.STDOUT ? Level.INFO : Level.ERROR;
    }
}
