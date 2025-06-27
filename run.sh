#!/bin/bash -ex

# display
mvn -q clean
mvn -q compile
mvn -q exec:exec -Dexec.mainClass=cs1302uga.api/cs1302.api.${1-ApiDriver}
