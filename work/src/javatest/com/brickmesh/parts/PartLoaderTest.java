/*
Copyright (c) 2016, Peter Dornbach
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name BrickMesh nor the names of its contributors may be used
      to endorse or promote products derived from this software without
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.brickmesh.parts;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.TreeMap;

import com.brickmesh.util.TestCase;

class PartLoaderTest extends TestCase {
  public static void main(String[] args)
      throws IOException, PartLoader.LoaderException {
    testLoadEmptyLxf();
    testLoadSimpleLxf();
    testLoadWanted();
  }

  private static void testLoadEmptyLxf()
      throws IOException, PartLoader.LoaderException {
    PartLoader.LxfLoader loader = new PartLoader().createLxfLoader(
        PartLoader.Options.createUnlimited());
    loader.parse(new FileInputStream("src/testdata/test-empty.lxf"));
    PartLoader.Result result = loader.getResult();
    expectTrue(result != null);
    expectTrue(result.isEmpty());
    RequiredItems items = result.items_;
    UnknownItems unknownItems = result.unknownItems_;
    expectTrue(items != null);
    expectEquals(0, items.numTotalItems());
    expectEquals(0, items.numDifferentItems());
    expectEquals(null, unknownItems.unknownPartIdsOrNull());
    expectEquals(null, unknownItems.unknownPartIdsOrNull());
    expectEquals(null, unknownItems.unknownColorIdsOrNull());

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", null);
    TreeMap<ItemId, Integer> expected = new TreeMap<ItemId, Integer>();
    expectEquals(actual, expected);
  }

  private static void testLoadSimpleLxf()
      throws IOException, PartLoader.LoaderException {
    PartLoader.LxfLoader loader = new PartLoader().createLxfLoader(
        PartLoader.Options.createUnlimited());
    loader.parse(new FileInputStream("src/testdata/test-simple.lxf"));
    PartLoader.Result result = loader.getResult();
    expectTrue(!result.isEmpty());
    RequiredItems items = result.items_;
    UnknownItems unknownItems = result.unknownItems_;
    expectEquals(null, unknownItems.unknownPartIdsOrNull());
    expectEquals(null, unknownItems.unknownColorIdsOrNull());
    expectTrue(result.imageBytes_ != null);

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", null);
    TreeMap<ItemId, Integer> expected = new TreeMap<ItemId, Integer>();
    expected.put(new ItemId("b:3005", "b:5"), 3);
    expected.put(new ItemId("b:6019", "b:103"), 1);
    expectEquals(actual, expected);
  }

  private static void testLoadWanted()
      throws IOException, PartLoader.LoaderException {
    PartLoader.WantedLoader loader = new PartLoader().createWantedLoader(
        PartLoader.Options.createUnlimited());
    loader.parse(new FileInputStream("src/testdata/wanted.xml"));
    PartLoader.Result result = loader.getResult();
    expectTrue(!result.isEmpty());
    RequiredItems items = result.items_;
    UnknownItems unknownItems = result.unknownItems_;
    expectEquals(null, unknownItems.unknownPartIdsOrNull());
    expectEquals(null, unknownItems.unknownColorIdsOrNull());
    expectEquals(null, result.imageBytes_);

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", null);
    TreeMap<ItemId, Integer> expected = new TreeMap<ItemId, Integer>();
    expected.put(new ItemId("b:3004", "b:6"), 1);
    expected.put(new ItemId("b:3023", "b:5"), 4);
    expectEquals(actual, expected);
  }
};