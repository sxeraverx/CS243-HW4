#!/bin/bash

# if java/javac are not in your ${PATH}, set this line to the directory
# they are located in, with a trailing '/', e.g. 'JAVAPATH=/usr/bin/'
JAVAPATH=

compile: clean
	# set up
	echo "PWD=${PWD}"
	mkdir -p classes/optimize classes/test

	# compile
	${JAVAPATH}javac \
		-cp lib/jwutil.jar:lib/javabdd-1.0b2.jar:lib/joeq.jar \
		-sourcepath src -d classes/optimize src/optimize/*.java
	${JAVAPATH}javac -target 1.5 \
		-sourcepath src -d classes/test src/optimize/test/*.java

	# jar it all up
	cd classes/optimize; jar cf optimize.jar `find . -name "*.class"`
	mv classes/optimize/optimize.jar lib/optimize.jar

	cd classes/test; jar cf test.jar `find . -name "*.class"`
	mv classes/test/test.jar lib/test.jar

	# set up parun
	cat bin/parun-template | sed -e "s|PARUNPATH|${PWD}|g" \
		| sed -e "s|JAVAPATH|${JAVAPATH}|g" > bin/parun
	chmod a+x bin/parun

clean:
	find . -name '*~' -delete
	find . -name '#*#' -delete
	rm -rf classes
	rm -rf bin/parun
	rm -rf lib/optimize.jar
	rm -rf lib/test.jar
