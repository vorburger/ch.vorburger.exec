ch.vorburger.exec [![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.vorburger.exec/exec/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.vorburger.exec/exec)
[![Javadocs](http://www.javadoc.io/badge/ch.vorburger.exec/exec.svg)](http://www.javadoc.io/doc/ch.vorburger.exec/exec)
=================

This is a small library allowing to launch external processes from Java code in the background,
and conveniently correctly pipe their output e.g. into slf4j, await either their termination or specific output, etc.

_If you like/use this project, [Sponsoring me](https://github.com/sponsors/vorburger) or a Star / Watch / Follow me on GitHub is very much appreciated!_

[Release Notes are in CHANGES.md](CHANGES.md).

Usage
---

Launching external processes from Java using the raw java.lang.ProcessBuilder API directly can be a little cumbersome.
[Apache Commons Exec](https://commons.apache.org/proper/commons-exec/) makes it a bit easier, but lacks some convenience.
This library makes it truly convenient:

```java
ManagedProcessBuilder pb = new ManagedProcessBuilder("someExec")
    .addArgument("arg1")
    .setWorkingDirectory(new File("/tmp"))
    .getEnvironment().put("ENV_VAR", "...")
    .setDestroyOnShutdown(true)
    .addStdOut(new BufferedOutputStream(new FileOutputStream(outputFile)))
    .setConsoleBufferMaxLines(7000);  // used by startAndWaitForConsoleMessageMaxMs

ManagedProcess p = pb.build();
p.start();
p.isAlive();
p.waitForExit();
// OR: p.waitForExitMaxMsOrDestroy(5000);
// OR: p.startAndWaitForConsoleMessageMaxMs("Successfully started", 3000);
p.exitValue();
// OR: p.destroy();

// This works even while it's running, not just when it exited
String output = p.getConsole();
```

If you need to, you can also attach a listener to get notified when the external process ends, by using `setProcessListener()` on the `ManagedProcessBuilder` with a `ManagedProcessListener` that implements `onProcessComplete()` and `onProcessFailed()`.

We currently internally use Apache Commons Exec by building on top, extending and wrapping it,
but without exposing this in its API, so that theoretically in the future this implementation detail could be changed.

Advantages
---

* automatically logs external process's STDOUT and STDERR using SLF4j out of the box (can be customized)
* automatically logs and throws for common errors (e.g. executable not found), instead of silently ignoring like j.l.Process
* automatically destroys external process with JVM shutdown hook (can be disabled)
* lets you await appearance of certain messages on the console
* lets you write tests against the expected output

History
---

Historically, this code was part of [MariaDB4j](https://github.com/vorburger/MariaDB4j/) (and this is why it's initial version was 3.0.0),
but was it later split into a separate project. This was done to make it usable in separate projects
(originally [to launch Ansible Networking CLI commands from OpenDaylight](https://github.com/shague/opendaylight-ansible), later [to manage etcd servers in tests](https://github.com/etcd-io/jetcd/issues/361),
both from [OpenDaylight](http://www.opendaylight.org)); later for use in <https://enola.dev>.

Similar Projects
---

For the _exec_ functionality, [zt-exec](https://github.com/zeroturnaround/zt-exec) (with [zt-process-killer](https://github.com/zeroturnaround/zt-process-killer)) is similar ([but refused to backlink us](https://github.com/zeroturnaround/zt-exec/pull/25)).

[NuProcess](https://github.com/brettwooldridge/NuProcess) is another similar library in the same space.

[`fleipold/jproc`](https://github.com/fleipold/jproc] is yet another similar library.

[os-lib](https://github.com/com-lihaoyi/os-lib) is a Scala library including functionality like this.

Related Projects
---

For the _expect-like_ functionality, from https://en.wikipedia.org/wiki/Expect#Java, note (in no particular order):

* https://github.com/vorburger/vexpect
* http://expectj.sourceforge.net
* https://github.com/cverges/expect4j
* https://github.com/Alexey1Gavrilov/ExpectIt
* https://github.com/iTransformers/expect4java
* https://github.com/ronniedong/Expect-for-Java

Release
---

First test that GPG is set up correctly (`gpg: no default secret key: No secret key
gpg: signing failed: No secret key`), and that the `settings.xml` [has the credz](https://github.com/vorburger/ch.vorburger.exec/issues/105)
for `oss.sonatype.org` (`status: 401 unauthorized`):

    git checkout main
    ./mvnw verify -Pgpg

    ./mvnw deploy

Once that works, the next release can be done similarly similarly to https://github.com/vorburger/MariaDB4j#release:

    git checkout main
    ./mvnw release:prepare
    ./mvnw release:perform -Pgpg
    ./mvnw release:clean
    git push

If `./mvnw release:prepare` fails with the following error, then comment out `forceSignAnnotated = true` under `[tag]` in `~/.gitconfig`:

    The git-tag command failed.
    [ERROR] Command output:
    [ERROR] error: gpg failed to sign the data
    [ERROR] error: unable to sign the tag

ToDo
---

This library is currently used to control daemon style external executables.
To launch a process which returns binary (or massive textual) output to its STDOUT
(and, presumably, have that piped into a java.io.OutputStream), it would need some tweaks.
This would include making the enabled-by-default logging into slf4j, and the built-in
startAndWaitForConsoleMessageMaxMs which collects output, a more configurable option.

Contributions & patches more than welcome!
