emacsIDEAs
==========

Porting some great extensions of emacs to Intellij IDEA. such as AceJump, CopyWithoutSelection.



This is introduction to plugin emacsIDEAs of Intellij IDEA.

* normal jump demo:      **C-L char**

* jump to line end:      **C-L space**

   type space to show line end.

* Jump and Copy:         **C-L char x marker_char**

   after markers show up, type 'x' to copy jump area.

* Jump and Paste:        **C-L char p marker_char**

   after markers show up, type 'p' to paste clipboard contents to jump target position.

* Jump and Cut:          **C-L char x marker_char**

   after markers show up, type x before marker_char to cut jump area.

* during jump, type ESC to exit.
