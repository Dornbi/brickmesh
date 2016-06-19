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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

final class SorterTest extends TestCase {
  public static void main(String[] args) {
    testQuickSort();
    testQuickSelect();
  }

  private static class ReverseIntSortable implements Sorter.Sortable {
    ReverseIntSortable(int[] array) {
      array_ = array;
    }

    public int size() {
      return array_.length;
    }

    public boolean before(int i1, int i2) {
      return array_[i1] > array_[i2];
    }

    public void swap(int i1, int i2) {
      int temp = array_[i1];
      array_[i1] = array_[i2];
      array_[i2] = temp;
    }

    public String toString() {
      return Arrays.toString(array_);
    }

    private final int[] array_;
  }

  private static void testQuickSort() {
    for (int iter = 0; iter < 100; ++iter) {
      int numElems = 20;
      int[] a = new int[numElems];
      for (int i = 0; i < numElems; ++i) {
        a[i] = i + 100;
      }
      Random r = new Random(iter);
      Sorter.Sortable sortable = new ReverseIntSortable(a);
      shuffle(sortable, r);
      Sorter.quicksort(sortable, r);
      for (int i = 0; i < numElems; ++i) {
        expectTrue(a[i] == numElems - i + 99);
      }
    }
  }

  private static void testQuickSelect() {
    for (int iter = 0; iter < 100; ++iter) {
      int numElems = 20;
      int numSelected = 10;
      int[] a = new int[numElems];
      for (int i = 0; i < numElems; ++i) {
        a[i] = i + 100;
      }
      Random r = new Random(iter);
      Sorter.Sortable sortable = new ReverseIntSortable(a);
      shuffle(sortable, r);
      Sorter.quickselect(sortable, numSelected, r);
      HashSet<Integer> allNumbers = new HashSet<Integer>();
      for (int i = 0; i < numSelected; ++i) {
        expectTrue(a[i] >= numSelected + 100);
        allNumbers.add(a[i]);
      }
      for (int i = numSelected; i < numElems; ++i) {
        expectTrue(a[i] < numSelected + 100);
        allNumbers.add(a[i]);
      }
      expectTrue(allNumbers.size() == numElems);
    }
  }

  private static void shuffle(Sorter.Sortable sortable, Random r) {
    for (int i = 0; i < sortable.size() * 10; ++i) {
      int i1 = r.nextInt(sortable.size());
      int i2 = r.nextInt(sortable.size());
      if (i1 != i2) {
        sortable.swap(i1, i2);
      }
    }
  }
}