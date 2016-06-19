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

import java.util.TreeMap;

// Maintain unknown items. Used for error reporting.
public class UnknownItems {
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

  public void addUnknownItem(ItemId itemId, boolean partKnown, boolean colorKnown, int count) {
    if (!partKnown) {
      unknownPartIds_ = addMapCount(unknownPartIds_, itemId.partId(), count);
    }
    if (!colorKnown) {
      unknownColorIds_ = addMapCount(unknownColorIds_, itemId.colorId(), count);
    }
    if (!partKnown || !colorKnown) {
      unknownItems_ = addMapCount(unknownItems_, itemId, count);
    }
  }

  public void addUnmappableItem(ItemId itemId, int count) {
    unmappableItems_ = allocMapIfNeeded(unmappableItems_);
    Integer c = unmappableItems_.get(itemId);
    if (c == null) {
      unmappableItems_.put(itemId, count);
    } else {
      unmappableItems_.put(itemId, Math.max(c, count));
    }
  }

  public void clearUnmappableItems() {
    unmappableItems_ = null;
  }

  public boolean isEmpty() {
    return unknownItems_ == null && unmappableItems_ == null;
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

  // Unknown color ids. The entry is the number of items affected.
  private TreeMap<String, Integer> unknownColorIds_;

  // Unknown part ids. The entry is the number of items affected.
  private TreeMap<String, Integer> unknownPartIds_;

  // Unknown items where either color or part id is not understood.
  private TreeMap<ItemId, Integer> unknownItems_;

  // Items that are fully understood but cannot be mapped to the target namespace.
  private TreeMap<ItemId, Integer> unmappableItems_;
}