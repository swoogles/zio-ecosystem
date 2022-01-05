# Ziverge Hack Day (December 2021)

Welcome to Ziverge Hack Day, December 2021 Edition!

We could use some help preparing the ZIO library ecosystem for the release of ZIO 2.0. The graph below illustrates the current state of the ecosystem. Some libraries are almost completely upgraded to ZIO 2.0, whereas other libraries need some additional work.

![ZIO 2.0 Ecosystem](./ecosystem.svg)

## Building the Dockerfile
```bash
docker build -t zio-ecosystem .
```

## Running the Dockerfile
```bash
docker run -it zio-ecosystem
```

Hey all! Previously we were using a hand-managed Google docto track the Zio Ecosystem and what version of ZIO each library depends on.

Now that has been replaced by a web app that queries/inspects Maven Central data to get this info so that it is always accurate.

