/*
 * #%L
 * ch.vorburger.exec
 * %%
 * Copyright (C) 2012 - 2024 Michael Vorburger
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

// https://github.com/vorburger/ch.vorburger.exec/issues/9
public final class NonExistingMain {

    public static void main(String[] args) throws Exception {
        ManagedProcessBuilder mpb = new ManagedProcessBuilder("cmd-does-not-exist");
        ManagedProcess p = mpb.build();
        p.start();
    }

    private NonExistingMain() {
    }
}
