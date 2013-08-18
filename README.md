## Field

Field is an open-source software project initiated by OpenEndedGroup, for the creation of their digital artworks. It is an environment for writing code to rapidly and experimentally assemble and explore algorithmic systems. It is _visual_, it is _hybrid_, it is _code-based_. We think that it has something to offer a diverse range of programmers and artists. It is developed and tested on Mac OS X (primarily) and Ubuntu 12 + Nvidia proprietary drivers.

*Our main documentation website is here: http://openendedgroup.com/field*

### MacOS X

#### Building Field

Before anything else, ensure you have Xcode installed.  Field won't build without it.

To build Field with Java 7 (as supplied by Apple):

	$ cd /Applications
	$ git clone git://github.com/OpenEndedGroup/Field.git
	$ mv Field Field.app
	$ mkdir -p Field.app/Contents/Plugins/1.7.0.jdk/Contents
	$ cd Field.app/Contents/Plugins/1.7.0.jdk/Contents
	$ ln -s /System/Library/Frameworks/JavaVM.framework/Home
	$ cd ../../..
	$ ant

That will build the core of Field. Individual plugins have additional targets inside the `build.xml` file. If you want to build them all (and have both Max/MSP and Processing 2.0b1 installed) then `ant extras_all`. For the less bold, you can add individual plugins: `extras_jsrscripting` adds nascant support for other languages (including Clojure and Ruby); `extras_online` adds a plugin that lets you write in-browser JavaScript; `extras_jfbxlib` brings in fbx support and so on. See the `build.xml` file for a list of targets.

#### Note

Notice we renamed the cloned repo to Field.app above?

This gives you a clickable "Field" application, so you can launch Field from your main Applications list.

Before this will work, you need to log out and back in after building Field for the first time.


#### Starting Field

Ensure Mercurial is installed first.  It can be installed using [OSX Homebrew](http://brew.sh) using:

	$ brew install hg

If you are using an older version of Java, you will need to do this before running Field the first time:

	$ cd /Applications/Field.app
	$ defaults write com.openendedgroup.Field use16 YES

Instead of launching Field from its application icon, you can start it from the command line, optionally passing a filename to open:

	$ cd /Applications/Field.app
	$ ./Contents/MacOS/field_mac64.sh -field.scratch nameOfFileToOpen.field

On first launch Field will ask where you want to store your Field files (we suggest ~/Documents/FieldWorkspace). It will also check to see if you have Mercurial installed. To start with the [the tutorials](http://openendedgroup.com/field/FieldGATech), simply uncompress them and put them inside your workspace.


### Linux

#### Building Field

To build Field:

	$ git clone git://github.com/OpenEndedGroup/Field.git
	$ cd Field/Contents
	$ ant

That will build the core of Field. Individual plugins have additional targets inside the `build.xml` file. If you want to build them all (and have both Max/MSP and Processing 2.0b1 installed) then `ant extras_all`. For the less bold, you can add individual plugins: `extras_jsrscripting` adds nascant support for other languages (including Clojure and Ruby); `extras_online` adds a plugin that lets you write in-browser JavaScript; `extras_jfbxlib` brings in fbx support and so on. See the `build.xml` file for a list of targets.


#### Starting field

To run:

	$ ./Contents/Linux/field_linux32.sh -field.scratch nameOfFileToOpen.field

or:

	$ ./Contents/Linux/field_linux64.sh -field.scratch nameOfFileToOpen.field

Should you find that Field crashes on startup complaining of "No Handles", you need to install libwebkitgtk-1.0. For example:

	$ sudo apt-get install libwebkitgtk-1.0-0

On first launch Field will ask where you want to store your Field files (we suggest ~/Documents/FieldWorkspace). It will also check to see if you have Mercurial installed. To start with the [the tutorials](http://openendedgroup.com/field/FieldGATech), simply uncompress them and put them inside your workspace.

