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

// An "item" is a part with a color that is in a model.
//
// An ItemId consists of:
// - colorId: an arbitrary id that should be in the color id space.
// - partId: an arbitrary id that should be in the part id space.
//
// The ids should point to an existing color and part, but there is
// no guarantee that those actually exist.
//
// This class is immutable.
public final class ItemId implements Comparable {
  public static String[] idPiecesOrNull(String id) {
    String[] split = id.split(":");
    if (split.length != 2) return null;
    return split;
  }
  
  public ItemId(String partId, String colorId) {
    partId_ = partId;
    colorId_ = colorId;
  }
  
  public String colorId() {
    return colorId_;
  }

  public String partId() {
    return partId_;
  }
  
  public boolean equals(Object other) {
    ItemId itemId = (ItemId)other;
    return colorId_.equals(itemId.colorId_) && partId_.equals(itemId.partId_);
  } 
    
  public int hashCode() {
    final int PRIME = 31;
    return colorId_.hashCode() * PRIME + partId_.hashCode();
  }
  
  public int compareTo(Object other) {
    ItemId itemId = (ItemId)other;
    int result = partId_.compareTo(itemId.partId_);
    if (result != 0) return result;
    return colorId_.compareTo(itemId.colorId_);
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(partId_);
    sb.append('-');
    sb.append(colorId_);
    return sb.toString();
  }
  
  private String partId_;
  private String colorId_;
}
