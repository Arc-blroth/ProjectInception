# ProjectInception - Issue 2

Issue #2 originally began as a followup to issue #1. It was clear that
there were quite a few caveats to JCEF on Linux that I had not previously
considered. But as I delved deeper into the issue, what appeared to be
a simple crash became something much larger.

## Part 1 - Can't initialize JOGL natives!
Problem: JOGL refuses to load .so files from the natives jar

Culprit: There are none to load!

On Windows, the natives jar contains all .dll's bundled inside,
and those libraries are extracted at runtime. But on Linux,
Pandomium handles loading libraries instead, and thus there
aren't actually any libraries for JOGL to load.

## Part 2 - Fix Java 9+
Problem: Natives still won't load - NPE crash in loadLibrary0

Culprit: https://bugs.openjdk.java.net/browse/JDK-8240521

For a long time, we've been able to modify the java native library
search path at runtime by setting `Classloader.sys_paths` to null via
reflection and forcing java to reload the library search path.
Pandomium relies on this undocumented hack to work. Luckily, we
don't actually need to fix the hack, since we're able to control
the exact command line of the Taterwebz subprocess. Instead, we
just ASM the hack out.

## Part 3 - The ICUDTL Segfault
Problem: CEF segfaults or segtraps after erroring with
         "Invalid file descriptor to ICU data received."
         
Culprit: `/proc/self/exe` resolution

At first, I believed this was a simple NPE somewhere due
to the fact that icudtl.dat was misplaced. But moving the
file did not help.

Next, I attempted to debug the NPE. `strace` did not print
anything useful however, `gdb` segfaulted itself while
debugging, and `valgrind` produced segfaults in internal
JVM code. In the end, I turned to the source of
chromium itself to trace the issue.

The one log statement we get is printed
[in icu_util.cc](https://source.chromium.org/chromium/chromium/src/+/master:base/i18n/icu_util.cc;l=241;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10).
The file descriptor that the method complains about
[is passed from the `g_icudtl_pf` variable](https://source.chromium.org/chromium/chromium/src/+/master:base/i18n/icu_util.cc;l=296;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10;bpv=1;bpt=1)
and set
[earlier in that file](https://source.chromium.org/chromium/chromium/src/+/master:base/i18n/icu_util.cc;l=147;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10;bpv=0;bpt=1).
`DIR_ASSETS` [is an alias for](https://source.chromium.org/chromium/chromium/src/+/master:base/base_paths.cc;l=27;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10;bpv=1;bpt=1)
`DIR_MODULE`,
[which is derived from](https://source.chromium.org/chromium/chromium/src/+/master:base/base_paths.cc;l=23;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10;bpv=1;bpt=1)
`FILE_MODULE`.
Platform-specific PathProviders handle the resolution of `FILE_MODULE`: on Linux this is resolved in
[PathProviderPosix](https://source.chromium.org/chromium/chromium/src/+/master:base/base_paths_posix.cc;l=40;drc=ba12ff85e28cfaa5482e89a9c123abab1fe39e10;bpv=0;bpt=1)
with a `readlink` to `/proc/self/exe`.

This is our culprit - under the JVM, `/proc/self/exe` will always be `/usr/bin/java` rather than
the location of `jcef_helper`, which is what we want it to resolve to.
There are a few solutions to this problem, but perhaps the easiest is
to simply intercept the `readlink` call
[using the magic of `LD_PRELOAD`](http://www.goldsborough.me/c/low-level/kernel/2016/08/29/16-48-53-the_-ld_preload-_trick/).

## Part 4 - ld Inconsistency
Problem: "Inconsistency detected by ld.so: dl-lookup.c: 111: check_match:
          Assertion `version->filename == NULL || ! _dl_name_match_p
          (version->filename, map)' failed!"
          
Culprit: The dynamic symbol table in `libawt.so`

This is a [relatively obscure bug in AWT](https://bugs.launchpad.net/ubuntu/+source/openjdk-lts/+bug/1838740)
that affects specific builds of OpenJDK. The only real solution is, unfortunately, is to use a different JRE.