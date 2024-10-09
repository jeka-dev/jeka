# docker file to elaborate jekadev/jeka docker images
# This is intended to play with, and not use in the build itself.

FROM ubuntu
RUN apt update
RUN apt install -y curl && apt install -y unzip && apt install -y git && apt install -y gcc && apt install -y libz-dev

RUN curl -sL https://jeka.dev/install.sh | /bin/bash -s - install

# This line is for installing musl on ubuntu in order to compile with graalvm with options '--static --libc=musl'
# Unfortunatly this fail cause linker does not find libz library
# Static linkage is mandatory to run on linux as alpine or distroless
#/usr/bin/ld: cannot find -lz: No such file or directory
#            collect2: error: ld returned 1 exit status
RUN apt install -y musl && apt install -y musl-dev && apt install -y musl-tools

RUN mkdir /workdir
WORKDIR /workdir

ENTRYPOINT ["/bin/bash"]
