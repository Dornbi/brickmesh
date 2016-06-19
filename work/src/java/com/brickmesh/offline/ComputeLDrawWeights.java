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

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.brickmesh.parts.ItemId;
import com.brickmesh.parts.PartModel;

// Computes weight estimates for many parts based on LDraw geometry.
public final class ComputeLDrawWeights {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Not enough args.");
      System.err.println("Usage: <command> <weights-csv> <ldraw-path>\n");
      return;
    }

    ArrayList<String> ids = loadIdsFromCsv(args[0]);
    for (String partId : ids) {
      PartModel.Part part = partModel_.findPartOrNull(partId);
      if (part == null) {
        System.err.println("Unknown part: " + partId);
        return;
      }
      double w = part.weightGrams_;
      LDrawWeightEstimator.Result result = estimateWeightOrNull(partId, args[1]);
      if (result == null) {
        System.err.println("No result for " + partId);
      } else {
        System.out.format("%s,%.3f\n", partId, result.weightGrams());
      }
    }
  }

  private static ArrayList<String> loadIdsFromCsv(String csvPath)
      throws IOException {
    ArrayList<String> result = new ArrayList<String>();
    LineNumberReader reader = new LineNumberReader(new FileReader(csvPath));
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      line = line.trim();
      if (line.length() == 0) continue;
      if (line.startsWith("#")) continue;
      String[] splitLine = line.split(",");
      result.add(splitLine[0]);
    }
    return result;
  }

  private static LDrawWeightEstimator.Result estimateWeightOrNull(
      String partId, String ldrawPath) {
    PartModel.Part part = partModel_.findPartOrNull(partId);
    if (part == null) return null;

    LDrawWeightEstimator lw = new LDrawWeightEstimator(ldrawPath);
    for (String id : part.ids_) {
      // Try the id first.
      String[] idPieces = ItemId.idPiecesOrNull(id);
      if (idPieces == null) continue;
      try {
        return lw.partWeightGramsForFile(idPieces[1] + ".dat");
      }
      catch (IOException e) {}

      // Try stripping the postfix, if any.
      String strippedId = idPieces[1].replaceFirst("[a-z].*\\Z", "");
      try {
        return lw.partWeightGramsForFile(strippedId + ".dat");
      }
      catch (IOException e) {}
    }
    return null;
  }

  private static PartModel partModel_;

  static {
    partModel_ = PartModel.getModel();
  }
}