#!/bin/bash

# The website is built using MkDocs with the Material theme.
# https://squidfunk.github.io/mkdocs-material/
# It requires Python to run.
# Install the packages with the following command:
# pip install mkdocs mkdocs-material

set -ex

REPO="https://github.com/manosbatsis/vaultaire.git"
DIR=temp-clone

# Delete any existing temporary website clone
rm -rf $DIR

# Clone the current repo into temp folder
git clone $REPO $DIR

# Move working directory into temp folder
cd $DIR

# Remove redundant link
sed -i '/See complete documentation at/d' README.md

# Generate the API docs
./gradlew :vaultaire:dokkaForGhPages

cat README.md > docs/index.md

# Build the site and push the new files up to GitHub
mkdocs gh-deploy

# Delete our temp folder
cd ..
rm -rf $DIR
