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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import com.brickmesh.util.Log;

// An "item" is a part in a specific color. This class stores all the items
// that are needed to build the model. The items are stored in a normalized
// form, i.e. there is no guarantee that the when reading back the content
// items will be exactly at the same granularity.
public class RequiredItems {
  // A part that has a specific color and quantity. This is one record out
  // of the many parts needed.
  public static class Item {
    public Item(PartModel.Part part, PartModel.Color color, int count) {
      part_ = part;
      color_ = color;
      count_ = count;
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
        for (Map.Entry<ItemId, Integer> entry : originalIds_.entrySet()) {
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
    
    public Map<ItemId, Integer> originalIds() {
      if (originalIds_ == null) {
        originalIds_ = new TreeMap<ItemId, Integer>();
      }
      return originalIds_;
    }

    public Map<ItemId, Integer> originalIdsOrNull() {
      return originalIds_;
    }

    // The sub-part.
    public PartModel.Part part_;

    // If non-null then the sub-part always comes in this color.
    // Otherwise it inherits the color of the composed part.
    public PartModel.Color color_;

    // How many sub-parts are needed for the composed part. Usually 1.
    public int count_;

    public ItemId itemId() {
      return new ItemId(part_.primaryId(), color_.primaryId());
    }

    // Original ids from the request with the number of elements requested.
    // This is only used to keep track of the original items if an item is
    // not mappable.
    private TreeMap<ItemId, Integer> originalIds_;

  }

  public RequiredItems(PartModel partModel, int sizeHint) {
    partModel_ = partModel;
    reset(sizeHint);
  }

  private RequiredItems() {
  }

  // Adds the given part and color id in the namespace to the items.
  public boolean addItem(
      String namespace, String partId, String colorId, int count,
      UnknownItems unknownItems) {
    List<String> colorIds = Arrays.asList(colorId);
    return addItem(namespace, partId, colorIds, count, unknownItems);
  }

  // Same as above, but it accepts a list of colors. The list must contain
  // at least one element, the rest is used as a hint. It only takes effect
  // the the part is a composite and the number of unbounded children is
  // equal to the number of items in the list.
  public boolean addItem(
      String namespace, String partId, List<String> colorIds, int count,
      UnknownItems unknownItems) {
    if (colorIds.size() < 1) return false;

    String nsPartId = namespace + ":" + partId;
    PartModel.Part part = partModel_.findPartOrNull(nsPartId);

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

    ItemId itemId = new ItemId(nsPartId, nsColorId);
    if (part == null || color == null) {
      if (unknownItems != null) {
        unknownItems.addUnknownItem(itemId, part != null, color != null, count);
      }
      return false;
    }

    addDecomposedItem(part, color, colors, count, itemId, count);
    return true;
  }

  public boolean isEmpty() {
    return items_.size() <= 0;
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
    for (Item item : items_.values()) {
      result += item.part_.weightGrams_ * item.count_;
    }
    return result;
  }

  // Exports an ordered list of items in the namespace. This changes
  // the internal representation so that the items are actually represented
  // in that namespace. A side effect is that the items that are not
  // mappable are moved to unmappableItems().
  public TreeMap<ItemId, Integer> exportToNamespace(String namespace, UnknownItems unknownItems) {
    PartComposer composer = new PartComposer(items_);
    return composer.exportToNamespace(namespace, unknownItems);
  }

  // Access to the items.
  public HashMap<ItemId, Item> items() {
    return items_;
  }

  // Copies everything but originalIds, which would be expensive but not
  // really needed.
  public RequiredItems deepClone() {
    RequiredItems other = new RequiredItems();
    other.partModel_ = partModel_;
    other.items_ = new HashMap<ItemId, Item>(items_.size());
    for (Map.Entry<ItemId, Item> entry : items_.entrySet()) {
      Item item = entry.getValue();
      Item otherItem = new Item(item.part_, item.color_, item.count_);
      other.items_.put(entry.getKey(), otherItem);
    }

    other.numDifferentItems_ = numDifferentItems_;
    other.numTotalItems_ = numTotalItems_;
    return other;
  }

  private void reset(int sizeHint) {
    items_ = new HashMap<ItemId, Item>(sizeHint);
    numDifferentItems_ = 0;
    numTotalItems_ = 0;
  }

  // This class maps items to a namespace and finds the best composition.
  private static class PartComposer {
    public PartComposer(Map<ItemId, Item> allItems) {
      perPartMap_ = new HashMap<String, HashMap<String, Item>>(allItems.size());
      for (Item item : allItems.values()) {
        Item newItem = new Item(item.part_, item.color_, item.count_);
        newItem.originalIds_ = item.originalIds_;
        String partId = newItem.part_.primaryId();
        HashMap<String, Item> items = perPartMap_.get(partId);
        if (items == null) {
          items = new HashMap<String, Item>(16);
          perPartMap_.put(partId, items);
        }
        String colorId = newItem.color_.primaryId();
        if (items.put(colorId, newItem) != null) {
          throw new AssertionError(
              String.format("Item present more than once: %s-%s", partId, colorId));
        }
      }
    }
    
    public TreeMap<ItemId, Integer> exportToNamespace(
        String namespace, UnknownItems unknownItems) {
      TreeMap<ItemId, Integer> result = new TreeMap<ItemId, Integer>();
      if (unknownItems != null) {
        unknownItems.clearUnmappableItems();
      }
      while (perPartMap_.size() > 0) {
        HashMap<String, Item> items = perPartMap_.values().iterator().next();
        Item item = items.values().iterator().next();
        Item bestItem = bestItemForChild(item.part_, item.color_, namespace);
        if (bestItem == null) {
          if (item.originalIdsOrNull() == null) {
            throw new AssertionError("No original ids: " + item);
          }
          if (unknownItems != null) {
            for (Map.Entry<ItemId, Integer> entry : item.originalIds().entrySet()) {
              unknownItems.addUnmappableItem(entry.getKey(), entry.getValue());
            }
          }
          removeItems(item);
          continue;
        }
        ItemId bestItemId = bestItem.itemId();
        Integer count = result.get(bestItemId);
        if (count == null) {
          result.put(bestItemId, bestItem.count_);
        } else {
          result.put(bestItemId, count + bestItem.count_);
        }
        removeItems(bestItem);
      }
      return result;
    }

    private Item bestItemForChild(
        PartModel.Part part, PartModel.Color color, String namespace) {
      Item bestItem = null;
      int bestParentCount = 0;
      if (part.parents_ != null) {
        for (PartModel.Part parent : part.parents_) {
          PartModel.Color childColor = parent.childPartColor(part);
          if (childColor == null) {
            // No specific color set for the child ==> the color of the parent is the same.
            Item parentItem = bestItemForChild(parent, color, namespace);
            if (parentItem != null) {
              int parentCount = parentItem.part_.numPartsInHierarchy() * parentItem.count_;
              if (bestItem == null || parentCount > bestParentCount) {
                bestItem = parentItem;
                bestParentCount = parentCount;
              }
            }
          } else {
            if (childColor != color) continue;

            // The color of the child is set explicitly ==> the parent can be any color.
            // To cover this; we look for other child items in the parent that inherit the
            // parent's color and see if they exist in the itemMap.
            PartModel.Part otherChild = parent.pickChildWithoutColor();
            HashMap<String, Item> otherItems = perPartMap_.get(otherChild.primaryId());
            if (otherItems == null) continue;
            for (Item otherItem : otherItems.values()) {
              PartModel.Color otherColor = otherItem.color_;
              Item parentItem = bestItemForChild(parent, otherColor, namespace);
              if (parentItem != null) {
                int parentCount = parentItem.part_.numPartsInHierarchy() * parentItem.count_;
                if (bestItem == null || parentCount > bestParentCount) {
                  bestItem = parentItem;
                  bestParentCount = parentCount;
                }
              }
            }
          }
        }
      }
      if (bestItem != null) {
        return bestItem;
      }

      if (part.idInNamespace(namespace) != null) {
        int count = maxCountForParent(part, color);
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
    private int maxCountForParent(PartModel.Part part, PartModel.Color color) {
      int countFromSelf = 0;
      HashMap<String, Item> items = perPartMap_.get(part.primaryId());
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
          int countFromChild =
              maxCountForParent(childItem.part_, childItemColor) /
              childItem.count_;
          countFromChildren = Math.min(countFromChildren, countFromChild);
        }
      }
      return countFromSelf + countFromChildren;
    }

    private void removeItems(Item item) {
      int remainingCount = item.count_;
      if (remainingCount == 0) {
        Log.info("Something is wrong. Count is already zero: " + item);
        return;
      }
      HashMap<String, Item> items = perPartMap_.get(item.part_.primaryId());
      Item existingItem = null;
      if (items != null) {
        existingItem = items.get(item.color_.primaryId());
      }
      if (existingItem != null) {
        int count = Math.min(item.count_, existingItem.count_);
        existingItem.count_ -= count;
        if (existingItem.count_ == 0) {
          items.remove(item.color_.primaryId());
          if (items.size() == 0) {
            perPartMap_.remove(item.part_.primaryId());
          }
        }
        remainingCount -= count;
        if (remainingCount == 0) {
          return;
        }
      }
      if (item.part_.items_ == null) {
        Log.info("This is wrong. No children found: " + item);
        return;
      }
      for (PartModel.Item subItem : item.part_.items_) {
        PartModel.Color subColor = subItem.color_ == null ? item.color_ : subItem.color_;
        Item removeItem = new Item(subItem.part_, subColor, remainingCount * subItem.count_);
        removeItems(removeItem);
      }
    }
    
    private HashMap<String, HashMap<String, Item>> perPartMap_;
  }
  
  private void addExactItem(
      PartModel.Part part, PartModel.Color color, int count, ItemId originalId,
      int originalCount) {
    Item item = new Item(part, color, count);
    item.originalIds().put(originalId, originalCount);
    ItemId itemId = item.itemId();
    Item existingItem = items_.get(itemId);
    if (existingItem == null) {
      items_.put(itemId, item);
      ++numDifferentItems_;
    } else {
      existingItem.count_ += item.count_;
      if (item.originalIdsOrNull() != null) {
        for (Map.Entry<ItemId, Integer> entry : item.originalIds().entrySet()) {
          ItemId originalItemId = entry.getKey();
          Integer existingCount = existingItem.originalIds().get(originalItemId);
          if (existingCount == null) {
            existingItem.originalIds().put(originalItemId, entry.getValue());
          } else {
            existingItem.originalIds().put(originalItemId, Math.max(existingCount, entry.getValue()));
          }
        }
      }
    }
    numTotalItems_ += item.count_;
  }

  private void addDecomposedItem(
      PartModel.Part part, PartModel.Color color, PartModel.Color[] colors,
      int count, ItemId originalId, int originalCount) {
    if (part.items_ == null) {
      addExactItem(part, color, count, originalId, originalCount);
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

  // The items that have been mapped successfully.
  private HashMap<ItemId, Item> items_;

  private int numDifferentItems_;
  private int numTotalItems_;

  private PartModel partModel_;
}
