FROM ubuntu
ARG UID=1001
ARG GID=1001

# Update the package list, install sudo, create a non-root user, and grant password-less sudo permissions
RUN apt update && \
    apt install -y sudo && \
    addgroup --gid $GID nonroot && \
    adduser --uid $UID --gid $GID --disabled-password --gecos "" nonroot && \
    echo 'nonroot ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers

RUN apt install -y curl  \
    && apt install -y unzip  \
    && apt install -y git

# Set the non-root user as the default user
USER nonroot

ARG CACHEBUST=1apt
WORKDIR /home/nonroot
RUN curl -s https://raw.githubusercontent.com/jeka-dev/jeka/0.11.x/dev.jeka.core/src/main/shell/jeka-update | bash -s - install check

