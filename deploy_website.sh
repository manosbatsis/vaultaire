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

# Generate the API docs
./gradlew :vaultaire:dokkaForGhPages

# Copy in special files that GitHub wants in the project root.
cp CHANGELOG.md docs/changelog.md
#cp CONTRIBUTING.md docs/contributing.md
cp FAQ.md docs/faq.md
cat README.md > docs/index.md
# Remove hardcoded TOC from index page content
TOC_FIRST=$(grep -n "TOC" docs/index.md | head -n 1 | cut -d: -f1)
TOC_LAST=$(grep -n "TOC -->" docs/index.md | head -n 1 | cut -d: -f1)
sed -i "$TOC_FIRST"','"$TOC_LAST"'d' docs/index.md

# Build the site and push the new files up to GitHub
mkdocs gh-deploy

# Delete our temp folder
cd ..
rm -rf $DIR
