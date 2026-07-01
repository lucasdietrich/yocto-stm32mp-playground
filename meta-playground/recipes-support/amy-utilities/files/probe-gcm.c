/* probe_gcm.c
 * build: gcc -o probe_gcm probe_gcm.c
 */
#include <linux/if_alg.h>
#include <sys/socket.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>

static void try_name(const char *type, const char *name) {
    int sock = socket(AF_ALG, SOCK_SEQPACKET, 0);
    if (sock < 0) { perror("socket"); return; }

    struct sockaddr_alg sa;
    memset(&sa, 0, sizeof(sa));
    sa.salg_family = AF_ALG;
    strncpy((char *)sa.salg_type, type, sizeof(sa.salg_type) - 1);
    strncpy((char *)sa.salg_name, name, sizeof(sa.salg_name) - 1);

    if (bind(sock, (struct sockaddr *)&sa, sizeof(sa)) == 0) {
        printf("OK  : [%s] %s\n", type, name);
    } else {
        printf("FAIL: [%s] %s  errno=%d (%s)\n", type, name, errno, strerror(errno));
    }
    close(sock);
}

int main(void) {
    /* test the building blocks in isolation first */
    try_name("skcipher", "ctr(aes-generic)");
    try_name("skcipher", "ctr(aes)");
    try_name("hash", "ghash-generic");
    try_name("hash", "ghash");
    try_name("cipher", "aes-generic");

    /* then the composite, several spellings */
    try_name("aead", "gcm_base(ctr(aes-generic),ghash-generic)");
    try_name("aead", "gcm(aes-generic)");
    try_name("aead", "gcm(aes)");
    return 0;
}