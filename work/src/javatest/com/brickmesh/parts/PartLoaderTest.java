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
import java.util.Arrays;

import com.brickmesh.util.TestCase;

class PartLoaderTest extends TestCase {
  public static void main(String[] args)
      throws IOException, PartLoader.LoaderException {
    testLoadLxf();
    testLoadWanted();
  }

  private static void testLoadLxf() 
      throws IOException, PartLoader.LoaderException {
    PartLoader.LxfLoader loader = new PartLoader().createLxfLoader(
        PartLoader.Options.createUnlimited());
    loader.parse(new FileInputStream("src/testdata/test.lxf"));
    PartLoader.Result result = loader.getResult();
    expectTrue(null == result.unknownPartIds_);
    expectTrue(null == result.unknownColorIds_);
    expectTrue(result.imageBytes_ != null);
        
    RequiredParts parts = result.parts_;
    expectEquals(2, parts.numItems_);
    expectEquals("3005", parts.preferredParts_[0].id_);
    expectEquals("5", parts.color_[0].id_);
    expectEquals(3, parts.quantity_[0]);
    expectEquals("6019", parts.preferredParts_[1].id_);
    expectEquals("103", parts.color_[1].id_);
    expectEquals(1, parts.quantity_[1]);
  }
  
  private static void testLoadWanted() 
      throws IOException, PartLoader.LoaderException {
    PartLoader.WantedLoader loader = new PartLoader().createWantedLoader(
        PartLoader.Options.createUnlimited());
    loader.parse(new FileInputStream("src/testdata/wanted.xml"));
    PartLoader.Result result = loader.getResult();
    expectTrue(null == result.unknownPartIds_);
    expectTrue(null == result.unknownColorIds_);
    expectTrue(result.imageBytes_ == null);
        
    RequiredParts parts = result.parts_;
    expectEquals(2, parts.numItems_);
    expectEquals("3004", parts.preferredParts_[0].id_);
    expectEquals(6, parts.color_[0].id_);
    expectEquals(1, parts.quantity_[0]);
    expectEquals("3023", parts.preferredParts_[1].id_);
    expectEquals(5, parts.color_[1].id_);
    expectEquals(4, parts.quantity_[1]);
  }
};