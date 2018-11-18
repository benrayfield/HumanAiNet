HumanAiNet
Early prototype of a p2p massively multiplayer games and AI research platform for a global sandbox millions of people can build mindbendingly interesting stuff together at gaming low lag - This is benrayfield's main project currently refactoring other projects into.

<nobr><img src=https://github.com/benrayfield/HumanAiNet/raw/master/data/website/rbm2018-4.png height=250>
<img src=https://github.com/benrayfield/HumanAiNet/raw/master/data/website/rbm2018-5.png height=250></nobr>

This is an early prototype of a p2p massively multiplayer games and AI research platform,
including research on neuralnets and computing theory.
The computing theory (TODO include more of those files)
will allow millions of people to safely and efficiently build things together
at gaming-low-lag by sandboxing the number of compute cycles and allocation of memory
and staying within immutable datastructs and functional programming
proven to stay within those limits.
Building games and new kinds of AI while playing them together.
While we may be able to trust 1000 Humans to not write malicious code,
we should not trust a million Humans or any AIs to not write malicious code
on that scale, so the sandboxing raises Dunbars Number,
the scale we can work and play together, in theory.
This is a loosely connected group of projects being refactored from
mutable to immutable, so the top level java packages "mutable" and "immutable".
Theres also a "data" dir parallel to those.
This system is meant to run in a big self extracting executable jar file
for the major updates, including dependencies it puts in data/tempLib etc,
then for minor updates a much smaller executable jar file doubleclicked
after the major update jar doubleclicked in the same place,
both doubleclicked anywhere and it creates a "data" dir beside the jar
or uses the data dir found. Then it would open a window for interactive use.
Eventually there will be no more jar file updates and instead do all updates within
the global sandbox where we can all work and play together at gamingLowLag.
The immutable datastructs will have global merkle secureHashed names
lazyEvaled for efficiency only when needing to copy them across an untrusted border
or to compare them by content in amortized constant time.
An important longterm goal of this project is for it to be forked into
many interoperable projects that use the same or some few p2p networks
and the same immutable functional programming sandbox together,
unlike a browser sandbox between a user and server this is Cross Site (CORS) / p2p.
The Internet should be a bunch of people building mindbendingly interesting stuff together
trying to form open standards in how those things work together instead of
incompatibility wars fueled by business interests to keep us separated and dependent on them.
This project is not to control anyone but to give people tools to work together
with defense against eachothers doings in the network but without needing to control eachother.
Everything will be by fork-edit at single variable granularity
so if you dont like what someone does, dont build on that fork (as your AIs will automatically
do however you want them to, including how they build new AIs and games etc, all in a global sandbox).

OS compatibility:
The parts other than LWJGL are already tested on Linux and work.
In theory will work almost anywhere but for now (2018-11) only the
Windows distribution of LWJGL is included by default. It does have a Linux distro too.
Its important to get it working on Linux before the music tools cuz of lower lag in JSoundCard,
which inTheory can move low lag data between speakers, microphones, GPUs, and CPUs.

Dependencies (included for 64 bit windows, TODO for Linux too):
Java 8+ (not included)
LWJGL2 (for OpenCL optimizations makes things 50 times faster, see mutable.util.OpenclUtil)
Javassist (included)

Required commandline options (in Eclipse forExample):
-Djava.library.path={projectDir}\data\tempLib
Also include to classpath all the jar files in that dir, which are mostly from LWJGL2.

Working Directory (such as in Eclipse):
same as src dir, such as ${workspace_loc:{projectName}/src}

How to start a RBM neuralnet experiment:
mutable.rbm.ui.PaintSlidingVecUi

How to run a debugger on runtime generated java code:
mutable.compilers.JavaCompilers.get(true,true).compile(...see example in RBM.java how to divide the class parts...)
then it pops up an OK button. Before you click it, refresh eclipse file tree, open the class you created, optionally put in a breakpoint, then click OK. Or you can use false params to not pop up the OK andOr use javassist (no debug info in classes but faster) instead of jdk (automatically looks on harddrive for it then calls it). This will normally happen automatically, such as in the mutable.rbm.ui.PaintSlidingVecUi window you type java code to change the learning algorithm while its learning, slightly modified so you dont have to type array indexs as its the body of a doubleloop called after tripleloop in GPU, and if it compiles it works instantly and you see the neural weight continue to change on screen else the code editor rectangle becomes gray until you change it to a valid code.

COMPUTE THEORY (TO BE ORGANIZED INTO THE MAIN PROJECT):
The compute theory parts will mostly be in "immutable" java package,
especially immutable.binufnode.
BIN means binary forest. UFnode means Unified Forest node.
There was a more json-like form but its planned to instead
derive that from the binary forest form or some combo of them.
There may be some RBM turingMachine research in the mutable parts
especially with tree based topologies.
