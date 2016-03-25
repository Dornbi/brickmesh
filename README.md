# BrickMesh

BrickMesh is a tool to order real LEGO® bricks for your digital models. It supports LEGO Digital Designer models and turns them into a BrickLink wanted list. You can use BrickLink to find the best shops for the list.

This is the command line version of the tool; you can use the online version at
http://www.brickmesh.com without installing anything.

# Features

It contains:
* It can read LEGO® Digital Designer (LDD) files.
* It can output BrickLink wanted lists.
* A comprehensive (but not complete) "part model" that contains information about
  LEGO® parts, similar parts and a mapping to BrickLink parts.
  As a special feature it also estimates the weight of your model.
* The weight estimates are mostly based on LDraw geometries of parts. This
  is roughly correct but can be quite off sometimes.
* A limited number of frequently used parts have been measured on a digital scale.

# Installation

## Pre-requisites

To compile your own version, you need:
* Java SE version 1.7 or later.
* Python 2.7 is also used to build the part model.
* You also need Google Protocol Buffers. The code was tested with 3.0.0beta2.
  You can install it from:
  https://github.com/google/protobuf
* In order to recompute the weight estimates you also need an LDraw part library.

## Compiling the tools

If everything is properly installed, you can try to compile everything:
```sh
./build-parts.sh
```

You can run LddTool like this:
```sh
./run-lddtool.sh ../lxf/1.70_ar234.lxf

...
<INVENTORY>
 <ITEM>
  <ITEMTYPE>P</ITEMTYPE>
  <ITEMID>2431</ITEMID>
  <COLOR>80</COLOR>
  <MINQTY>1</MINQTY>
  <CONDITION>N</CONDITION>
  <NOTIFY>N</NOTIFY>
 </ITEM>
 <ITEM>
  <ITEMTYPE>P</ITEMTYPE>
  <ITEMID>3005</ITEMID>
  ...
 </ITEM>
 ...
</INVENTORY>
Estimated weight: 81.342 gram(s)
```

This will recompile everything upon every invocation.

## Recomputing weights based on LDraw

```sh
./run-offline.sh ExtractWeights assets/part-model.txt > weight-list.csv
./run-offline.sh ComputeLDrawWeights weight-list <ldraw-path> > new-ldraw-weights.csv
cp new-ldraw-weights.csv src/model/weights-ldraw.txt
```