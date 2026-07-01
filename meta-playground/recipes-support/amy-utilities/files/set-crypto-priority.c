/* set_crypto_priority.c
 * build: gcc -o set_crypto_priority set_crypto_priority.c
 * run as root: ./set_crypto_priority "stm32-gcm-aes" 0
 */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <linux/cryptouser.h>

#define NETLINK_CRYPTO 21

struct nl_req {
    struct nlmsghdr nlh;
    struct crypto_user_alg cru;
    struct nlattr attr;
    __u32 priority;
};

int main(int argc, char **argv) {
    if (argc != 3) {
        fprintf(stderr, "usage: %s <driver_name> <priority>\n", argv[0]);
        return 1;
    }

    const char *driver_name = argv[1];
    __u32 new_priority = (__u32)strtoul(argv[2], NULL, 10);

    int fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_CRYPTO);
    if (fd < 0) { perror("socket"); return 1; }

    struct sockaddr_nl sa;
    memset(&sa, 0, sizeof(sa));
    sa.nl_family = AF_NETLINK;
    if (bind(fd, (struct sockaddr *)&sa, sizeof(sa)) < 0) {
        perror("bind");
        return 1;
    }

    struct nl_req req;
    memset(&req, 0, sizeof(req));

    req.nlh.nlmsg_len = sizeof(req);
    req.nlh.nlmsg_type = CRYPTO_MSG_UPDATEALG;
    req.nlh.nlmsg_flags = NLM_F_REQUEST | NLM_F_ACK;
    req.nlh.nlmsg_seq = 1;
    req.nlh.nlmsg_pid = getpid();

    strncpy(req.cru.cru_driver_name, driver_name,
            sizeof(req.cru.cru_driver_name) - 1);

    req.attr.nla_len = sizeof(struct nlattr) + sizeof(__u32);
    req.attr.nla_type = CRYPTOCFGA_PRIORITY_VAL;
    req.priority = new_priority;

    struct sockaddr_nl dst;
    memset(&dst, 0, sizeof(dst));
    dst.nl_family = AF_NETLINK;

    if (sendto(fd, &req, req.nlh.nlmsg_len, 0,
               (struct sockaddr *)&dst, sizeof(dst)) < 0) {
        perror("sendto");
        return 1;
    }

    char buf[4096];
    ssize_t n = recv(fd, buf, sizeof(buf), 0);
    if (n < 0) { perror("recv"); return 1; }

    struct nlmsghdr *resp = (struct nlmsghdr *)buf;
    if (resp->nlmsg_type == NLMSG_ERROR) {
        struct nlmsgerr *err = (struct nlmsgerr *)((char *)resp + NLMSG_HDRLEN);
        if (err->error != 0) {
            fprintf(stderr, "netlink error: %s\n", strerror(-err->error));
            return 1;
        }
        printf("priority updated ok\n");
    }

    close(fd);
    return 0;
}