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

package com.brickmesh.util;

import java.util.Random;

// Quicksort and Quickselect. Unlike the java native sort
// implementation this allows sorting of structs flattened
// into multiple primitive type arrays.
public final class Sorter {
  // Quicksort and Quickselect work on this interface.
  public interface Sortable {
    // Number of elements.
    int size();
    
    // Should the element at idx1 be before the element at idx2?
    boolean before(int idx1, int idx2);
    
    // Swaps the two elements.
    void swap(int idx1, int idx2);
  }
  
  public static void quicksort(Sortable sortable, Random rand) {
    quicksort(sortable, 0, sortable.size() - 1, rand);
  }

  // Simple single-pivot quicksort. Good enough until proven otherwise.
  public static void quicksort(Sortable sortable, int start,
      int end, Random rand) {
    if (end - start < 1) return;
    int pivot = start + rand.nextInt(end - start + 1);
    int left = start;
    int right = end;
    while (left <= right) {
      while (sortable.before(left, pivot)) {
        ++left;
      }
      while (sortable.before(pivot, right)) {
        --right;
      }
      if (left <= right) {
        // This is a slight inefficienty of the Sortable interface.
        // Since we only have the index of the pivot, we need to make
        // sure it follows swaps.
        if (left == pivot) {
          pivot = right;
        } else if (right == pivot) {
          pivot = left;
        }
        
        sortable.swap(left, right);
        ++left;
        --right;
      }
    }
    quicksort(sortable, start, right, rand);
    quicksort(sortable, left, end, rand);
  }

  public static void quickselect(Sortable sortable, int n, Random rand) {
    if (n >= sortable.size()) {
      return;
    }
    int left = 0;
    int right = sortable.size() - 1;
    while (right >= left) {
      int pivot = partition(sortable, left, right,
          left + rand.nextInt(right - left + 1));
      if (n == pivot) {
        return;
      } else if (n < pivot) {
        right = pivot - 1; 
      } else {
        left = pivot + 1;
      }
    }
    throw new AssertionError("Invalid index");
  }
  
  private static int partition(Sortable sortable, int left, int right,
      int pivot) {
    sortable.swap(pivot, right);
    pivot = right;
    int storeIdx = left;
    for (int i = left; i < right; ++i) {
      if (!sortable.before(pivot, i)) {
        sortable.swap(storeIdx, i);
        ++storeIdx;
      }
    }
    sortable.swap(right, storeIdx);  // Move pivot to its final place
    return storeIdx;
  }
}
