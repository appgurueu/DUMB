![DUMB logo](https://raw.githubusercontent.com/appgurueu/DUMB/master/res/DUMB_300x200_nomargin.png)

# DUMB

[DUMB](https://github.com/appgurueu/DUMB) is a standalone Java application.
DUMB's main purpose is to fix PDFs exported with SMART notebook software. Additionally, documents can be converted without fixing them.
Made by Lars Müller aka appgurueu and licensed under the MIT license.

# Requirements

* Java Runtime Environment (JRE) 8 or higher
* "Apache OpenOffice or LibreOffice; the latest stable version is usually recommended" - [for jodconverter](https://github.com/sbraconnier/jodconverter/wiki/System-Requirements)

Note that some conversions may take long (like PDF to FODG) as the files which need to be fixed are flawed and bloated.

# Execution

[Download the JAR](https://raw.githubusercontent.com/appgurueu/DUMB/master/build/libs/DUMB-alpha-all.jar) and then execute it using `java -jar DUMB.jar {arguments}`.

## Command

Arguments are passed as a sequence of `<key> <value>`-pairs. Possible keys are:

* `operation`: Operation to execute - `fix`/`convert`
* `source`: source file (note: extension needs to match file type)
* `destination`: destination file (again, extension has to match type)
* `officehome`: where to find Libre Office binaries
* `background`: `fill`/`kill`/`margin`, `width`: in cm, `margin`: in cm, `margin-left`, `margin-right`, `margin-top`, `margin-bottom`: properties for the fixed document

If the arguments are invalid, the GUI is spawned.

### Examples

Convert a PDF into a FODG:

```bash
java -jar DUMB.jar operation convert source "/home/user/folder/my doc.pdf" destination "/home/user/folder/my doc.fodg"
```

Fix a PDF and replace it by the fixed one:

```bash
java -jar DUMB.jar operation fix source "/home/user/folder/my doc.pdf" destination "/home/user/folder/my doc.pdf"
```

Additionally, kill the background, set a width of 30cm & a margin of 1cm:

```bash
java -jar DUMB.jar operation fix source "/home/user/folder/my doc.pdf" destination "/home/user/folder/my doc.pdf" background kill width 30 margin 1
```

Open the GUI:

```bash
java -jar DUMB.jar operation gui or literally everything with a different format (also zero arguments)
```
