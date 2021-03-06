# Yoke

Yoke is a polyglot middleware framework for Vert.x, shipping with over 12 bundled middleware.

[![Build Status](https://travis-ci.org/pmlopes/yoke.png?branch=master)](https://travis-ci.org/pmlopes/yoke)


## Instalation

To get started with Yoke in your project you can use Vert.x module system to download it from the maven repository or
include the maven dependency your self. The moment this module is only compatible with Vert.x 2. It can be backported to
the 1.x series but there is no work in progress in that direction.

The Vert.x module id is: `com.jetdrone~yoke~1.0.4`. If you prefer to use [maven](http://maven.apache.org), you can get
the artifacts using the following dependency:

    <dependency>
      <groupId>com.jetdrone</groupId>
      <artifactId>yoke</artifactId>
      <version>1.0.4</version>
      <scope>provided</scope>
    </dependency>

The scope is provided because you should include the module from your Vert.x application to avoid having duplicated jars
in the server classpath.


## Getting started

Yoke is a polyglot framework so you should choose a trail to follow with your favourite language:

* [Java](java/Readme.html)
* [Groovy](groovy/Readme.html)
* [JavaScript](resources/Readme.html)

You can also get tutorials:

* [Java-Tutorial](http://pmlopes.github.io/yoke/Java-Tutorial.html)
* [Groovy-Tutorial](http://pmlopes.github.io/yoke/Groovy-Tutorial.html)
* [JavaScript-Tutorial](http://pmlopes.github.io/yoke/JavaScript-Tutorial.html)
* [Mozilla Persona](http://pmlopes.github.io/yoke/Persona.html)

And also Benchmarks:

* [Benchmark](http://pmlopes.github.io/yoke/Benchmark.html)

If you need help with Yoke. just ask your questions on [yoke framework group](https://groups.google.com/forum/#!forum/yoke-framework).


## Example Applications

Under the directory example you will find a couple of examples built with Yoke.

* A naive implementation of a CMS application using Redis as a database backend
* Mozilla Persona authentication implementation
* Groovy get started example
* JavaScript get started example

All these examples are presented as a showcase of the framework, they are lacking lots of features and testing.


## Inspiration

This project was inspired by [Connect](http://www.senchalabs.org/connect/).


## License

This project is released under the Apache License 2.0 as included in the root of the project.
