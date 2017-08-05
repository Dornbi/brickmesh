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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.brickmesh.util.TestCase;

public class RequiredItemsTest extends TestCase {
  public static void main(String[] args) {
    runAllTests(RequiredItemsTest.class);
  }

  public RequiredItemsTest() {
    partModel_ = PartModel.getModel();
    expectedItems_ = new HashMap<ItemId, RequiredItems.Item>();
    expectedUnknownItems_ = new UnknownItems();
    unknownItems_ = new UnknownItems();
  }

  public void testAddEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectEquals(0, items.numUniqueItems());
    expectEquals(0, items.numTotalItems());
    expectEquals(0.0, items.weightEstimateGrams());
    expectItems(items);
  }

  public void testExportEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap());
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet());
  }

  public void testMinusEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);

    RequiredItems minusEmpty = items.minusMatches(createItemMap());
    expectEquals(minusEmpty.exportToNamespace("b", null), createItemMap());
    expectEquals(0, minusEmpty.numUniqueItems());
    expectEquals(0, minusEmpty.numTotalItems());
    expectEquals(0.0, minusEmpty.weightEstimateGrams());

    RequiredItems minusNonExisting = items.minusMatches(
        createItemMap(new ItemId("l:3005", "l:1"), 1));
    expectEquals(minusNonExisting.exportToNamespace("b", null), createItemMap());
    expectEquals(0, minusNonExisting.numUniqueItems());
    expectEquals(0, minusNonExisting.numTotalItems());
    expectEquals(0.0, minusNonExisting.weightEstimateGrams());
  }

  public void testAddSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));
    expectFalse(items.addItem("l", "3005", "nocolor", 5, unknownItems_));
    expectFalse(items.addItem("l", "nopart", "1", 2, unknownItems_));
    addItem(expectedItems_, "l:3005", "l:1", 3, new ItemId("l:3005", "l:1"), 3);
    expectedUnknownItems_.addUnknownItem(new ItemId("l:3005", "l:nocolor"), true, false, 5);
    expectedUnknownItems_.addUnknownItem(new ItemId("l:nopart", "l:1"), false, true, 2);
    expectItems(items);
    expectEquals(1, items.numUniqueItems());
    expectEquals(3, items.numTotalItems());
    expectEquals(1.29, items.weightEstimateGrams());
  }

  public void testExportSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap(new ItemId("b:3005", "b:1"), 3));
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet(new ItemId("b:3005", "b:1")));
  }

  public void testMinusSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));

    RequiredItems minusEmpty = items.minusMatches(createItemMap());
    expectEquals(minusEmpty.exportToNamespace("b", null), createItemMap(
        new ItemId("b:3005", "b:1"), 3));
    expectEquals(1, minusEmpty.numUniqueItems());
    expectEquals(3, minusEmpty.numTotalItems());
    expectEquals(1.29, minusEmpty.weightEstimateGrams());

    RequiredItems minusSome = items.minusMatches(createItemMap(
        new ItemId("b:3005", "b:1"), 1));
    expectEquals(minusSome.exportToNamespace("b", null), createItemMap(
        new ItemId("b:3005", "b:1"), 2));
    expectEquals(1, minusSome.numUniqueItems());
    expectEquals(2, minusSome.numTotalItems());
    expectEquals(0.86, minusSome.weightEstimateGrams());

    RequiredItems minusAll = items.minusMatches(createItemMap(
        new ItemId("b:3005", "b:1"), 3));
    expectEquals(minusAll.exportToNamespace("b", null), createItemMap());
    expectEquals(0, minusAll.numUniqueItems());
    expectEquals(0, minusAll.numTotalItems());
    expectEquals(0.0, minusAll.weightEstimateGrams());

    RequiredItems minusMore = items.minusMatches(createItemMap(
        new ItemId("b:3005", "b:1"), 4));
    expectEquals(minusMore.exportToNamespace("b", null), createItemMap());
    expectEquals(0, minusMore.numUniqueItems());
    expectEquals(0, minusMore.numTotalItems());
    expectEquals(0.0, minusMore.weightEstimateGrams());
  }

  public void testAddDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));
    addItem(expectedItems_, "b:2429", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    addItem(expectedItems_, "b:2430", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    expectItems(items);
    expectEquals(2, items.numUniqueItems());
    expectEquals(4, items.numTotalItems());
    expectEquals(1.64, items.weightEstimateGrams());
  }

  public void testExportDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap(new ItemId("b:2429c01", "b:1"), 2));
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet(
        new ItemId("b:2429c01", "b:1"),
        new ItemId("b:2429", "b:1"),
        new ItemId("b:2430", "b:1")));
  }

  public void testMinusDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));

    RequiredItems minusSubpart = items.minusMatches(createItemMap(
        new ItemId("b:2429", "b:1"), 1));
    expectEquals(minusSubpart.exportToNamespace("b", null), createItemMap(
        new ItemId("b:2429c01", "b:1"), 1,
        new ItemId("b:2430", "b:1"), 1));
    expectEquals(2, minusSubpart.numUniqueItems());
    expectEquals(3, minusSubpart.numTotalItems());
    expectEquals(1.23, minusSubpart.weightEstimateGrams());

    RequiredItems minusFullpart = items.minusMatches(createItemMap(
        new ItemId("b:2429c01", "b:1"), 1));
    expectEquals(minusFullpart.exportToNamespace("b", null), createItemMap(
        new ItemId("b:2429c01", "b:1"), 1));
    expectEquals(2, minusFullpart.numUniqueItems());
    expectEquals(2, minusFullpart.numTotalItems());
    expectEquals(0.82, minusFullpart.weightEstimateGrams());
  }

  public void testAddMinifig() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("5", "5", "5", "24", "24"),
        1, unknownItems_));
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("21", "21", "21", "24", "24"),
        2, unknownItems_));
    addItem(expectedItems_, "b:973", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addItem(expectedItems_, "b:973", "b:5", 2, new ItemId("l:76382", "l:21"), 2);
    addItem(expectedItems_, "b:981", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addItem(expectedItems_, "b:981", "b:5", 2, new ItemId("l:76382", "l:21"), 2);
    addItem(expectedItems_, "b:982", "b:2", 1, new ItemId("l:76382", "l:5"), 1);
    addItem(expectedItems_, "b:982", "b:5", 2, new ItemId("l:76382", "l:21"), 2);
    addItem(expectedItems_, "b:983", "b:3", 2, new ItemId("l:76382", "l:5"), 1);
    addItem(expectedItems_, "b:983", "b:3", 4, new ItemId("l:76382", "l:21"), 2);
    expectItems(items);
    expectEquals(7, items.numUniqueItems());
    expectEquals(15, items.numTotalItems());
    expectEquals(4.35, items.weightEstimateGrams());
  }

  public void testExportMinifig() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("5", "5", "5", "24", "24"),
        1, unknownItems_));
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("21", "21", "21", "24", "24"),
        2, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap(
        new ItemId("b:973c67", "b:2"), 1,
        new ItemId("b:973c02", "b:5"), 2));
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingMinifig() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("5", "5", "5", "24", "24"),
        1, unknownItems_));
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("21", "21", "21", "24", "24"),
        2, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectTrue(actual.contains(new ItemId("b:973", "b:2")));
    expectTrue(actual.contains(new ItemId("b:973", "b:5")));
    expectTrue(actual.contains(new ItemId("b:973c01", "*")));
    expectTrue(actual.contains(new ItemId("b:973c02", "*")));
    expectTrue(actual.contains(new ItemId("b:981", "b:2")));
    expectTrue(actual.contains(new ItemId("b:981", "b:5")));
    expectTrue(actual.contains(new ItemId("b:982", "b:2")));
    expectTrue(actual.contains(new ItemId("b:982", "b:5")));
    expectTrue(actual.contains(new ItemId("b:983", "b:3")));
    expectTrue(actual.contains(new ItemId("b:983", "b:3")));
  }

  public void testMinusMinifig() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("5", "5", "5", "24", "24"),
        1, unknownItems_));
    expectTrue(items.addItem(
        "l", "76382",
        Arrays.asList("21", "21", "21", "24", "24"),
        2, unknownItems_));

    RequiredItems minusArm = items.minusMatches(createItemMap(
        new ItemId("b:981", "b:2"), 1));
    expectEquals(minusArm.exportToNamespace("b", null), createItemMap(
        new ItemId("b:973", "b:2"), 1,
        new ItemId("b:982", "b:2"), 1,
        new ItemId("b:983", "b:3"), 2,
        new ItemId("b:973c02", "b:5"), 2));
    expectEquals(6, minusArm.numUniqueItems());
    expectEquals(14, minusArm.numTotalItems());

    RequiredItems minusArms = items.minusMatches(createItemMap(
        new ItemId("b:981", "b:5"), 2));
    expectEquals(minusArms.exportToNamespace("b", null), createItemMap(
        new ItemId("b:973", "b:5"), 2,
        new ItemId("b:982", "b:5"), 2,
        new ItemId("b:983", "b:3"), 4,
        new ItemId("b:973c67", "b:2"), 1));
    expectEquals(6, minusArms.numUniqueItems());
    expectEquals(13, minusArms.numTotalItems());

    RequiredItems minusOther = items.minusMatches(createItemMap(
        new ItemId("b:973c01", "b:2"), 1));
    // This is an artifact of the exporter algorithm - it picks b:973c67
    // which does not actually exist in b:5.
    expectEquals(minusOther.exportToNamespace("b", null), createItemMap(
        new ItemId("b:981", "b:5"), 1,
        new ItemId("b:982", "b:5"), 1,
        new ItemId("b:973c02", "b:5"), 1,
        new ItemId("b:973c67", "b:5"), 1));
    expectEquals(6, minusOther.numUniqueItems());
    expectEquals(12, minusOther.numTotalItems());

    RequiredItems minusManyOther = items.minusMatches(createItemMap(
        new ItemId("b:973c01", "b:2"), 10));
    expectEquals(minusManyOther.exportToNamespace("b", null), createItemMap(
        new ItemId("b:973", "b:5"), 2,
        new ItemId("b:981", "b:2"), 1,
        new ItemId("b:981", "b:5"), 2,
        new ItemId("b:982", "b:2"), 1,
        new ItemId("b:982", "b:5"), 2));
    expectEquals(5, minusManyOther.numUniqueItems());
    expectEquals(8, minusManyOther.numTotalItems());
  }

  public void testAddHierarchy() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));
    addItem(expectedItems_, "b:32181", "b:12", 1, new ItemId("l:76320", "l:40"), 1);
    addItem(expectedItems_, "b:32182", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addItem(expectedItems_, "b:32040", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addItem(expectedItems_, "b:32183", "b:11", 1, new ItemId("l:76320", "l:40"), 1);
    addItem(expectedItems_, "b:108", "b:22", 1, new ItemId("l:76320", "l:40"), 1);
    expectItems(items);
    expectEquals(5, items.numUniqueItems());
    expectEquals(5, items.numTotalItems());
    expectEquals(4.29, items.weightEstimateGrams());
  }

  public void testExportHierarchy() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap(new ItemId("b:32181c02", "b:12"), 1));
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingHierarchy() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet(
        new ItemId("b:32181", "b:12"),
        new ItemId("b:32181c02", "*"),
        new ItemId("b:32181c03", "*"),
        new ItemId("b:32182", "b:11"),
        new ItemId("b:32040", "b:11"),
        new ItemId("b:32183", "b:11"),
        new ItemId("b:32183c01", "b:11"),
        new ItemId("b:108", "b:22")));
  }

  public void testMinusHierarchy() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));

    RequiredItems minusOther = items.minusMatches(createItemMap(
        new ItemId("b:32181c03", "b:12"), 1));
    expectEquals(minusOther.exportToNamespace("b", null), createItemMap(
        new ItemId("b:108", "b:22"), 1));
    expectEquals(1, minusOther.numUniqueItems());
    expectEquals(1, minusOther.numTotalItems());
  }

  public void testAddVirtualParts() {
    RequiredItems actual = new RequiredItems(partModel_, 10);
    expectTrue(actual.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));
    addItem(expectedItems_, "v:60797-1", "b:11", 1, new ItemId("l:60797", "l:26"), 1);
    addItem(expectedItems_, "v:60797-2", "b:15", 1, new ItemId("l:60797", "l:26"), 1);
    expectItems(actual);
    expectEquals(2, actual.numUniqueItems());
    expectEquals(2, actual.numTotalItems());
    expectEquals(3.2, actual.weightEstimateGrams());
  }

  public void testExportVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap(new ItemId("b:60797c01", "b:11"), 1));
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet(
        new ItemId("b:60797c01", "*"),
        new ItemId("b:60797c02", "b:11"),
        new ItemId("b:60797c03", "b:11")));
  }

  public void testMinusVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));

    RequiredItems minusVirtual = items.minusMatches(createItemMap(
        new ItemId("b:60797c03", "b:11"), 1));
    
    // The remainder contains only virtual parts that cannot be exported.
    addItem(expectedItems_, "v:60797-2", "b:15", 1, null, 0);
    expectItems(minusVirtual);
    expectEquals(1, minusVirtual.numUniqueItems());
    expectEquals(1, minusVirtual.numTotalItems());
    
    HashSet<ItemId> interesting = minusVirtual.interestingItems("b");
    expectEquals(interesting, createItemSet(
        new ItemId("b:60797c01", "*")));
  }

  public void testAddNonExistentVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "43"), 1, unknownItems_));
    addItem(expectedItems_, "v:60797-1", "b:11", 1, new ItemId("l:60797", "l:26"), 1);
    addItem(expectedItems_, "v:60797-2", "b:14", 1, new ItemId("l:60797", "l:26"), 1);
    expectItems(items);
    expectEquals(2, items.numUniqueItems());
    expectEquals(2, items.numTotalItems());
    expectEquals(3.2, items.weightEstimateGrams());
  }

  public void testExportNonExistentVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "43"), 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(actual, createItemMap());
    expectedUnknownItems_.addUnmappableItem(new ItemId("l:60797", "l:26"), 1);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void testInterestingNonExistentVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "43"), 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(actual, createItemSet(
        new ItemId("b:60797c01", "b:11"),
        new ItemId("b:60797c02", "b:11"),
        new ItemId("b:60797c03", "b:11")));
  }

  public void expectItems(RequiredItems items) {
    expectEquals(expectedItems_, items.items());
    expectEquals(
        expectedUnknownItems_.unknownColorIdsOrNull(),
        unknownItems_.unknownColorIdsOrNull());
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  public void addItem(HashMap<ItemId, RequiredItems.Item> map,
      String partId, String colorId, int count,
      ItemId originalId, int originalCount) {
    PartModel.Color color = partModel_.findColorOrNull(colorId);
    PartModel.Part part = partModel_.findPartOrNull(partId);
    ItemId itemId = new ItemId(part.primaryId(), color.primaryId());
    RequiredItems.Item existing = map.get(itemId);
    if (existing == null) {
      RequiredItems.Item item = new RequiredItems.Item(part, color, count);
      if (originalId != null) {
        item.originalIds().put(originalId, originalCount);
      }
      map.put(item.itemId(), item);
    } else {
      existing.count_ += count;
      existing.originalIds().put(originalId, originalCount);
    }
  }

  public static HashSet<ItemId> createItemSet(Object... keysValues) {
    HashSet<ItemId> result = new HashSet<ItemId>(keysValues.length);
    for (int i = 0; i < keysValues.length; ++i) {
      ItemId key = (ItemId)keysValues[i];
      result.add(key);
    }
    return result;
  }

  public static HashMap<ItemId, Integer> createItemMap(Object... keysValues) {
    if (keysValues.length % 2 != 0) {
      throw new IllegalArgumentException(
          "createMap() should be invoked with an even number of args.");
    }
    HashMap<ItemId, Integer> result = new HashMap<ItemId, Integer>(keysValues.length / 2);
    for (int i = 0; i < keysValues.length; i += 2) {
      ItemId key = (ItemId)keysValues[i];
      Integer value = (Integer)keysValues[i + 1];
      result.put(key, value);
    }
    return result;
  }

  private PartModel partModel_;
  private HashMap<ItemId, RequiredItems.Item> expectedItems_;
  private UnknownItems expectedUnknownItems_;
  private UnknownItems unknownItems_;
};
