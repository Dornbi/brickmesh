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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Map;

import com.brickmesh.parts.PartLoader;
import com.brickmesh.parts.UnknownItems;

// Helps adding missing parts to the part model. The input is one or more
// LXF files, the output are fragments of files that can be added to the
// part model and weight CSV files after checking and editing manually.
class MissingPartTool {
  public static void main(String[] args)
      throws PartLoader.LoaderException, IOException {
    if (args.length < 2) {
      System.err.println("No args.");
      System.err.println("Usage: <command> <ldraw-path> <lxf-path> [<lxf-path> ...]>\n");
      return;
    }

    PartLoader.LxfLoader loader = new PartLoader().createLxfLoader(
        PartLoader.Options.createUnlimited());
    for (int i = 1; i < args.length; ++i) {
      FileInputStream fis = new FileInputStream(args[i]);
      try {
        loader.parse(fis);
      }
      finally {
        fis.close();
      }
    }

    PartLoader.Result result = loader.getResult();
    UnknownItems unknownItems = result.unknownItems_;
    if (unknownItems.unknownPartIdsOrNull() != null) {
      LDrawWeightEstimator estimator = new LDrawWeightEstimator(args[0]);
      PrintStream partTemplateStream = new PrintStream(
          new FileOutputStream("part-templates.new"));
      PrintStream lddWeightStream = new PrintStream(
          new FileOutputStream("weights-ldraw.new"));
      PrintStream noWeightStream = new PrintStream(
          new FileOutputStream("weights-no-ldraw.new"));
      try {
        for (Map.Entry<String, Integer> entry : unknownItems.unknownPartIdsOrNull().entrySet()) {
          String partId = entry.getKey();
          partTemplateStream.println("part {");
          partTemplateStream.println("  id: \"" + partId + "\"");
          partTemplateStream.println("}");
          partTemplateStream.println();
        
          LDrawWeightEstimator.Result weightResult = estimator.partWeightGramsForPart(partId);
          if (weightResult == null) {
            noWeightStream.format("%s,\n", partId);
          } else {
            lddWeightStream.format("%s,%.3f\n", partId, weightResult.weightGrams());
          }
        }
      }
      finally {
        partTemplateStream.close();
        lddWeightStream.close();
        noWeightStream.close();
      }
    }
  }
}