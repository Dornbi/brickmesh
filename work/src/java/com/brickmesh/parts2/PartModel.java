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

package com.brickmesh.parts2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.google.protobuf.TextFormat;

import com.brickmesh.proto.PartModelProto;
import com.brickmesh.util.Log;

public class PartModel {
  // A single color.
  public static class Color {
    // The IDs of the color in all color namespaces.
    String[] id_;

    // A human-readable name of the color.
    String name_;
  }

  // For parts composed from sub-parts, this class holds info about the sub-parts.
  public static class Item {
    // The sub-part.
    public Part part_;
    
    // If non-null then the sub-part always comes in this color.
    // Otherwise it inherits the color of the composed part.
    public Color color_;

    // How many sub-parts are needed for the composed part. Usually 1.
    public int count_;

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(part_.id_);
      if (color_ != null) {
        sb.append(",color=");
        sb.append(color_.id_);
      }
      sb.append(",count=");
      sb.append(count_);
      return sb.toString();
    }
  }
  
  public static class Part {
    String[] id_;

    HashSet<Part> similar_;
    HashSet<Part> confirm_;

    // If this is a composite part made from other parts, this is the
    // list of all the parts that it is made from.
    Item[] items_;

    // If this part that is used to compose other parts, they are listed here.
    HashSet<Part> parents_;    

    // The weight in grams. Must be nonzero after loading.
    public double weightGrams_;
  }
  
  
  public static class MappingResult {
    public ArrayList<Item> mappedItems_;
    public ArrayList<String> unmappedColorIds_;
    public ArrayList<String> unmappedPartIds_;
    public ArrayList<ItemId> unmappedItemIds_;
    public ArrayList<ItemId> nonExistentItemIds_;
  }

  public static PartModel getModel() {
    if (model_ == null) {
      model_ = new PartModel(
        new InputStreamReader(PartModel.class.getResourceAsStream(
          "/com/brickmesh/assets/color-model.txt")),
        new InputStreamReader(PartModel.class.getResourceAsStream(
          "/com/brickmesh/assets/part-model.txt")));
    }
    return model_;
  }

  public static PartModel getModel(String colorPath, String partPath) {
    try {
      return new PartModel(
          new FileReader(colorPath),
          new FileReader(partPath));
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }
  
  public Color getColorById(String id) {
    return null;
  }
  
  public Part getPartById(String id) {
    return null;
  }
  
  public MappingResult mapPartIdList(ArrayList<ItemId> itemIdList) {
    return null;
  }
  
  private static class ErrorCollector {
    public void error(String message) {
      System.err.println("Error: " + message);
      if (messages_ == null) {
        messages_ = new ArrayList<String>();
      }
      messages_.add(message);
    }
    
    public void finishStage() {
      if (messages_ != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("Errors in the current stage:\n");
        for (String message : messages_) {
          sb.append(message);
          sb.append('\n');
        }
        throw new AssertionError(sb.toString());
      }
    }
    
    private ArrayList<String> messages_;
  }
  
  private PartModel(Reader colorReader, Reader partReader) {
    loadColors(colorReader);
    loadParts(partReader);
  }
  
  private void loadColors(Reader reader) {
    try {
      BufferedReader br = new BufferedReader(reader);
      PartModelProto.ColorModel.Builder builder =
          PartModelProto.ColorModel.newBuilder();
      TextFormat.merge(br, builder);
      PartModelProto.ColorModel modelProto = builder.build();

      ArrayList<Color> colorArray = new ArrayList<Color>(modelProto.getColorCount());
      colorIdMap_ = new HashMap<String, Color>(modelProto.getColorCount());
      ErrorCollector errorCollector = new ErrorCollector();

      for (PartModelProto.Color colorProto : modelProto.getColorList()) {
        Color color = new Color();
        color.id_ = colorProto.getIdList().toArray(new String[colorProto.getIdCount()]);
        if (color.id_.length == 0) {
          errorCollector.error("Color with no id.");
          continue;
        }
        color.name_ = colorProto.getName();
        colorArray.add(color);
        for (String id : color.id_) {
          if (!isValidColorId(id)) {
            errorCollector.error("Invalid color id: " + id);
            continue;
          }
          if (colorIdMap_.put(id, color) != null) {
            errorCollector.error("Duplicate color id: " + id);
            continue;
          }
        }
      }
      errorCollector.finishStage();
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void loadParts(Reader reader) {
    try {
      boolean error = false;
      BufferedReader br = new BufferedReader(reader);
      PartModelProto.PartModel.Builder builder =
          PartModelProto.PartModel.newBuilder();
      TextFormat.merge(br, builder);
      PartModelProto.PartModel modelProto = builder.build();
      
      partMap_ = new HashMap<String, Part>(modelProto.getPartCount());
      
      // Stage 1: load all parts.
      loadPartProto(modelProto);

      // Stage 2: Populate contained & similar parts. This is a separate
      // stage to make sure that all parts are loaded.
      populateRelatedParts(modelProto);

      // Stage 3: Compute weights.
      computeWeights();
      
      // Stage 4: Verify that part ids are not in conflict.
      verifyPartIds();

      Log.info("partMap_.size()=" + partMap_.size());
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }
  
  private void loadPartProto(PartModelProto.PartModel modelProto) {
    ErrorCollector errorCollector = new ErrorCollector();
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      Part part = new Part();
      part.id_ = partProto.getIdList().toArray(new String[partProto.getIdCount()]);
      if (part.id_.length == 0) {
        errorCollector.error("Part with no id.");
        continue;
      }
      for (String id : part.id_) {
        if (!isValidPartId(id)) {
          errorCollector.error("Invalid part id: " + id);
          continue;
        }
        if (partMap_.put(id, part) != null) {
          errorCollector.error("Duplicate id: " + id);
          continue;
        }
      }
      part.weightGrams_ = partProto.getWeightGrams();
    }
    errorCollector.finishStage();
  }

  private void populateRelatedParts(PartModelProto.PartModel modelProto) {
    ErrorCollector errorCollector = new ErrorCollector();
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      String partId = partProto.getId(0);
      Part part = partMap_.get(partId);

      // Populate similar parts.
      if (partProto.getSimilarCount() > 0) {
        for (PartModelProto.Part.Similar similarProto : partProto.getSimilarList()) {
          String similarId = similarProto.getId();
          Part similarPart = partMap_.get(similarId);
          if (similarPart == null) {
            errorCollector.error(
                "Similar part not found: " + similarId + " in part: " + partId);
            continue;
          }
          if (!addSimilarPart(part, similarPart, errorCollector)) {
            continue;
          }
          if (similarProto.getConfirm() == PartModelProto.Part.Similar.Confirm.OTHER) {
            part.confirm_ = includeInSet(similarPart, part.confirm_);
          } else if (similarProto.getConfirm() == PartModelProto.Part.Similar.Confirm.BOTH) {
            part.confirm_ = includeInSet(similarPart, part.confirm_);
            similarPart.confirm_ = includeInSet(part, similarPart.confirm_);
          }
        }
      }

      // Decor ids are really just syntactic sugar to automatically create
      // them and make them related to the undecorated version.
      if (partProto.getDecorCount() > 0) {
        for (PartModelProto.Part.Decor decorProto : partProto.getDecorList()) {
          Part decorPart = new Part();
          decorPart.id_ = decorProto.getIdList().toArray(new String[decorProto.getIdCount()]);
          for (String id : decorPart.id_) {
            if (!isValidPartId(id)) {
              errorCollector.error("Invalid decor part id: " + id + " in part: " + partId);
            }
            if (partMap_.put(id, decorPart) != null) {
              errorCollector.error("Duplicate decor id: " + id + " in part: " + partId);
            }
          }
          decorPart.similar_ = new HashSet<Part>();
          if (!addSimilarPart(part, decorPart, errorCollector)) {
            continue;
          }
          part.confirm_ = includeInSet(decorPart, part.confirm_);
          decorPart.confirm_ = includeInSet(part, decorPart.confirm_);
        }
      }
      
      // Populate contained parts.
      if (partProto.getItemCount() > 0) {
        Item[] items = new Item[partProto.getItemCount()];
        for (int i = 0 ; i < partProto.getItemCount(); ++i) {
          String itemId = partProto.getItem(i).getId();
          Item item = new Item();
          item.part_ = partMap_.get(itemId);
          if (item.part_ == null) {
            errorCollector.error("Contained item not found: " + itemId + " in part: " + partId);
            continue;
          }
          String colorId = partProto.getItem(i).getColor();
          if (partProto.getItem(i).hasColor()) {
            item.color_ = colorIdMap_.get(colorId);
          }
          if (item.color_ == null) {
            errorCollector.error("Unknown color: " + colorId + " in part: " + partId);
            continue;
          }
          item.count_ = partProto.getItem(i).getCount();
          if (item.count_ <= 0) {
            errorCollector.error("Invalid count: " + item.count_ + " in part: " + partId);
            continue;
          }
          items[i] = item;
          if (item.part_.parents_ == null) {
            item.part_.parents_ = new HashSet<Part>();
          }
          if (!item.part_.parents_.add(part)) {
            errorCollector.error("Duplicate inclusion of: " + itemId + " in part: " + partId);
            continue;
          }
        }
        part.items_ = items;
      }
    }
    errorCollector.finishStage();
  }
  
  private void computeWeights() {
    ErrorCollector errorCollector = new ErrorCollector();
    HashSet<Part> visited = new HashSet<Part>(partMap_.size());
    for (Part part : partMap_.values()) {
      computeWeight(part, visited, errorCollector);
    }
    errorCollector.finishStage();
  }
  
  private void verifyPartIds() {
    ErrorCollector errorCollector = new ErrorCollector();
    for (Part part : partMap_.values()) {
      for (String id : part.id_) {
        String[] idPieces = getIdPieces(id);
        String[] conflictingIdSpaces = ID_CONFLICT_MAP.get(idPieces[0]);
        for (int i = 0; i < conflictingIdSpaces.length; ++i) {
          if (partMap_.containsKey(conflictingIdSpaces[i] + ":" + idPieces[1])) {
            errorCollector.error(String.format("Id conflict %s+%s: %s",
                idPieces[0], conflictingIdSpaces[i], idPieces[1]));
          }
        }
      }
    }
    errorCollector.finishStage();
  }
   
  private void computeWeight(Part part, HashSet<Part> visited, ErrorCollector errorCollector) {
    if (!visited.add(part)) {
      return;
    }
    
    // If the part consists of sub-parts then the weight is the sum of those.
    if (part.items_ != null) {
      if (part.weightGrams_ > 0.0) {
        errorCollector.error("Part " + part.id_[0] + " must not contain weight.");
        return;
      }
      double weightGrams = 0.0;
      for (Item item : part.items_) {
        computeWeight(item.part_, visited, errorCollector);
        if (item.part_.weightGrams_ <= 0.0) {
          errorCollector.error("Likely loop in sub-parts: " + part.id_[0] + ", " + item.part_.id_[0]);
        }
        weightGrams += item.part_.weightGrams_ * item.count_;
      }
      part.weightGrams_ = weightGrams;
      return;
    }
    
    // If it already has weight then the weight is that.
    if (part.weightGrams_ > 0.0) return;
    
    // If any of the similar parts contain weight, use that.
    if (part.similar_ != null) {
      for (Part similarPart : part.similar_) {
        computeWeight(similarPart, visited, errorCollector);
        if (similarPart.weightGrams_ > 0.0) {
          part.weightGrams_ = similarPart.weightGrams_;
          return;
        }
      }
    }
    errorCollector.error("Unable to compute weight for part: " + part.id_[0]);
    return;
  }
  
  private boolean addSimilarPart(Part part, Part similarPart, ErrorCollector errorCollector) {
    if (part.similar_ == null) {
      part.similar_ = new HashSet<Part>();
    }
    if (!part.similar_.add(similarPart)) {
      errorCollector.error(
          "Similar part already present: " + similarPart.id_[0] + " in part: " + part.id_[0]);
      return false;
    }
    if (similarPart.similar_ == null) {
      similarPart.similar_ = new HashSet<Part>();
    }
    if (!similarPart.similar_.add(part)) {
      errorCollector.error(
          "Similar part already present: " + similarPart.id_[0] + " in part: " + part.id_[0]);
      return false;
    }
    return true;
  }

  private static HashSet<Part> includeInSet(Part part, HashSet<Part> set) {
    if (set == null) {
      set = new HashSet<Part>();
    }
    set.add(part);
    return set;
  }

  private static String[] getIdPieces(String id) {
    String[] split = id.split(":");
    if (split.length != 2) return null;
    return split;
  }

  private static boolean isValidColorId(String colorId) {
    String idSpace = getIdPieces(colorId)[0];
    if (idSpace == null) return false;
    if (idSpace.equals("l")) return true;
    if (idSpace.equals("bl")) return true;
    return false;
  }

  private static boolean isValidPartId(String partId) {
    String idSpace = getIdPieces(partId)[0];
    if (idSpace == null) return false;
    return ID_CONFLICT_MAP.containsKey(idSpace);
  }
  
  private HashMap<String, Color> colorIdMap_;
  private HashMap<String, Part> partMap_;

  private static PartModel model_;
  private static final HashMap<String, String[]> ID_CONFLICT_MAP;
  
  static {
    ID_CONFLICT_MAP = new HashMap<String, String[]>();
    String[] G_CONFLICT = { "ga", "bl", "l" };
    ID_CONFLICT_MAP.put("g", G_CONFLICT);
    String[] GA_CONFLICT = { "g", "bl", "l" };
    ID_CONFLICT_MAP.put("ga", GA_CONFLICT);
    String[] BL_CONFLICT = { "g", "ga" };
    ID_CONFLICT_MAP.put("bl", BL_CONFLICT);
    String[] L_CONFLICT = { "g", "ga" };
    ID_CONFLICT_MAP.put("l", L_CONFLICT);
    String[] V_CONFLICT = {};
    ID_CONFLICT_MAP.put("l", V_CONFLICT);
  }
}