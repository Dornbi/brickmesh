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

import java.util.ArrayList;
import java.util.Random;

final class SorterBenchmark extends Benchmark {
  private static class Reverse2IntSortable implements Sorter.Sortable {
    Reverse2IntSortable(int[] array, int[] array2) {
      array_ = array;
      array2_ = array2;
      if (array2.length != array.length) {
        throw new IllegalArgumentException("Arrays must be the same length: " +
            array.length + ", " + array2.length);
      }
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
      int temp2 = array2_[i1];
      array2_[i1] = array2_[i2];
      array2_[i2] = temp2;
    }
    
    private final int[] array_;
    private final int[] array2_;
  }

  public static void main(String args[]) {
    long phaseStartNanos = System.nanoTime();
    benchmarkIntArray(2000000);
    phaseStartNanos = printRuntimeStats("int[] array", phaseStartNanos);
    benchmarkIntArrayList(2000000);
    phaseStartNanos = printRuntimeStats("ArrayList", phaseStartNanos);
    benchmarkQuickSelect(100000);
    phaseStartNanos = printRuntimeStats("QuickSelect", phaseStartNanos);
    benchmarkQuickSort(100000);
    phaseStartNanos = printRuntimeStats("QuickSort", phaseStartNanos);
  }
  
  private static long benchmarkIntArray(int size) {
    int[] array = new int[size];
    for (int i = 0; i < size; ++i) {
      array[i] = i;
    }
    long sum = 0;
    for (int i = 0; i < size; ++i) {
      sum += array[i];
    }
    return sum;
  }

  private static long benchmarkIntArrayList(int size) {
    final ArrayList<Integer> arrayList = new ArrayList<Integer>(size);
    for (int i = 0; i < size; ++i) {
      arrayList.add(i);
    }
    long sum = 0;
    for (int i = 0; i < size; ++i) {
      sum += arrayList.get(i);
    }
    return sum;
  }

  private static void benchmarkQuickSelect(int size) {
    Random random = new Random(1);
    int[] array = new int[size];
    int[] array2 = new int[size];
    for (int i = 0; i < size; ++i) {
      array[i] = random.nextInt(size / 2);
    }
    Sorter.Sortable sortable = new Reverse2IntSortable(array, array2);
    Sorter.quickselect(sortable, size / 2, random);
  }

  private static void benchmarkQuickSort(int size) {
    Random random = new Random(1);
    int[] array = new int[size];
    int[] array2 = new int[size];
    for (int i = 0; i < size; ++i) {
      array[i] = random.nextInt(size / 2);
    }
    Sorter.Sortable sortable = new Reverse2IntSortable(array, array2);
    Sorter.quicksort(sortable, random);
  }
  
  private static void printFirst(int[] a, int n) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < n; ++i) {
      if (i > 0) {
        buf.append(",");
      }
      buf.append(a[i]);
    }
    System.out.println(buf);
  }
}
