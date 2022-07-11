FROM python:3.7

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:11 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Setup adapted from https://github.com/hseeberger/scala-sbt/blob/master/debian/Dockerfile
RUN \
  apt-get update -q && \
  apt-get upgrade -qq && \
  apt-get install -y git gcc graphviz-dev && \
  rm -rf /var/lib/apt/lists/* && \
  pip install --upgrade pip

# Install SBT
RUN \
  curl -L "https://github.com/sbt/sbt/releases/download/v1.5.7/sbt-1.5.7.tgz" | tar zxf - -C /usr/share && \
  cd /usr/share/sbt/bin && \
  rm sbt.bat sbtn-x86_64-apple-darwin sbtn-x86_64-pc-linux sbtn-x86_64-pc-win32.exe && \
  ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt

# Copy files into the Docker image
WORKDIR /project-root
COPY project ./project
COPY python-code ./python-code
COPY server/src ./src
COPY build.sbt ./

# Compile the Scala code
RUN sbt compile;

# Set up the Python code
RUN \
    cd python-code && \
    pip install -U -r requirements.txt && \
    cd ..

# This runs the Scala program in "dot" mode. The output is piped to the Python
# code which renders it as a spiffy SVG graphic to STDOUT.
CMD /bin/sh -c 'sbt --error "run dot" | python python-code/zio_ecosystem/create_svg_graphic.py'
