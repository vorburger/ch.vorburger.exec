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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Tests {@link ManagedProcessBuilder}.
 *
 * @author Michael Vorburger
 */
class ManagedProcessBuilderTest {

    @Test
    void managedProcessBuilder() throws IOException {

        ManagedProcessBuilder mbp =
                new ManagedProcessBuilder(
                        Path.of("/somewhere").resolve("absolute").resolve("bin").resolve("thing"));

        Path arg = Path.of("relative").resolve("file");
        mbp.addArgument(arg);

        // needed to force auto-setting the directory
        mbp.getCommandLine();

        Path cwd = mbp.getWorkingDirectory();
        Assertions.assertNotNull(cwd);
        Path testPath = Path.of("somewhere").resolve("absolute").resolve("bin");
        assertThat(cwd.toAbsolutePath().endsWith(testPath)).isTrue();

        Path execPath = Path.of(mbp.getExecutable());
        Path expectedSuffix = testPath.resolve("thing");

        assertThat(execPath.endsWith(expectedSuffix)).isTrue();

        String arg0 = mbp.getArguments().get(0);
        assertNotSame("relative/file", arg0);
        assertTrue(arg0.contains("relative"));
    }
}
