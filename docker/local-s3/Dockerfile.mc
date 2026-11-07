# 自建 MinIO Client 镜像(替代 Docker Hub 的 minio/mc)
# 二进制来源:github.com/minio/mc releases RELEASE.2025-08-13T08-35-41Z (linux-amd64)
# 装到 /usr/bin/mc,与 compose 中 createbuckets 的 entrypoint 调用路径一致
FROM alpine:3.20
COPY binaries/mc /usr/bin/mc
RUN chmod +x /usr/bin/mc
ENTRYPOINT ["mc"]
