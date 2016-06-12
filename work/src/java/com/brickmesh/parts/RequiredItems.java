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
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import com.brickmesh.util.Log;

public class RequiredItems {
  // A part that has a specific color and quantity. This is one record out
  // of the many parts needed.
  public static class Item {
    public Item(PartModel.Part part, PartModel.Color color, int count) {
      part_ = part;
      color_ = color;
      count_ = count;
    }
    
    // The sub-part.
    public PartModel.Part part_;
    
    // If non-null then the sub-part always comes in this color.
    // Otherwise it inherits the color of the composed part.
    public PartModel.Color color_;

    // How many sub-parts are needed for the composed part. Usually 1.
    public int count_;
    
    // Original ids from the request with the number of elements requested.
    // This is only used to keep track of the original items if an item is
    // not mappable.
    public HashMap<ItemId, Integer> originalIds_;
    
    public boolean equals(Item other) {
      if (part_ != other.part_) return false;
      if (color_ != other.color_) return false;
      if (count_ != other.count_) return false;
      if (!originalIds_.equals(other.originalIds_)) return false;
      return true;
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(part_.primaryId());
      sb.append(",color=");
      sb.append(color_.primaryId());
      sb.append(",count=");
      sb.append(count_);
      if (originalIds_ != null) {
        sb.append(",originalIds=(");
        boolean first = true;
        for (HashMap.Entry<ItemId, Integer> entry : originalIds_.entrySet()) {
          if (!first) sb.append(',');
          sb.append(entry.getKey().toString());
          sb.append(":");
          sb.append(entry.getValue());
          first = false;
        }
        sb.append(")");
      }
      return sb.toString();
    }
  }

  public RequiredItems(PartModel partModel, int numPartsHint) {
    partModel_ = partModel;
    items_ = new HashMap<String, HashMap<String, Item>>(numPartsHint);
  }
  
  // Adds the given part and color id in the namespace to the items.
  public boolean addDecomposed(String namespace, String partId, String colorId, int count) {
    List<String> colorIds = Arrays.asList(colorId);
    return addDecomposed(namespace, partId, colorIds, count);
  }
  
  // Same as above, but it accepts a list of colors. The list must contain
  // at least one element, the rest is used as a hint. It only takes effect
  // the the part is a composite and the number of unbounded children is
  // equal to the number of items in the list.
  public boolean addDecomposed(String namespace, String partId, List<String> colorIds, int count) {
    if (colorIds.size() < 1) return false;
    
    String nsPartId = namespace + ":" + partId;
    PartModel.Part part = partModel_.findPartOrNull(nsPartId);
    if (part == null) {
      unknownPartIds_ = addMapCount(unknownPartIds_, nsPartId, count);
    }

    PartModel.Color[] colors = new PartModel.Color[colorIds.size()];
    PartModel.Color color = null;
    String nsColorId = null;
    for (int i = 0; i < colorIds.size(); ++i) {
      String id = namespace + ":" + colorIds.get(i);
      PartModel.Color c = partModel_.findColorOrNull(id);
      if (i == 0) {
        color = c;
        nsColorId = id;
      }
      if (c == null) {
        colors = null;
        break;
      }
      colors[i] = c;
    }

    if (color == null) {
      unknownColorIds_ = addMapCount(unknownColorIds_, nsColorId, count);
    }
    
    ItemId itemId = new ItemId(nsPartId, nsColorId);
    if (part == null || color == null) {
      unknownItems_ = addMapCount(unknownItems_, itemId, count);
      return false;
    }
    addDecomposedItem(part, color, colors, count, itemId, count);
    return true;
  }

  public boolean isEmpty() {
    if (items_.size() > 0) return false;
    if (unknownItems_ != null && unknownItems_.size() > 0) return false;
    if (unmappableItems_ != null && unmappableItems_.size() > 0) return false;
    return true;
  }
  
  public TreeMap<String, Integer> unknownColorIdsOrNull() {
    return unknownColorIds_;
  }

  public TreeMap<String, Integer> unknownPartIdsOrNull() {
    return unknownPartIds_;
  }

  public TreeMap<ItemId, Integer> unknownItemsOrNull() {
    return unknownItems_;
  }

  public TreeMap<ItemId, Integer> unmappableItemsOrNull() {
    return unmappableItems_;
  }

  // Number of different items. This is approximate as the numbers may
  // change with items being composed or decomposed.
  public int numDifferentItems() {
    return numDifferentItems_;
  }
  
  // Number of total items. This is approximate as the numbers may
  // change with items being composed or decomposed.
  public int numTotalItems() {
    return numTotalItems_;
  }
  
  public double weightEstimateGrams() {
    double result = 0.0;
    for (HashMap<String, Item> items : items_.values()) {
      for (Item item : items.values()) {
        result += item.part_.weightGrams_ * item.count_;
      }
    }
    return result;
  }
    
  // Exports an ordered list of items in the namespace. This changes
  // the internal representation so that the items are actually represented
  // in that namespace. A side effect is that the items that are not
  // mappable are moved to unmappableItems().
  public TreeMap<ItemId, Integer> exportToNamespace(String namespace) {
    if (composedToNamespace_ == null) {
      composeToNamespace(namespace);
      composedToNamespace_ = namespace;
    } else if (!composedToNamespace_.equals(namespace)) {
      throw new IllegalStateException("Already composed to namespace: " +
          composedToNamespace_);
    }
    TreeMap<ItemId, Integer> result = new TreeMap<ItemId, Integer>();
    for (HashMap<String, Item> items : items_.values()) {
      for (Item item : items.values()) {
        String partId = item.part_.idInNamespace(namespace);
        if (partId == null) {
          throw new AssertionError("No id for part: " + item);
        }
        String colorId = item.color_.idInNamespace(namespace);
        if (colorId == null) {
          throw new AssertionError("No id for color: " + item);
        }
        result.put(new ItemId(partId, colorId), item.count_);
      }
    }
    return result;
  }
  
  // Access to the internal representation. This should only be used
  // for testing & debugging.
  public HashMap<String, HashMap<String, Item>> items() {
    return items_;
  }
  
  private void reset(int numPartsHint) {
    items_ = new HashMap<String, HashMap<String, Item>>(numPartsHint);
    numDifferentItems_ = 0;
    numTotalItems_ = 0;
  }

  private void composeToNamespace(String namespace) {
    HashMap<String, HashMap<String, Item>> oldMap = items_;
    reset(oldMap.size());
    while (oldMap.size() > 0) {
      HashMap<String, Item> items = oldMap.values().iterator().next();
      Item item = items.values().iterator().next();
      Item bestItem = bestItemForChild(oldMap, item.part_, item.color_, namespace);
      if (bestItem == null) {
        if (item.originalIds_ == null) {
          throw new AssertionError("No original ids: " + item);
        }
        for (HashMap.Entry<ItemId, Integer> entry : item.originalIds_.entrySet()) {
          ItemId itemId = entry.getKey();
          if (unmappableItems_ == null) {
            unmappableItems_ = new TreeMap<ItemId, Integer>();
          }
          Integer count = unmappableItems_.get(itemId);
          if (count == null) {
            unmappableItems_.put(itemId, entry.getValue());
          } else {
            unmappableItems_.put(itemId, Math.max(count, entry.getValue()));
          }
        }
        removeItems(oldMap, item);
        continue;
      }
      addItem(bestItem);
      removeItems(oldMap, bestItem);
    }
  }
  
  private static void removeItems(
      HashMap<String, HashMap<String, Item>> allItems, Item item) {
    int remainingCount = item.count_;
    if (remainingCount == 0) {
      Log.info("Something is wrong. Count is already zero: " + item);
      return;
    }
    HashMap<String, Item> items = allItems.get(item.part_.primaryId());
    if (items != null) {
      Item existingItem = items.get(item.color_.primaryId());
      if (existingItem != null) {
        int count = Math.min(item.count_, existingItem.count_);
        existingItem.count_ -= count;
        if (existingItem.count_ == 0) {
          items.remove(item.color_.primaryId());
          if (items.size() == 0) {
            allItems.remove(item.part_.primaryId());
          }
        }
        remainingCount -= count;
        if (remainingCount == 0) {
          return;
        }
      }
    }
    if (item.part_.items_ == null) {
      Log.info("This is wrong. No children found: " + item);
      return;
    }
    for (PartModel.Item subItem : item.part_.items_) {
      PartModel.Color subColor = subItem.color_ == null ? item.color_ : subItem.color_;
      Item removeItem = new Item(subItem.part_, subColor, remainingCount * subItem.count_);
      removeItems(allItems, removeItem);
    }
  }

  private void addItem(Item item) {
    HashMap<String, Item> items = items_.get(item.part_.primaryId());
    if (items == null) {
      items = new HashMap<String, Item>();
      items_.put(item.part_.primaryId(), items);
    }
    Item existingItem = items.get(item.color_.primaryId());
    if (existingItem == null) {
      items.put(item.color_.primaryId(), item);
      ++numDifferentItems_;
    } else {
      existingItem.count_ += item.count_;
      if (item.originalIds_ != null) {
        if (existingItem.originalIds_ == null) {
          existingItem.originalIds_ = new HashMap<ItemId, Integer>(item.originalIds_.size());
        }
        for (HashMap.Entry<ItemId, Integer> entry : item.originalIds_.entrySet()) {
          ItemId itemId = entry.getKey();
          Integer existingCount = existingItem.originalIds_.get(itemId);
          if (existingCount == null) {
            existingItem.originalIds_.put(itemId, entry.getValue());
          } else {
            existingItem.originalIds_.put(itemId, Math.max(existingCount, entry.getValue()));
          }
        }
      }
    }
    numTotalItems_ += item.count_;
  }

  private void addItem(
      PartModel.Part part, PartModel.Color color, int count, ItemId originalId,
      int originalCount) {
    Item item = new Item(part, color, count);
    item.originalIds_ = new HashMap<ItemId, Integer>(1);
    item.originalIds_.put(originalId, originalCount);
    addItem(item);
  }
  
  private void addDecomposedItem(
      PartModel.Part part, PartModel.Color color, PartModel.Color[] colors,
      int count, ItemId originalId, int originalCount) {
    if (part.items_ == null) {
      addItem(part, color, count, originalId, originalCount);
    } else {
      int idx = -1;
      if (colors != null && colors.length == part.numChildrenWithoutColor()) {
        idx = 0;
      }
      for (PartModel.Item subItem : part.items_) {
        if (subItem.color_ == null) {
          if (idx >= 0) {
            for (int i = 0; i < subItem.count_; ++i) {
              addDecomposedItem(subItem.part_, colors[idx], null, count,
                  originalId, originalCount);
              ++idx;
            }
          } else {
            addDecomposedItem(subItem.part_, color, null, count * subItem.count_,
                originalId, originalCount);
          }
        } else {
          addDecomposedItem(subItem.part_, subItem.color_, null, count * subItem.count_,
              originalId, originalCount);
        }
      }
    }
  }

  private static Item bestItemForChild(
      HashMap<String, HashMap<String, Item>> allItems,
      PartModel.Part part, PartModel.Color color, String namespace) {
    Item bestItem = null;
    if (part.parents_ != null) {
      for (PartModel.Part parent : part.parents_) {
        PartModel.Color childColor = parent.childPartColor(part);
        if (childColor == null) {
          // No specific color set for the child ==> the color of the parent is the same.
          Item parentItem = bestItemForChild(allItems, parent, color, namespace);
          if (parentItem != null && (bestItem == null || parentItem.count_ > bestItem.count_)) {
            bestItem = parentItem;
          }
        } else {
          if (childColor != color) continue;
          
          // The color of the child is set explicitly ==> the parent can be any color.
          // To cover this; we look for other child items in the parent that inherit the
          // parent's color and see if they exist in the itemMap.
          PartModel.Part otherChild = parent.pickChildWithoutColor();
          HashMap<String, Item> otherItems = allItems.get(otherChild.primaryId());
          if (otherItems == null) continue;
          for (Item otherItem : otherItems.values()) {
            PartModel.Color otherColor = otherItem.color_;
            Item parentItem = bestItemForChild(allItems, parent, otherColor, namespace);
            if (parentItem != null && (bestItem == null || parentItem.count_ > bestItem.count_)) {
              bestItem = parentItem;
            }
          }
        }
      }
    }
    if (bestItem != null) {
      return bestItem;
    }
        
    if (part.idInNamespace(namespace) != null) {
      int count = maxCountForParent(allItems, part, color);
      if (count > 0) {
        Item item = new Item(part, color, count);
        return item;
      }
    }
    return null;
  }

  // For a particular item, computes the maximum number of items that
  // can be used to satisfy the needs considering the full child hierarchy
  // of the item. The item is described by its part and color; the needs
  // by itemNeeds.
  private static int maxCountForParent(
      HashMap<String, HashMap<String, Item>> allItems,
      PartModel.Part part, PartModel.Color color) {
    int countFromSelf = 0;
    HashMap<String, Item> items = allItems.get(part.primaryId());
    if (items != null) {
      Item item = items.get(color.primaryId());
      if (item != null) {
        countFromSelf = item.count_;
      }
    }
    if (part.items_ == null) {
      return countFromSelf;
    }

    int countFromChildren = 0;
    if (part.items_ != null) {
      countFromChildren = Integer.MAX_VALUE;
      for (PartModel.Item childItem : part.items_) {
        PartModel.Color childItemColor =
            childItem.color_ == null ? color : childItem.color_;
        int countFromChild = maxCountForParent(
            allItems, childItem.part_, childItemColor) / childItem.count_;
        countFromChildren = Math.min(countFromChildren, countFromChild);
      }
    }
    return countFromSelf + countFromChildren;
  }
  
  private static <K> TreeMap<K, Integer> addMapCount(TreeMap<K, Integer> map, K key, int count) {
    map = allocMapIfNeeded(map);
    Integer c = map.get(key);
    if (c == null) {
      map.put(key, count);
    } else {
      map.put(key, count + c);
    }
    return map;
  }
  
  private static <K> TreeMap<K, Integer> allocMapIfNeeded(TreeMap<K, Integer> map) {
    if (map == null) {
      return new TreeMap<K, Integer>();
    } else {
      return map;
    }
  }  
  
  // The items that have been mapped successfully.
  private HashMap<String, HashMap<String, Item>> items_;
  
  private int numDifferentItems_;
  private int numTotalItems_;
  private String composedToNamespace_;

  // Unknown color ids. The entry is the number of items affected.
  private TreeMap<String, Integer> unknownColorIds_;

  // Unknown part ids. The entry is the number of items affected.
  private TreeMap<String, Integer> unknownPartIds_;

  // Unknown items where either color or part id is not understood.
  private TreeMap<ItemId, Integer> unknownItems_;
  
  // Items that are fully understood in the model but are not mappable
  // to the target namespace, for example because there there is no
  // combination that represents this color.
  private TreeMap<ItemId, Integer> unmappableItems_;
  
  private PartModel partModel_;
}
