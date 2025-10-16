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
package ch.vorburger.build;

import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.jspecify.annotations.NonNull;

public final class DefaultExecutorCtorPlugin implements Plugin {

    @Override
    public boolean matches(TypeDescription target) {
        return target.getName().equals("org.apache.commons.exec.DefaultExecutor");
    }

    @Override
    public DynamicType.@NonNull Builder<?> apply(DynamicType.@NonNull Builder<?> builder,
                                        @NonNull TypeDescription type,
                                        @NonNull ClassFileLocator cfl) {
        try {
            // Resolve referenced types from class files being transformed (no Class.forName).
            TypePool pool = TypePool.Default.of(cfl);
            TypeDescription threadFactory =
                    TypeDescription.ForLoadedType.of(java.util.concurrent.ThreadFactory.class); // JDK is safe to load
            TypeDescription execStreamHandler =
                    pool.describe("org.apache.commons.exec.ExecuteStreamHandler").resolve();
            TypeDescription path =
                    TypeDescription.ForLoadedType.of(java.nio.file.Path.class);

            // Find existing ctor: (ThreadFactory, ExecuteStreamHandler, Path)
            MethodDescription.InDefinedShape superCtor =
                    type.getDeclaredMethods()
                            .filter(ElementMatchers.isConstructor()
                                    .and(ElementMatchers.takesArguments(threadFactory, execStreamHandler, path)))
                            .getOnly();

            // Define new protected ctor: (Path, ThreadFactory, ExecuteStreamHandler)
            // Then map to super order: (ThreadFactory, ExecuteStreamHandler, Path) => 1, 2, 0
            return builder
                    .defineConstructor(Visibility.PROTECTED)
                    .withParameter(path, "workingDirectory")
                    .withParameter(threadFactory, "threadFactory")
                    .withParameter(execStreamHandler, "executeStreamHandler")
                    .intercept(MethodCall.invoke(superCtor)
                            .withArgument(1)  // ThreadFactory
                            .withArgument(2)  // ExecuteStreamHandler
                            .withArgument(0)  // Path
                    );

        } catch (Exception e) {
            throw new IllegalStateException("Failed to add protected constructor to DefaultExecutor", e);
        }
    }

    @Override
    public void close() { }
}
