scala-notebook
==============

A more friendly, browser-based interactive Scala prompt (REPL).

Based on the IPython notebook project, this project will let you interact with Scala in a browser window,
which has the following advantages over the standard REPL:

* Easy to view and edit past commands
* Commands can return HTML or images, allowing richer interactivity (charts, for example)
* Notebooks can be saved and loaded, providing a bridge between interactive REPL and classes in a project
* Supports mixing Scala expressions and markdown, letting you create rich, interactive documents similar to Mathematica

While I think this tool will be helpful for everyone using Scala, I expect it to be particularly valuable for the
scientific and analytics community.

Technically, my vision is as follows:

First, leverage the work of the IPython project by pretty much using their front-end as-is, at least as a starting point.
If you're going to work on this, you should clone IPython as a reference. This worked for me in Windows:
* Install Python and setuptools

* Install the ipython runtime (to easily get the dependencies)
 easy_install ipython
 easy_install ipython[zmq]
 easy_install tornado

Now, you can run "python ipython.py notebook" from the ipython git folder.

This project is based on the IPython master branch as of around May 2012, which has a very different look from the released version at that time.

A little about how IPython works:

* Uses tornado templates to serve up the content. I chose Scalata/ssp as this is most similar to tornado
* Uses JQuery and JSon for things like loading & saving notebooks
* Uses Websockets for two-way asynchronous communication with the python kernel; which is essentially a running JVM.

Having the web server process separate from the process doing the evaluation is also important in Scala; we want to separate
the user's actions from the web server, allowing a restart of the client process.

To that end, the project is organized as follows:
* *server* is the web server
* *client* is the shim for the client process (linked into whatever client classes are available to the user)
* *common* are the classes shared by both


