#define _GNU_SOURCE

#include <stdlib.h>
#include <dlfcn.h>
#include <stdio.h>

typedef ssize_t (*original_readlink_t)(char *, char *, size_t);

ssize_t original_readlink(const char *pathname, char *buf, size_t bufsiz) {
    return ((original_readlink_t)dlsym(RTLD_NEXT, "readlink"))(pathname, buf, bufsiz);
}

ssize_t readlink(const char *pathname, char *buf, size_t bufsiz) {

    // @Inject(method = "readlink", at = @At("HEAD"))
    if (strcmp(pathname, "/proc/self/exe") == 0) {
        char* result_buf = getenv("PROJECT_INCEPTION_PROC_SELF_EXE");
        ssize_t bytes_put = 0;
        size_t result_buf_length = strlen(result_buf);
        while (bytes_put < result_buf_length && bytes_put < bufsiz) {
            buf[bytes_put] = result_buf[bytes_put];
            bytes_put++;
        }
        return bytes_put;
    }

    return original_readlink(pathname, buf, bufsiz);
}