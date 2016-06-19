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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import com.brickmesh.proto.PartModelProto;
import com.brickmesh.util.TestCase;
import com.brickmesh.util.Util;

class PartModelTest extends TestCase {
  public static void main(String[] args) {
    testLoadRealModel();
    testComputeWeight();
  }

  private static void testLoadRealModel() {
    // This loads the model and also performs a bunch of checks.
    PartModel partModel = PartModel.getModel();
  }

  private static void testComputeWeight() {
    try {
      PartModel model1 = PartModel.getModel(
          customColorModelPath(),
          customPartModelPath(1.0, 0.0, 0.0));
      expectEquals(1.0, model1.findPartOrNull("b:1").weightGrams_);
      expectEquals(1.0, model1.findPartOrNull("b:2").weightGrams_);
      expectEquals(2.0, model1.findPartOrNull("b:3").weightGrams_);

      PartModel model2 = PartModel.getModel(
          customColorModelPath(),
          customPartModelPath(0.5, 1.0, 0.0));
      expectEquals(0.5, model2.findPartOrNull("b:1").weightGrams_);
      expectEquals(1.0, model2.findPartOrNull("b:2").weightGrams_);
      expectEquals(1.0, model2.findPartOrNull("b:3").weightGrams_);

      PartModel model3 = PartModel.getModel(
          customColorModelPath(),
          customPartModelPath(0.0, 0.4, 0.0));
      expectEquals(0.4, model3.findPartOrNull("b:1").weightGrams_);
      expectEquals(0.4, model3.findPartOrNull("b:2").weightGrams_);
      expectEquals(0.8, model3.findPartOrNull("b:3").weightGrams_);

      PartModel model4 = PartModel.getModel(
          customColorModelPath(),
          customPartModelPath(0.0, 0.4, 0.5));
      expectEquals(0.4, model4.findPartOrNull("b:1").weightGrams_);
      expectEquals(0.4, model4.findPartOrNull("b:2").weightGrams_);
      expectEquals(0.5, model4.findPartOrNull("b:3").weightGrams_);
    }
    catch (IOException ex) {
      expectEquals("", ex.toString());
    }
  }

  private static String customPartModelPath(double w1, double w2, double w3)
      throws IOException {
    PartModelProto.Part.Builder part1 = PartModelProto.Part.newBuilder()
        .addId("b:1");
    if (w1 > 0.0) {
      part1.setWeightGrams(w1);
    }
    PartModelProto.Part.Builder part2 = PartModelProto.Part.newBuilder()
        .addId("b:2")
        .addSimilar(PartModelProto.Part.Similar.newBuilder().setId("b:1"));
    if (w2 > 0.0) {
      part2.setWeightGrams(w2);
    }
    PartModelProto.Part.Builder part3 = PartModelProto.Part.newBuilder()
      .addId("b:3")
      .addItem(PartModelProto.Part.Item.newBuilder().setId("b:1").setCount(2));
    if (w3 > 0.0) {
      part3.setWeightGrams(w3);
    }

    PartModelProto.PartModel model = PartModelProto.PartModel.newBuilder()
      .addPart(part1)
      .addPart(part2)
      .addPart(part3)
      .build();
    return writeTempMessage(model);
  }

  private static String customColorModelPath() throws IOException {
    final String COLOR_MODEL = Util.joinStrings("\n",
      "color {",
      "  id: 'b:1'",
      "  name: 'Color'",
      "}");
    PartModelProto.ColorModel.Builder builder = PartModelProto.ColorModel.newBuilder();
    TextFormat.merge(new StringReader(COLOR_MODEL), builder);
    return writeTempMessage(builder.build());
  }

  private static String writeTempMessage(Message message) throws IOException {
    FileWriter fw = null;
    try {
      File tempFile = File.createTempFile("brickmesh-test", ".txt");
      fw = new FileWriter(tempFile);
      fw.write(message.toString());
      return tempFile.getPath();
    }
    finally {
      if (fw != null) {
        fw.close();
      }
    }
  }
};