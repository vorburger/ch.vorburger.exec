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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.io.File;

/**
 * Tests {@link ManagedProcessBuilder}.
 *
 * @author Michael Vorburger
 */
public class ManagedProcessBuilderTest {

    @Test
    public void managedProcessBuilder() throws Exception {
        ManagedProcessBuilder mbp =
                new ManagedProcessBuilder(new File("/somewhere/absolute/bin/thing"));

        File arg = new File("relative/file");
        mbp.addArgument(arg);

        // needed to force auto-setting the directory
        mbp.getCommandLine();

        File cwd = mbp.getWorkingDirectory();
        if (SystemUtils.IS_OS_WINDOWS) {
            // NOT equalTo("C:\\somewhere\\absolute\\bin") because that makes
            // the Test specific to running on a C: drive, which it may not be.
            assertThat(cwd.getAbsolutePath(), endsWith("somewhere\\absolute\\bin"));
        } else {
            assertThat(cwd.getAbsolutePath(), equalTo("/somewhere/absolute/bin"));
        }

        String arg0 = mbp.getExecutable();
        if (SystemUtils.IS_OS_WINDOWS) {
            // NOT equalTo("C:\\somewhere\\absolute\\bin") because that makes
            // the Test specific to running on a C: drive, which it may not be.
            assertThat(arg0, endsWith("somewhere\\absolute\\bin\\thing"));
        } else {
            assertThat(arg0, equalTo("/somewhere/absolute/bin/thing"));
        }

        String arg1 = mbp.getArguments().get(0);
        assertNotSame(arg1, "relative/file");
        assertTrue(arg1.contains("relative"));
        // System.out.println(arg1);
    }
}
