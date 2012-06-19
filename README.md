## Field

Field is an open-source software project initiated by OpenEndedGroup, for the creation of their digital artworks. It is an environment for writing code to rapidly and experimentally assemble and explore algorithmic systems. It is _visual_, it is _hybrid_, it is _code-based_. We think that it has something to offer a diverse range of programmers and artists. It is developed and tested on Mac OS X (primarily) and Ubuntu 12 + Nvidia proprietary drivers.

*Our main documentation website is here: http://openendedgroup.com/field*

### Building

To build Field:

	git clone git@github.com:OpenEndedGroup/Field.git
	cd Field/Contents
	ant


That will build the core of Field. Individual plugins have additional targets inside the `build.xml` file. If want to build them all (and have both Max/MSP and Processing 2.0x installed) then `ant extras_all`

### Running --- Mac OS X

This repository is _inside_ a Mac OS X Application structure. That is, if you name this repository `field.app` then you'll have a double-clickable Mac app. 

If you are on OS X you probably want to set this:

	defaults write com.openendedgroup.Field use16 YES

This tells Field to launch using an OS installed 1.6 VM. If you want to use an OpenJDK 1.7 VM place the .jdk bundle inside Contents/Plugins and:
	
	defaults delete com.openendedgroup.Field use16 

Finally, to run from the command line, run 

	./Contents/MacOS/field_mac64.sh -field.scratch nameOfFileToOpen.field

### Running --- Linux

To run:

	./Contents/Linux/field_linux32.sh -field.scratch nameOfFileToOpen.field

or:

	./Contents/Linux/field_linux64.sh -field.scratch nameOfFileToOpen.field

Should you find that Field crashes on startup complaining of "No Handles", you need to install libwebkitgtk-1.0. For example:

	sudo apt-get install libwebkitgtk-1.0-0


