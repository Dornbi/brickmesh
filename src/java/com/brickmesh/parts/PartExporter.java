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

import java.io.OutputStream;
import java.io.PrintStream;

public class PartExporter {
  public static class Options {
    public Options() {
      wantedListId_ = 0;
      acceptNew_ = true;
      acceptUsed_ = false;
      notify_ = false;
    }
    
    public int wantedListId_;
    public boolean acceptNew_;
    public boolean acceptUsed_;
    public boolean notify_;
  }
  
  public static void exportToWantedList(
      RequiredParts parts, OutputStream output, Options options) {
    if (options == null) {
      options = new Options();
    }
    PrintStream ps = output instanceof PrintStream ?
      (PrintStream)output : new PrintStream(output);
    ps.println("<INVENTORY>");
    for (int i = 0; i < parts.numQty_; ++i) {
      int itemIdx = parts.itemIdx_[i];
      ps.println(" <ITEM>");
      ps.println("  <ITEMTYPE>P</ITEMTYPE>");
      ps.print("  <ITEMID>");
      ps.print(parts.preferredParts_[itemIdx].id_);
      ps.println("</ITEMID>");
      ps.print("  <COLOR>");
      ps.print(parts.color_[itemIdx].id_);
      ps.println("</COLOR>");
      ps.print("  <MINQTY>");
      ps.print(parts.quantity_[i]);
      ps.println("</MINQTY>");
      if (options.acceptNew_ && !options.acceptUsed_) {
        ps.println("  <CONDITION>N</CONDITION>");
      } else if (!options.acceptNew_ && options.acceptUsed_) {
        ps.println("  <CONDITION>U</CONDITION>");
      }
      if (options.notify_) {
        ps.println("  <NOTIFY>Y</NOTIFY>");
      } else {
        ps.println("  <NOTIFY>N</NOTIFY>");
      }
      if (options.wantedListId_ > 0) {
        ps.print("  <WANTEDLISTID>");
        ps.print(options.wantedListId_);
        ps.println("  </WANTEDLISTID>");
      }
      ps.println(" </ITEM>");
    }
    ps.println("</INVENTORY>");
  }
}