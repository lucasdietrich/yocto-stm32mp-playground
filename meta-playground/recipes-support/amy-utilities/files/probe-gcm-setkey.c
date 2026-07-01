/* probe_gcm_setkey.c
 * build: gcc -o probe_gcm_setkey probe_gcm_setkey.c
 */
#include <linux/if_alg.h>
#include <sys/socket.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <unistd.h>

int main(void) {
    int sock = socket(AF_ALG, SOCK_SEQPACKET, 0);
    if (sock < 0) { perror("socket"); return 1; }

    struct sockaddr_alg sa;
    memset(&sa, 0, sizeof(sa));
    sa.salg_family = AF_ALG;
    strcpy((char *)sa.salg_type, "aead");
    strcpy((char *)sa.salg_name, "gcm(aes)");

    if (bind(sock, (struct sockaddr *)&sa, sizeof(sa)) != 0) {
        printf("bind FAIL errno=%d (%s)\n", errno, strerror(errno));
        return 1;
    }
    printf("bind OK\n");

    unsigned char key[32]; /* 256 bits */
    memset(key, 0x42, sizeof(key));

    if (setsockopt(sock, SOL_ALG, ALG_SET_KEY, key, sizeof(key)) != 0) {
        printf("setkey FAIL errno=%d (%s)\n", errno, strerror(errno));
        return 1;
    }
    printf("setkey OK (256 bit)\n");

    if (setsockopt(sock, SOL_ALG, ALG_SET_AEAD_AUTHSIZE, NULL, 16) != 0) {
        printf("set authsize FAIL errno=%d (%s)\n", errno, strerror(errno));
        return 1;
    }
    printf("authsize OK\n");

    close(sock);
    return 0;
}