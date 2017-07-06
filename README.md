ch.vorburger.exec
=================

_ If you like/use this project, a Star / Watch / Follow me on GitHub is appreciated._

_TODO Fix up badges..._

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.vorburger.exec/exec/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.vorburger.exec/exec)
[![Javadocs](http://www.javadoc.io/badge/ch.vorburger.mariaDB4j/mariaDB4j-core.svg)](http://www.javadoc.io/doc/ch.vorburger.mariaDB4j/mariaDB4j-core)
[![JitPack](https://jitpack.io/v/vorburger/MariaDB4j.svg)](https://jitpack.io/#vorburger/MariaDB4j)
[![Dependency Status](https://www.versioneye.com/java/ch.vorburger.mariadb4j:mariadb4j/2.2.2/badge?style=flat)](https://www.versioneye.com/java/ch.vorburger.mariadb4j:mariadb4j/2.2.2)
[![Build Status](https://secure.travis-ci.org/vorburger/MariaDB4j.png?branch=master)](http://travis-ci.org/vorburger/MariaDB4j/)

This project is a small library allowing to launch external processes from Java code in the background,
and conveniently correctly pipe their output e.g. into slf4j, await either their termination or specific output, etc.

It builds on top, extends and wraps Apache commons exec (but without exposing this in its API).  _TODO Link to Apache commons exec (spelling?)_

[Release Notes are in CHANGES.md](CHANGES.md).

Usage
---

```java
ManagedProcess pb = new ManagedProcessBuilder("someExec").build();
pb.addArgument("arg1");
pb.setWorkingDirectory(new File("/tmp"));
pb.getEnvironment().put("ENV_VAR", "...");
ManagedProcess p = pb.build();

p.start();
p.isAlive();
p.waitForExit();
// OR: p.waitForExitMaxMsOrDestroy(5000);
// OR: p.startAndWaitForConsoleMessageMaxMs("Successfully started", 3000);
p.exitValue();
```

History
---

Historically, this code was part of [MariaDB4j](https://github.com/vorburger/MariaDB4j/),
but was it later split into a separate project. This was done to make it usable in separate projects
(originally for a [POC to launch Ansible Networking CLI commands](https://github.com/vorburger/opendaylight-ansible/)
from [OpenDaylight](http://www.opendaylight.org)).

Contributions, patches, forks more than welcome!
