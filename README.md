# XStream Documentation

This repository contains the GitHub pages for [XStream](https://github.com/x-stream/xstream)
that are used to render automatically the static [website of XStream](https://x-stream.github.io/). 

# Site Generation

The content of this repository is generated. XStream uses Maven and XSite to generate the pages.
To create an update of the site, you have to edit the sources in the XStream repository.
 
## Prerequisites
+ Java 8 runtime
+ Maven 3.0
 
## Steps
0. Create a clone of the current repository
0. Create a clone of the [XStream Git repository](https://github.com/x-stream/xstream) using
the same base directory and switch to v-1.4.x branch.
0. Edit the sources in xstream.git/xstream-distribution/src/content
0. From the root of xstream.git call

		mvn clean package
		
0. Update the repository with the GitHub pages.

		rsync -cr --progress --delete --exclude=".*" --exclude="*.md" --exclude=jira --exclude 1\\.\* --exclude="*javadoc" ./xstream-distribution/target/xsite/ ../x-stream.io/
 	 
## After Release
 
Typically you want to update the site after a XStream release. In that case you can call: 
 
 	cp -r target/checkout/xstream-distribution/target/xsite ../x-stream.io/''<version>''
	rsync -cr --progress --delete --exclude=".*" --exclude="*.md" --exclude=jira --exclude 1\\.\* ./target/checkout/xstream-distribution/target/xsite/ ../x-stream.io/

Note, that this variant will also replace the javadocs.
