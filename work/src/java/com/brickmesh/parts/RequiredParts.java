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

public final class RequiredParts {
  public static final int DEFAULT_ITEMS = 128;

  // Required parts will be capped at this value. 
  public static final int MAX_QTY_PER_ITEM = 10000;
  
  public RequiredParts() {
    quantity_ = new int[DEFAULT_ITEMS];
    itemIdx_ = new int[DEFAULT_ITEMS];
    partGroups_ = new PartModel.PartGroup[DEFAULT_ITEMS];
    preferredParts_ = new PartModel.Part[DEFAULT_ITEMS];
    color_ = new PartModel.Color[DEFAULT_ITEMS];
  }
  
  public void addItem(PartModel.PartGroup partGroup, PartModel.Part preferredPart,
      PartModel.Color color, int quantity) {
    if (numItems_ == itemIdx_.length) {
      growItems();
    }
    quantity = Math.min(quantity, MAX_QTY_PER_ITEM);
    quantity_[numItems_] = quantity;
    itemIdx_[numItems_] = numQty_;
    partGroups_[numItems_] = partGroup;
    preferredParts_[numItems_] = preferredPart;
    color_[numItems_] = color;
    totalQty_ += quantity;
    ++numQty_;
    ++numItems_;
  }
  
  public float weightEstimateGrams() {
    float weight = 0.0f;
    for (int i = 0; i < numQty_; ++i) {
      PartModel.Part part = preferredParts_[itemIdx_[i]];
      weight += quantity_[i] * part.weightGrams_;
    }
    return weight;
  }
  
  private void growItems() {
    int itemLen2 = itemIdx_.length * 2;
    quantity_ = Arrays.copyOf(quantity_, itemLen2);
    itemIdx_ = Arrays.copyOf(itemIdx_, itemLen2);
    partGroups_ = Arrays.copyOf(partGroups_, itemLen2);
    preferredParts_ = Arrays.copyOf(preferredParts_, itemLen2);
    color_ = Arrays.copyOf(color_, itemLen2);
  }
  
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    for (int i = 0; i < numQty_; ++i) {
      if (i > 0) {
        buf.append(", ");
      }
      int idx = itemIdx_[i];
      buf.append(String.format("%s-%d: %d", preferredParts_[idx], color_[idx],
          quantity_[i]));
    }
    buf.append("]");
    return buf.toString();
  }
  
  // The total number of parts still needed.
  public int totalQty_;

  // The number of different items still needed.
  public int numQty_;
  
  // Quantity for each item still needed.
  // [0..numQty_ + reserve]
  public int[] quantity_;
  
  // The index of the item in itemIds_ and color_.
  // [0..numQty_ + reserve]
  public int[] itemIdx_;
  
  // The total number of items (including the ones that are already found.)
  public int numItems_;
  
  // The PartGroup that is needed.
  // [0..numItems_ + reserve]
  public PartModel.PartGroup[] partGroups_;
  
  // If there is a preferred item within the group, this is set here.
  // [0..numItems_ + reserve]
  public PartModel.Part[] preferredParts_;

  // Color of the item.
  // [0..numItems_ + reserve]
  public PartModel.Color[] color_;
}