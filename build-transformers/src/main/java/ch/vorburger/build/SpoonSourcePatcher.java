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

import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpoonSourcePatcher {

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: SpoonSourcePatcher <sources-root>");
        }
        Path sourcesRoot = Path.of(args[0]);

        Launcher launcher = new Launcher();
        launcher.addInputResource(sourcesRoot.toString());
        launcher.setSourceOutputDirectory(sourcesRoot.toString());

        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(17);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setPrettyPrintingMode(Environment.PRETTY_PRINTING_MODE.AUTOIMPORT);

        launcher.buildModel();

        patchExecuteWatchdog(launcher);
        patchDefaultExecutor(launcher);

        launcher.prettyprint();
    }

    private static void patchExecuteWatchdog(Launcher launcher) {
        CtClass<?> clazz = launcher.getFactory().Class().get("org.apache.commons.exec.ExecuteWatchdog");
        if (clazz == null) throw new IllegalStateException("Class not found: " + "org.apache.commons.exec.ExecuteWatchdog");

        CtTypeReference<?> duration = t(launcher, "java.time.Duration");
        CtTypeReference<?> threadFactory = t(launcher, "java.util.concurrent.ThreadFactory");

        List<CtTypeReference<?>> sig = List.of(duration, threadFactory);
        if (hasCtor(clazz, sig)) return;

        CtConstructor<?> ctor = makeCtor(
                launcher,
                List.of(
                        new Param("java.time.Duration", "timeout"),
                        new Param("java.util.concurrent.ThreadFactory", "threadFactory")
                ),
                "this(threadFactory, timeout)",
                Map.of(
                        "timeout", "The timeout Duration for the process. It must be greater than 0 or {@code INFINITE_TIMEOUT_DURATION}.",
                        "threadFactory", "The thread factory."
                )
        );

        addAsLastMember(clazz, ctor);
    }

    private static void patchDefaultExecutor(Launcher launcher) {
        CtClass<?> clazz = launcher.getFactory().Class().get("org.apache.commons.exec.DefaultExecutor");
        if (clazz == null) throw new IllegalStateException("Class not found: " + "org.apache.commons.exec.DefaultExecutor");

        CtTypeReference<?> path = t(launcher, "java.nio.file.Path");
        CtTypeReference<?> threadFactory = t(launcher, "java.util.concurrent.ThreadFactory");
        CtTypeReference<?> execStreamHandler = t(launcher, "org.apache.commons.exec.ExecuteStreamHandler");

        List<CtTypeReference<?>> sig = List.of(path, threadFactory, execStreamHandler);
        if (hasCtor(clazz, sig)) return;

        CtConstructor<?> ctor = makeCtor(
                launcher,
                List.of(
                        new Param("java.nio.file.Path", "workingDirectory"),
                        new Param("java.util.concurrent.ThreadFactory", "threadFactory"),
                        new Param("org.apache.commons.exec.ExecuteStreamHandler", "executeStreamHandler")
                ),
                "this(threadFactory, executeStreamHandler, workingDirectory)",
                Map.of(
                        "workingDirectory", "The working directory of the process.",
                        "threadFactory", "The thread factory.",
                        "executeStreamHandler", "Taking care of output and error stream."
                )
        );

        addAsLastMember(clazz, ctor);
    }

    private record Param(String type, String name) {}

    private static CtTypeReference<?> t(Launcher l, String qn) {
        return l.getFactory().Type().createReference(qn);
    }

    private static CtConstructor<?> makeCtor(Launcher l, List<Param> params, String bodyCall, Map<String, String> paramDocs) {
        var f = l.getFactory();

        CtConstructor<?> ctor = f.createConstructor();
        ctor.addModifier(ModifierKind.PROTECTED);

        StringBuilder jd = new StringBuilder();
        jd.append("""
                Auto-generated constructor by SpoonSourcePatcher.
                This constructor was added by the patch process to mirror the Byte Buddy transformation on the binary.
                """).append("\n");
        for (Param p : params) {
            String desc = paramDocs.getOrDefault(p.name, "");
            jd.append("@param ").append(p.name).append(" ").append(desc).append("\n");
        }
        ctor.addComment(f.createComment(jd.toString(),
                CtComment.CommentType.JAVADOC));

        for (Param p : params) {
            f.createParameter(ctor, t(l, p.type), p.name).addModifier(ModifierKind.FINAL);
        }

        CtBlock<?> body = f.createBlock();
        CtStatement call = f.Code().createCodeSnippetStatement(bodyCall);
        body.addStatement(call);
        ctor.setBody(body);

        return ctor;
    }

    private static boolean hasCtor(CtClass<?> clazz, List<CtTypeReference<?>> paramTypes) {
        List<String> want = paramTypes.stream()
                .map(CtTypeReference::getQualifiedName)
                .toList();

        return clazz.getConstructors().stream().anyMatch(c -> {
            List<String> have = c.getParameters().stream()
                    .map(p -> p.getType().getQualifiedName())
                    .toList();
            return have.equals(want);
        });
    }

    private static void addAsLastMember(CtClass<?> clazz, CtConstructor<?> ctor) {
        clazz.addTypeMember(ctor);

        List<CtTypeMember> members = new ArrayList<>(clazz.getTypeMembers());
        members.remove(ctor);
        members.add(ctor);
        clazz.setTypeMembers(members);
    }
}
