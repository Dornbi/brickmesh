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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.brickmesh.util.TestCase;

class RequiredItemsTest extends TestCase {
  public static void main(String[] args) {
    new RequiredItemsTest().testSimpleWithUnknowns();
    new RequiredItemsTest().testSimpleDecompose();
    new RequiredItemsTest().testMinifigDecompose();
    new RequiredItemsTest().testHierarchyDecompose();
    new RequiredItemsTest().testVirtualParts();
    new RequiredItemsTest().testNonExistentVirtualParts();
  }
  
  public RequiredItemsTest() {
    partModel_ = PartModel.getModel();
    expectedItems_ = new HashMap<String, HashMap<String, RequiredItems.Item>>();
  }
    
  private void testSimpleWithUnknowns() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    expectTrue(actual.addDecomposed("l", "3005", "1", 3));
    expectFalse(actual.addDecomposed("l", "3005", "nocolor", 5));
    expectFalse(actual.addDecomposed("l", "nopart", "1", 2));
    addExpectedItem("l:3005", "l:1", 3, new ItemId("l:3005", "l:1"), 3);
    expectedUnknownColorIds_ = new TreeMap<String, Integer>();
    addExpectedUnknown(expectedUnknownColorIds_, "l:nocolor", 5);
    expectedUnknownPartIds_ = new TreeMap<String, Integer>();
    addExpectedUnknown(expectedUnknownPartIds_, "l:nopart", 2);
    expectedUnknownItems_ = new TreeMap<ItemId, Integer>();
    addExpectedUnknown(expectedUnknownItems_, new ItemId("l:3005", "l:nocolor"), 5);
    addExpectedUnknown(expectedUnknownItems_, new ItemId("l:nopart", "l:1"), 2);
    expectActual(actual);
    expectEquals(1, actual.numDifferentItems());
    expectEquals(3, actual.numTotalItems());
    expectEquals(1.29, actual.weightEstimateGrams());

    expectedItems_.clear();
    addExpectedItem("l:3005", "b:1", 3, null, 0);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }

  private void testSimpleDecompose() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    expectTrue(actual.addDecomposed("l", "73983", "1", 2));
    addExpectedItem("b:2429", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    addExpectedItem("b:2430", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    expectActual(actual);
    expectEquals(2, actual.numDifferentItems());
    expectEquals(4, actual.numTotalItems());
    expectEquals(1.64, actual.weightEstimateGrams());

    expectedItems_.clear();
    addExpectedItem("b:2429c01", "b:1", 2, null, 0);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }

  private void testMinifigDecompose() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    List<String> colors = Arrays.asList("5", "5", "5", "24", "24");
    expectTrue(actual.addDecomposed("l", "76382", colors, 1));
    addExpectedItem("b:973", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addExpectedItem("b:981", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addExpectedItem("b:982", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addExpectedItem("b:983", "b:3", 2, new ItemId("l:76382", "l:5"), 1);
    expectActual(actual);
    expectEquals(4, actual.numDifferentItems());
    expectEquals(5, actual.numTotalItems());
    expectEquals(1.45, actual.weightEstimateGrams());

    expectedItems_.clear();
    addExpectedItem("b:973c67", "b:2", 1, null, 0);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }
  
  private void testHierarchyDecompose() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    expectTrue(actual.addDecomposed("l", "76320", "40", 1));
    addExpectedItem("b:32181", "b:12", 1, new ItemId("l:76320", "l:40"), 1);
    addExpectedItem("b:32182", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addExpectedItem("b:32040", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addExpectedItem("b:32183", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addExpectedItem("b:108", "b:22", 1, new ItemId("l:76320", "l:40"), 1);
    expectActual(actual);
    expectEquals(5, actual.numDifferentItems());
    expectEquals(5, actual.numTotalItems());
    expectEquals(4.29, actual.weightEstimateGrams());

    expectedItems_.clear();
    addExpectedItem("b:32181c02", "b:12", 1, null, 0);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }

  private void testVirtualParts() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    List<String> colors = Arrays.asList("26", "42");
    expectTrue(actual.addDecomposed("l", "60797", colors, 1));
    addExpectedItem("v:60797-1", "b:11", 1, new ItemId("l:60797", "l:26"), 1);
    addExpectedItem("v:60797-2", "b:15", 1, new ItemId("l:60797", "l:26"), 1);
    expectActual(actual);
    expectEquals(2, actual.numDifferentItems());
    expectEquals(2, actual.numTotalItems());
    expectEquals(3.2, actual.weightEstimateGrams());

    expectedItems_.clear();
    addExpectedItem("b:60797c01", "b:11", 1, null, 0);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }

  private void testNonExistentVirtualParts() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    List<String> colors = Arrays.asList("26", "43");
    expectTrue(actual.addDecomposed("l", "60797", colors, 1));
    addExpectedItem("v:60797-1", "b:11", 1, new ItemId("l:60797", "l:26"), 1);
    addExpectedItem("v:60797-2", "b:14", 1, new ItemId("l:60797", "l:26"), 1);
    expectActual(actual);
    expectEquals(2, actual.numDifferentItems());
    expectEquals(2, actual.numTotalItems());
    expectEquals(3.2, actual.weightEstimateGrams());

    expectedItems_.clear();
    expectedUnmappableItems_ = new TreeMap<ItemId, Integer>();
    expectedUnmappableItems_.put(new ItemId("l:60797", "l:26"), 1);
    TreeMap<ItemId, Integer> expected = extractExpected();
    TreeMap<ItemId, Integer> exported = actual.exportToNamespace("b");
    expectEquals(exported, expected);
    expectActual(actual);
  }
  
  private void expectActual(RequiredItems actual) {
    expectEquals(expectedItems_, actual.items());
    expectEquals(expectedUnknownColorIds_, actual.unknownColorIdsOrNull());
    expectEquals(expectedUnknownPartIds_, actual.unknownPartIdsOrNull());
    expectEquals(expectedUnknownItems_, actual.unknownItemsOrNull());
    expectEquals(expectedUnmappableItems_, actual.unmappableItemsOrNull());
  }
  
  private void addExpectedItem(String partId, String colorId, int count,
      ItemId originalId, int originalCount) {
    PartModel.Color color = partModel_.findColorOrNull(colorId);
    PartModel.Part part = partModel_.findPartOrNull(partId);
    RequiredItems.Item item = new RequiredItems.Item(part, color, count);
    if (originalId != null) {
      if (item.originalIds_ == null) {
        item.originalIds_ = new HashMap<ItemId, Integer>();
      }
      item.originalIds_.put(originalId, originalCount);
    }
    HashMap<String, RequiredItems.Item> items = new HashMap<String, RequiredItems.Item>();
    items.put(color.primaryId(), item);
    expectedItems_.put(part.primaryId(), items);
  }

  private TreeMap<ItemId, Integer> extractExpected() {
    TreeMap<ItemId, Integer> extracted = new TreeMap<ItemId, Integer>();
    for (HashMap<String, RequiredItems.Item> items : expectedItems_.values()) {
      for (RequiredItems.Item item : items.values()) {
        extracted.put(new ItemId(item.part_.primaryId(), item.color_.primaryId()), item.count_);
      }
    }
    return extracted;
  }

  private static <K> void addExpectedUnknown(Map<K, Integer> map, K key, int count) {
    Integer c = map.get(key);
    if (c == null) {
      map.put(key, count);
    } else {
      map.put(key, c + count);
    }
  }
  
  private PartModel partModel_;
  private HashMap<String, HashMap<String, RequiredItems.Item>> expectedItems_;
  private TreeMap<String, Integer> expectedUnknownColorIds_;
  private TreeMap<String, Integer> expectedUnknownPartIds_;
  private TreeMap<ItemId, Integer> expectedUnknownItems_;
  private TreeMap<ItemId, Integer> expectedUnmappableItems_;
};