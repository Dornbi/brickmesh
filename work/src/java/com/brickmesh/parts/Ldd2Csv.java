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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class Ldd2Csv {
  public static void main(String[] args)
      throws PartLoader.LoaderException, IOException {
    if (args.length == 0) {
      System.err.println("No args.");
      System.err.println("Usage: <command> <lxf-path> [<lxf-path> ...]>\n");
      return;
    }

    PartLoader.LxfLoader loader = new PartLoader().createLxfLoader(
        PartLoader.Options.createUnlimited());
    for (String arg : args) {
      FileInputStream fis = new FileInputStream(arg);
      try {
        loader.parse(fis);
      }
      finally {
        fis.close();
      }
    }

    RequiredItems items = loader.getResult().items_;
    UnknownItems unknownItems = loader.getResult().unknownItems_;
    TreeMap<ItemId, Integer> blItems = items.exportToNamespace("b", unknownItems);
    PartExporter.exportToCsvList(blItems, System.out);
    System.err.format("Estimated weight: %.3f gram(s)\n", items.weightEstimateGrams());

    if (unknownItems.unknownItemsOrNull() != null) {
      for (Map.Entry<ItemId, Integer> entry : unknownItems.unknownItemsOrNull().entrySet()) {
        ItemId itemId = entry.getKey().withoutNamespace();
        System.err.format("Warning: Did not recognize LEGO item: part=%s, color=%s, count=%d\n",
            itemId.partId(), itemId.colorId(), entry.getValue());
      }
    }
    if (unknownItems.unmappableItemsOrNull() != null) {
      for (Map.Entry<ItemId, Integer> entry : unknownItems.unmappableItemsOrNull().entrySet()) {
        ItemId itemId = entry.getKey().withoutNamespace();
        System.err.format("Warning: Unable to map LEGO item: part=%s, color=%s, count=%d\n",
            itemId.partId(), itemId.colorId(), entry.getValue());
      }
    }
  }
}