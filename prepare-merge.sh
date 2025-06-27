#!/bin/bash -ex

# here
git checkout main
echo "* $(date)" >> README.rst
git commit -am "preparing to merge $*"

