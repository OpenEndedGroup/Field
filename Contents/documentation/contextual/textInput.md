The Code Editor
===============

In Field code resides in boxes. The editor is where you edit this code and by default the language of Field is Python.

Some helpful keyboard short-cuts:

   -   _command-return_ executes the current line or the currently text selection
   -   _command-0_ executes everything in the box (just as if you selected everything and pressed command-return)
   -   _command-left-arrow_ executes everything in the outermost "execution area". Areas appear automatically in the margin to record selections that have been executed previously. Right click on them for more options

By default you are editing the code that stored in the property "python_source". But you can edit other properties here to ---- see the popup box at the top (command-+ will cycle between them). Automatically executed things and GLSLang shader sources are stored here for example. If you activate the WrapInTransform plugin, you'll be able to change the langage of this text editor (a drop-down box will be added to the top of the editor).

Finally, some useful peculularites of Field's code editor. Firstly, you can embed [GUI elements into it](http://localhost:10010/field/EmbeddingGui) (sliders, color pickers, graphs and even other programming languages) --- check out the right click menu. Secondly, the undo stack for the editor is kept separately for each box (it's also saved with the document).

Complete documentation for the text editor [starts here](http://localhost:10010/field/TextEditor).
