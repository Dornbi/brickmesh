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

class RequiredItemsTest extends TestCase {
  public static void main(String[] args) {
    new RequiredItemsTest().testAddEmpty();
    new RequiredItemsTest().testExportEmpty();
    new RequiredItemsTest().testInterestingEmpty();
    new RequiredItemsTest().testAddSimple();
    new RequiredItemsTest().testExportSimple();
    new RequiredItemsTest().testInterestingSimple();
    new RequiredItemsTest().testAddSimpleDecompose();
    new RequiredItemsTest().testExportSimpleDecompose();
    new RequiredItemsTest().testInterestingSimpleDecompose();
    new RequiredItemsTest().testAddMinifigDecompose();
    new RequiredItemsTest().testExportMinifigDecompose();
    new RequiredItemsTest().testInterestingMinifigDecompose();
    new RequiredItemsTest().testAddHierarchyDecompose();
    new RequiredItemsTest().testExportHierarchyDecompose();
    new RequiredItemsTest().testInterestingHierarchyDecompose();
    new RequiredItemsTest().testAddVirtualParts();
    new RequiredItemsTest().testExportVirtualParts();
    new RequiredItemsTest().testInterestingVirtualParts();
    new RequiredItemsTest().testAddNonExistentVirtualParts();
    new RequiredItemsTest().testExportNonExistentVirtualParts();
    new RequiredItemsTest().testInterestingNonExistentVirtualParts();
  }

  public RequiredItemsTest() {
    partModel_ = PartModel.getModel();
    expectedItems_ = new HashMap<ItemId, RequiredItems.Item>();
    expectedUnknownItems_ = new UnknownItems();
    unknownItems_ = new UnknownItems();
    expectedExported_ = new TreeMap<ItemId, Integer>();
    expectedInteresting_ = new HashSet<ItemId>();
  }

  private void testAddEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectEquals(0, items.numUniqueItems());
    expectEquals(0, items.numTotalItems());
    expectEquals(0.0, items.weightEstimateGrams());
    expectItems(items);
  }

  private void testExportEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    TreeMap<ItemId, Integer> exported = items.exportToNamespace("b", unknownItems_);
    expectEquals(expectedExported_, exported);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingEmpty() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    HashSet<ItemId> actual = items.interestingItems("b");
    expectEquals(expectedInteresting_, actual);
  }

  private void testAddSimple() {
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

  private void testExportSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectedExported_.put(new ItemId("b:3005", "b:1"), 3);
    expectEquals(expectedExported_, actual);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingSimple() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "3005", "1", 3, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectedInteresting_.add(new ItemId("b:3005", "b:1"));
    expectEquals(expectedInteresting_, actual);
  }

  private void testAddSimpleDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));
    addItem(expectedItems_, "b:2429", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    addItem(expectedItems_, "b:2430", "b:1", 2, new ItemId("l:73983", "l:1"), 2);
    expectItems(items);
    expectEquals(2, items.numUniqueItems());
    expectEquals(4, items.numTotalItems());
    expectEquals(1.64, items.weightEstimateGrams());
  }

  private void testExportSimpleDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectedExported_.put(new ItemId("b:2429c01", "b:1"), 2);
    expectEquals(expectedExported_, actual);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingSimpleDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "73983", "1", 2, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectedInteresting_.add(new ItemId("b:2429c01", "b:1"));
    expectedInteresting_.add(new ItemId("b:2429", "b:1"));
    expectedInteresting_.add(new ItemId("b:2430", "b:1"));
    expectEquals(expectedInteresting_, actual);
  }

  private void testAddMinifigDecompose() {
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

  private void testExportMinifigDecompose() {
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
    expectedExported_.put(new ItemId("b:973c67", "b:2"), 1);
    expectedExported_.put(new ItemId("b:973c02", "b:5"), 2);
    expectEquals(expectedExported_, actual);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingMinifigDecompose() {
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

  private void testAddHierarchyDecompose() {
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

  private void testExportHierarchyDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectedExported_.put(new ItemId("b:32181c02", "b:12"), 1);
    expectEquals(expectedExported_, actual);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingHierarchyDecompose() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem("l", "76320", "40", 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectedInteresting_.add(new ItemId("b:32181", "b:12"));
    expectedInteresting_.add(new ItemId("b:32181c02", "*"));
    expectedInteresting_.add(new ItemId("b:32181c03", "*"));
    expectedInteresting_.add(new ItemId("b:32182", "b:11"));
    expectedInteresting_.add(new ItemId("b:32040", "b:11"));
    expectedInteresting_.add(new ItemId("b:32183", "b:11"));
    expectedInteresting_.add(new ItemId("b:32183c01", "b:11"));
    expectedInteresting_.add(new ItemId("b:108", "b:22"));
    expectEquals(expectedInteresting_, actual);
  }

  private void testAddVirtualParts() {
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

  private void testExportVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectedExported_.put(new ItemId("b:60797c01", "b:11"), 1);
    expectEquals(expectedExported_, actual);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "42"), 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectedInteresting_.add(new ItemId("b:60797c01", "*"));
    expectedInteresting_.add(new ItemId("b:60797c02", "b:11"));
    expectedInteresting_.add(new ItemId("b:60797c03", "b:11"));
    expectEquals(expectedInteresting_, actual);
  }

  private void testAddNonExistentVirtualParts() {
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

  private void testExportNonExistentVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "43"), 1, unknownItems_));

    TreeMap<ItemId, Integer> actual = items.exportToNamespace("b", unknownItems_);
    expectEquals(expectedExported_, actual);
    expectedUnknownItems_.addUnmappableItem(new ItemId("l:60797", "l:26"), 1);
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void testInterestingNonExistentVirtualParts() {
    RequiredItems items = new RequiredItems(partModel_, 10);
    expectTrue(items.addItem(
        "l", "60797", Arrays.asList("26", "43"), 1, unknownItems_));

    HashSet<ItemId> actual = items.interestingItems("b");
    expectedInteresting_.add(new ItemId("b:60797c01", "b:11"));
    expectedInteresting_.add(new ItemId("b:60797c02", "b:11"));
    expectedInteresting_.add(new ItemId("b:60797c03", "b:11"));
    expectEquals(expectedInteresting_, actual);
  }

  private void expectItems(RequiredItems items) {
    expectEquals(expectedItems_, items.items());
    expectEquals(
        expectedUnknownItems_.unknownColorIdsOrNull(),
        unknownItems_.unknownColorIdsOrNull());
    expectEquals(
        expectedUnknownItems_.unmappableItemsOrNull(),
        unknownItems_.unmappableItemsOrNull());
  }

  private void addItem(HashMap<ItemId, RequiredItems.Item> map,
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

  private PartModel partModel_;
  private HashMap<ItemId, RequiredItems.Item> expectedItems_;
  private UnknownItems expectedUnknownItems_;
  private UnknownItems unknownItems_;
  private TreeMap<ItemId, Integer> expectedExported_;
  private HashSet<ItemId> expectedInteresting_;
};