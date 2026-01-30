FROM amazonlinux:2023 AS builder

# Install build dependencies
RUN dnf -y install gcc make tar gzip

# Build liburing from source (includes liburing-ffi.so)
ARG LIBURING_VERSION=2.5
RUN curl -sL https://github.com/axboe/liburing/archive/refs/tags/liburing-${LIBURING_VERSION}.tar.gz | tar xz && \
    cd liburing-liburing-${LIBURING_VERSION} && \
    ./configure --prefix=/usr && \
    make -j$(nproc) && \
    make install

# Extract OpenSearch
COPY opensearch-min-*.tar.gz /opt/opensearch.tar.gz
RUN mkdir -p /usr/share/opensearch && \
    tar zxf /opt/opensearch.tar.gz -C /usr/share/opensearch --strip-components=1

# Install plugin
COPY opensearch-iouring-*.zip /tmp/
RUN /usr/share/opensearch/bin/opensearch-plugin install --batch file:///tmp/opensearch-iouring-*.zip

# Final image
FROM amazonlinux:2023

RUN dnf -y install nmap-ncat shadow-utils && dnf clean all

# Copy liburing libraries
COPY --from=builder /usr/lib64/liburing* /usr/lib64/
RUN ldconfig

RUN groupadd -g 1000 opensearch && \
    adduser -u 1000 -g 1000 -d /usr/share/opensearch opensearch

COPY --from=builder --chown=1000:0 /usr/share/opensearch /usr/share/opensearch

USER opensearch
WORKDIR /usr/share/opensearch
ENV PATH=/usr/share/opensearch/bin:$PATH

EXPOSE 9200 9300
CMD ["opensearch"]
