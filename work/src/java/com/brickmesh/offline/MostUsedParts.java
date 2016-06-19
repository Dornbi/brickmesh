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

package com.brickmesh.offline;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.brickmesh.parts.ItemId;
import com.brickmesh.parts.PartLoader;
import com.brickmesh.parts.PartModel;
import com.brickmesh.parts.RequiredItems;

// Prints out the list of most used parts, given one or more LDD files.
// This is useful to get a list of parts that are used often and hence
// need more accurate weight estimates.
public final class MostUsedParts {
  public static void main(String[] args)
      throws IOException, PartLoader.LoaderException {
    if (args.length < 1) {
      System.err.println("Not enough args.");
      System.err.println("Usage: <command> <lxf-file> [<lxf-file> ...]\n");
      return;
    }
    
    HashMap<ItemId, Integer> counts = countParts(args);
    ArrayList<Map.Entry<ItemId, Integer>> parts =
        new ArrayList<Map.Entry<ItemId, Integer>>(counts.entrySet());
    Collections.sort(parts, new Comparator<Map.Entry<ItemId, Integer>>() {
      @Override
      public int compare(Map.Entry<ItemId, Integer> e1,
                         Map.Entry<ItemId, Integer> e2) {
        return e2.getValue() - e1.getValue();
      }
    });
    
    for (Map.Entry<ItemId, Integer> entry : parts) {
      ItemId itemId = entry.getKey();
      int qty = entry.getValue();
      System.out.format("%16s: %5d\n", itemId, qty);
    }
  }
  
  private static HashMap<ItemId, Integer> countParts(String[] args) 
      throws IOException, PartLoader.LoaderException {
    PartLoader.LxfLoader loader = new PartLoader(partModel_).createLxfLoader(
      PartLoader.Options.createUnlimited());
    HashMap<ItemId, Integer> counts = new HashMap<ItemId, Integer>();
    for (String arg : args) {
      FileInputStream fis = new FileInputStream(arg);
      try {
        loader.parse(fis);
      }
      finally {
        fis.close();
      }
    }

    PartLoader.Result result = loader.getResult();
    RequiredItems requiredItems = result.items_;
    for (RequiredItems.Item item : requiredItems.items().values()) {
      ItemId itemId = new ItemId(item.part_.primaryId(), item.color_.primaryId());
      Integer count = counts.get(itemId);
      if (count == null) {
        counts.put(itemId, item.count_);
      } else {
        counts.put(itemId, count + item.count_);
      }
    }
    
    return counts;
  }

  private static PartModel partModel_;

  static {
    partModel_ = PartModel.getModel();
  }
}