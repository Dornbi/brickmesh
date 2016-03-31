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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ExtractWeights {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Not enough args.");
      System.err.println("Usage: <command> <part-model-path>\n");
      return;
    }
    
    processPartModel(args[0]);
  }
  
  private static void processPartModel(String path)
      throws IOException {
    LineNumberReader reader = new LineNumberReader(new FileReader(path));
    String partId = "";
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      Matcher partIdMatcher = MATCH_PART_ID.matcher(line);
      Matcher weightMatcher = MATCH_WEIGHT.matcher(line);
      if (weightMatcher.matches()) {
        String weightStr = weightMatcher.group(1);
        float weight = Float.parseFloat(weightStr);
        System.out.format("%s,%.3f\n", partId, weight);
      }
      if (partIdMatcher.matches()) {
        partId = partIdMatcher.group(1);
      }
    }
  }
  
  static {
    try {
      MATCH_PART_ID = Pattern.compile("\\A *id: \\\"([a-z0-9]+)\\\".*");
      MATCH_WEIGHT = Pattern.compile("\\A *weight_grams: ([0-9\\.]+).*");
      
    }
    catch (PatternSyntaxException e) {
      throw new AssertionError(e);
    }
  }

  private static Pattern MATCH_PART_ID;
  private static Pattern MATCH_WEIGHT;
}