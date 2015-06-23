This Eclipse plugin adds the following features to Eclipse:
  * Show the encoding of the current editing text file in the trim area.<br />![current_encoding.png](images/current_encoding.png)
  * Use the [ICU](http://site.icu-project.org/) component to detect the possible encodings of the current editing text file, and alert you to change encoding if the encoding may not be set correctly.<br />![detected_encoding.png](images/detected_encoding.png)
  * Allow you to change encoding through the popup menu.<br />![change_encoding.png](images/change_encoding.png)

I worked in an environment where I needed to access remote files (which may be of UTF-8 or Big5) through the Remote System Explorer, but Eclipse do not detect the file encoding for me and will corrupt the file if I forget to set the file encoding properly, so I wrote this plugin.

If you use Remote System Explorer, you may want to try [My RSE Extensions](http://myrseextensions.sourceforge.net/).
