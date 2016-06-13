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

// Class that collects timing stats for the test set.
public final class TimeStats {
  public enum Phase {
    LOAD(0), ITEMS(1), OPT(2);
    
    public static final int MAX = 3;
    
    Phase(int value) {
      value_ = value;
    }
    
    public final int getValue() {
      return value_;
    }
    
    public static final String getName(int value) {
      if (value == 0) return "LOAD";
      if (value == 1) return "ITEMS";
      if (value == 2) return "OPT";
      return "Unknown";
    }
    
    private int value_;
  };
  
  public TimeStats(int numItems) {
    statsNanos_ = new long[numItems * Phase.MAX];
    items_ = new String[numItems];
    lastNanos_ = System.nanoTime();
    idx_ = -1;
  }

  public void measure(String item, Phase phase) {
    if (idx_ < 0 || !item.equals(items_[idx_])) {
      ++idx_;
      if (idx_ * Phase.MAX >= statsNanos_.length) {
        throw new IllegalArgumentException("Too many items.");
      }
      items_[idx_] = item;
    }
    long t2 = System.nanoTime();
    statsNanos_[idx_ * Phase.MAX + phase.getValue()] = t2 - lastNanos_;
    lastNanos_ = t2;
  }
  
  public void print() {
    ++idx_;
    long[] phaseMinNanos = new long[Phase.MAX];
    int[] phaseMinIdx = new int[Phase.MAX];
    long[] phaseMaxNanos = new long[Phase.MAX];
    int[] phaseMaxIdx = new int[Phase.MAX];
    long[] phaseAvgNanos = new long[Phase.MAX];
    long totalMinNanos = 0;
    int totalMinIdx = 0;
    long totalMaxNanos = 0;
    int totalMaxIdx = 0;
    for (int i = 0; i < idx_; ++i) {
      long total = 0;
      for (int phase = 0; phase < Phase.MAX; ++phase) {
        long t = statsNanos_[i * Phase.MAX + phase];
        total += t;
        if (phaseMinNanos[phase] == 0 || t < phaseMinNanos[phase]) {
          phaseMinNanos[phase] = t;
          phaseMinIdx[phase] = i;
        }
        if (t > phaseMaxNanos[phase]) {
          phaseMaxNanos[phase] = t;
          phaseMaxIdx[phase] = i;
        }
        phaseAvgNanos[phase] += t;
      }
      if (totalMinNanos == 0 || total < totalMinNanos) {
        totalMinNanos = total;
        totalMinIdx = i;
      }
      if (total > totalMaxNanos) {
        totalMaxNanos = total;
        totalMaxIdx = i;
      }
    }
    
    Log.perfLog("Total min = %d us (%s)\n",
        totalMinNanos / 1000, items_[totalMinIdx]);
    Log.perfLog("Total max = %d us (%s)\n",
        totalMaxNanos / 1000, items_[totalMaxIdx]);
    for (int phase = 0; phase < Phase.MAX; ++phase) {
      Log.perfLog("Phase %s min = %d us (%s)\n", Phase.getName(phase),
          phaseMinNanos[phase] / 1000, items_[phaseMinIdx[phase]]);
      Log.perfLog("Phase %s max = %d us (%s)\n", Phase.getName(phase),
          phaseMaxNanos[phase] / 1000, items_[phaseMaxIdx[phase]]);
      Log.perfLog("Phase %s avg = %d us\n", Phase.getName(phase),
          phaseAvgNanos[phase] / 1000 / idx_);
    }
  }

  private int idx_;
  private long lastNanos_;
  private final String[] items_;
  private final long[] statsNanos_;
}
