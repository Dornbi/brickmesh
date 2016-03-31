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

import java.util.Arrays;

import com.brickmesh.util.TestCase;
import com.brickmesh.util.Util;

class RequiredPartsTest extends TestCase {
  public static void main(String[] args) {
    PartModel partModel = PartModel.getModel();
    testRequiredPartsCreate(partModel);
  }
  
  private static void testRequiredPartsCreate(PartModel partModel) {
    RequiredParts parts = createTestParts(partModel);
    expectEquals(4, parts.totalQty_);
    expectEquals(3, parts.numQty_);
    expectEquals(Util.intArray(1, 2, 1),
        Arrays.copyOfRange(parts.quantity_, 0, 3));
    expectEquals(Util.intArray(0, 1, 2),
        Arrays.copyOfRange(parts.itemIdx_, 0, 3));
    expectEquals(3, parts.numItems_);
    expectEquals("3005", parts.partGroups_[0].part_.get(0).id_);
    expectEquals("3794", parts.partGroups_[1].part_.get(0).id_);
    expectEquals("3070b", parts.partGroups_[2].part_.get(0).id_);
    expectEquals("5", parts.color_[0].id_);
    expectEquals("1", parts.color_[1].id_);
    expectEquals("2", parts.color_[2].id_);
  }

  private static final RequiredParts createTestParts(PartModel partModel) {
    RequiredParts parts = new RequiredParts();
    parts.addItem(
        partModel.getPartByIdOrNull("3005"),
        partModel.getPreferredPartByIdOrNull("3005"),
        partModel.getColorByIdOrNull("5"),
        1);
    parts.addItem(
        partModel.getPartByIdOrNull("3794b"),
        partModel.getPreferredPartByIdOrNull("3794b"),
        partModel.getColorByIdOrNull("1"),
        2);
    parts.addItem(
        partModel.getPartByIdOrNull("3070b"),
        partModel.getPreferredPartByIdOrNull("3070b"),
        partModel.getColorByIdOrNull("2"),
        1);
    return parts;
  }
};