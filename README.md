## Field

Field is an open-source software project initiated by OpenEndedGroup, for the creation of their digital artworks.

It is an environment for writing code to rapidly and experimentally assemble and explore algorithmic systems.

It is _visual_, it is _code-based_, it is _hybrid_.

We think it has something to offer a diverse range of programmers and artists.

Field is developed and tested on Mac OS X (primarily) and Ubuntu 12 + Nvidia proprietary drivers.

*Our main documentation website is: http://openendedgroup.com/field*

### MacOS X

#### Building Field

Before anything else, ensure you have Xcode installed.  Field won't build without it.  You'll also need a JDK 1.7 (you can run Field on 1.8, but you need 1.7 to build it); and `ant` installed. 

To build Field with Java 7 (as supplied by Apple):

	$ cd /Applications
	$ git clone git://github.com/OpenEndedGroup/Field.git
	$ mv Field Field.app
	$ mkdir -p Field.app/Contents/Plugins/1.7.0.jdk/Contents
	$ cd Field.app/Contents/Plugins/1.7.0.jdk/Contents
	$ ln -s /System/Library/Frameworks/JavaVM.framework/Home
	$ cd ../../..
	$ JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home/ ant

This will build the core of Field. Individual plugins have additional targets inside the `Contents/build.xml` file.

If you want to build them all (and have both Max/MSP and Processing 2.0b1 installed) then:

	$ ant extras_all

For the less bold, you can add individual plugins:

* `$ ant extras_jsrscripting` - adds nascant support for other languages (including Clojure and Ruby)
* `$ ant extras_online` - adds a plugin for writing in-browser JavaScript
* `$ ant extras_jfbxlib` - adds fbx support

... and so on. For the complete list, see the `Contents/build.xml` file.


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

On first launch Field will ask where to store your Field files (we suggest ~/Documents/FieldWorkspace).

To start with [the tutorials](http://openendedgroup.com/field/FieldGATech), simply uncompress them and put them inside your workspace.


### Linux

#### Building Field

To build Field (you can run Field on 1.8, but you need 1.7 to build it);

	$ git clone git://github.com/OpenEndedGroup/Field.git
	$ cd Field/Contents
	$ ant

This will build the core of Field. Individual plugins have additional targets inside the `Contents/build.xml` file.

If you want to build them all (and have both Max/MSP and Processing 2.0b1 installed) then:

	$ ant extras_all

For the less bold, you can add individual plugins:

* `$ ant extras_jsrscripting` - adds nascant support for other languages (including Clojure and Ruby)
* `$ ant extras_online` - adds a plugin for writing in-browser JavaScript
* `$ ant extras_jfbxlib` - adds fbx support

... and so on. For the complete list, see the `Contents/build.xml` file.


#### Starting field

To run:

	$ ./Contents/linux/field_linux32.sh -field.scratch nameOfFileToOpen.field

or:

	$ ./Contents/linux/field_linux64.sh -field.scratch nameOfFileToOpen.field

On first launch Field will ask where to store your Field files (we suggest ~/Documents/FieldWorkspace), and it will check to see if you have Mercurial installed.

To start with [the tutorials](http://openendedgroup.com/field/FieldGATech), simply uncompress them and put them inside your workspace.

#### Common Problems

Should you find that Field crashes on startup complaining of "No Handles", you need to install libwebkitgtk-1.0. For example:

	$ sudo apt-get install libwebkitgtk-1.0-0

Should you find that Field crashes on startup inside libsoup (https://bugs.eclipse.org/bugs/show_bug.cgi?id=400626#c4), you'll need to use:

	$ ./Contents/linux/field_linux64_mozilla.sh -field.scratch nameOfFileToOpen.field
	
to launch Field instead (this may disable the internal webbrowser). 

Should you find that Field doesn't seem to be drawing its UI correctly (black or missing boxes) then you are almost certainly using buggy graphics card drivers on Linux. Field is developed with the latest Nvidia proprietary drivers and generally requires a correctly functioning OpenGL 3.2+ stack.




