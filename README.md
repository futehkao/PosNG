# PosNG

PosNG is a framework aim to provide ISO 8583 processing capabilities.  It consists of two modules: Progency and PosNg.

## Progeny
Progency is a module that is a fork of jPOS 1.5.3 in order to avoid AGPL license imposed by later jPOS versions.  In addition, Progeny has changed the build system from Ant to Gradle and upgraded it's external libraries dependencies to the most recent versions.  For example, PosNG uses jdom2 instead of jdom.

Some jPOS modules are not ported over:
* iso.filter
* log4j
* space
* util.FSDMsg

Enhancements
* Gradle is the build system.
* Libraries are upgraded to recent versions, e.g. from jdom to jdom2.
* Third party libraries can all be retrieved from Maven.
* Replaced Vector with List when appropriate.
* Replace Hashtable with HashMap when appropriate.  

In order to prevent conflicts from current jPOS libraries and confusions with the jPOS project, all packages in the project have been renamed to start with com.futeh.progeny, as opposed to org.jpos.

The license file for jPOS is included, as required by jPOS 1.5.3, in the jar file under the licenses/jPOS class resources path.

As a reminder, this is derivative work from jPOS.  The modifications to each file are minimal.  The copyright notice and the original change logs associated with each file are left unchanged.  However, in the future, bug fixes and removals of deprecated code will proceed.  Consequently, it will continue to diverge from the original jPOS distributions.

## PosNG
It is a complete redesign of ISO 8583 processing.  


