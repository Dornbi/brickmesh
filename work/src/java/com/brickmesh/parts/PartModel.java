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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import com.google.protobuf.TextFormat;

import com.brickmesh.proto.PartModelProto;
import com.brickmesh.util.Log;
import com.brickmesh.util.Util;

public class PartModel {
  // Magic value to represent parts that can be any color.
  // This is only used rarely, for example in RequiredItems.interestingItems.
  public static Color ANY_COLOR;

  // A single color.
  public static class Color {
    // The IDs of the color in all color namespaces.
    public String[] ids_;

    // A human-readable name of the color.
    public String name_;

    public String primaryId() {
      return ids_[0];
    }

    public String idInNamespace(String namespace) {
      for (String id : ids_) {
        if (id.startsWith(namespace) &&
            id.charAt(namespace.length()) == ':') {
          return id;
        }
      }
      return null;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("ids=");
      sb.append(Util.joinArray(",", ids_));
      return sb.toString();
    }
  }

  // A part that has a specific color and quantity.
  // In the model for parts composed from sub-parts, this holds info
  // about the sub-parts. color_ may be null or explicitly set.
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
      sb.append(part_.primaryId());
      if (color_ != null) {
        sb.append(",color=");
        sb.append(color_.primaryId());
      }
      sb.append(",count=");
      sb.append(count_);
      return sb.toString();
    }
  }

  // A single part in the model.
  public static class Part {
    // All the ids of this part. Each one is unique.
    public String[] ids_;

    // Parts that are similar. They are different molds but can
    // replace each other without asking the user. null if there are
    // no similar parts.
    public HashSet<Part> similar_;

    // Like similar_, but needs user confirmation.
    public HashSet<Part> confirm_;

    // If this is a composite part made from other parts, this is the
    // list of all the parts that it is made from.
    public Item[] items_;

    // If this part that is used to compose other parts, they are listed here.
    // Otherwise null.
    public HashSet<Part> parents_;

    // The weight in grams. The PartLoader ensures that this is non zero.
    // For composed items, this is usually the sum of their sub-parts.
    public double weightGrams_;

    public String primaryId() {
      return ids_[0];
    }

    public String idInNamespace(String namespace) {
      for (String id : ids_) {
        if (id.startsWith(namespace) &&
            id.charAt(namespace.length()) == ':') {
          return id;
        }
      }
      return null;
    }

    // Returns the color of a sub-part. null if childPart is not
    // found or if it has no color.
    public Color childPartColor(Part childPart) {
      if (items_ != null) {
        for (Item item : items_) {
          if (item.part_ == childPart) {
            return item.color_;
          }
        }
      }
      return null;
    }

    // One of the parts that inherits the color of the parent.
    // The PartLoader guarantees that this is always non-null unless
    // there are no children.
    public Part pickChildWithoutColor() {
      if (items_ != null) {
        for (Item item : items_) {
          if (item.color_ == null) {
            return item.part_;
          }
        }
      }
      return null;
    }

    // Returns the number of children without color.
    public int numChildrenWithoutColor() {
      if (items_ != null) {
        int count = 0;
        for (Item item : items_) {
          if (item.color_ == null) {
            count += item.count_;
          }
        }
        return count;
      } else {
        return 0;
      }
    }

    // The total number of parts in the hierarchy. 1 if there
    // are no children.
    public int numPartsInHierarchy() {
      if (items_ == null) return 1;
      int count = 0;
      for (Item item : items_) {
        count += item.part_.numPartsInHierarchy() * item.count_;
      }
      return count;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("ids=");
      sb.append(Util.joinArray(",", ids_));
      sb.append(String.format(",weight=%.3f", weightGrams_));
      if (items_ != null) {
        sb.append(",items=[");
        sb.append(Util.joinArray(",", items_));
        sb.append("]");
      }
      return sb.toString();
    }
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

  public Color findColorOrNull(String colorId) {
    return colorMap_.get(colorId);
  }

  public Part findPartOrNull(String partId) {
    return partMap_.get(partId);
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

      colorMap_ = new HashMap<String, Color>(modelProto.getColorCount());
      for (String id : ANY_COLOR.ids_) {
        colorMap_.put(id, ANY_COLOR);
      }
      ErrorCollector errorCollector = new ErrorCollector();

      for (PartModelProto.Color colorProto : modelProto.getColorList()) {
        Color color = new Color();
        color.ids_ = colorProto.getIdList().toArray(new String[colorProto.getIdCount()]);
        if (color.ids_.length == 0) {
          errorCollector.error("Color with no id.");
          continue;
        }
        color.name_ = colorProto.getName();
        for (String id : color.ids_) {
          if (!isValidColorId(id)) {
            errorCollector.error("Invalid color id: " + id);
            continue;
          }
          if (colorMap_.put(id, color) != null) {
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
      long startNanos = System.nanoTime();

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

      Log.info("partMap_.size()=%d", partMap_.size());
      Util.logPhaseTime("Model load", startNanos);
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void loadPartProto(PartModelProto.PartModel modelProto) {
    ErrorCollector errorCollector = new ErrorCollector();
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      Part part = new Part();
      ArrayList<String> ids = new ArrayList<String>(partProto.getIdCount() * 4);
      for (String id : partProto.getIdList()) {
        String[] expanded = expandPartId(id);
        if (expanded == null) {
          errorCollector.error("Invalid part id: " + id);
          continue;
        }
        for (String expandedId : expanded) {
          ids.add(expandedId);
          if (partMap_.put(expandedId, part) != null) {
            errorCollector.error("Id collision: " + id);
            continue;
          }
        }
      }
      if (ids.size() == 0) {
        errorCollector.error("Part with no id.");
        continue;
      }
      part.ids_ = ids.toArray(new String[ids.size()]);
      part.weightGrams_ = partProto.getWeightGrams();
    }
    errorCollector.finishStage();
  }

  private void populateRelatedParts(PartModelProto.PartModel modelProto) {
    ErrorCollector errorCollector = new ErrorCollector();
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      String partId = translatePartIdOrError(partProto.getId(0));
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
          if (similarProto.getConfirm()) {
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
          ArrayList<String> ids = new ArrayList<String>(decorProto.getIdCount() * 4);
          for (String id : decorProto.getIdList()) {
            String[] expanded = expandPartId(id);
            if (expanded == null) {
              errorCollector.error("Invalid decor part id: " + id + " in part: " + partId);
            }
            for (String expandedId : expanded) {
              ids.add(expandedId);
              if (partMap_.put(expandedId, decorPart) != null) {
                errorCollector.error("Decor id collision: " + id);
                continue;
              }
            }
          }
          if (ids.size() == 0) {
            errorCollector.error("Decor with no id in part " + partId);
            continue;
          }
          decorPart.ids_ = ids.toArray(new String[ids.size()]);
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
          PartModelProto.Part.Item itemProto = partProto.getItem(i);
          String itemId = itemProto.getId();
          Item item = new Item();
          item.part_ = partMap_.get(itemId);
          if (item.part_ == null) {
            errorCollector.error("Contained item not found: " + itemId + " in part: " + partId);
            continue;
          }
          if (itemProto.hasColor()) {
            if (i == 0) {
              errorCollector.error("Item 0 must not have color in part: " + partId);
              continue;
            }
            String colorId = itemProto.getColor();
            item.color_ = colorMap_.get(colorId);
            if (item.color_ == null) {
              errorCollector.error("Unknown color: " + colorId + " in part: " + partId);
              continue;
            }
          }
          if (itemProto.hasCount()) {
            item.count_ = itemProto.getCount();
            if (item.count_ <= 0) {
              errorCollector.error("Invalid count: " + item.count_ + " in part: " + partId);
              continue;
            }
          } else {
            item.count_ = 1;
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
    for (Part part : partMap_.values()) {
      HashSet<Part> visited = new HashSet<Part>();
      computeWeight(part, visited, errorCollector);
      if (part.weightGrams_ <= 0.0) {
        errorCollector.error("Unable to compute weight for part: " + part.primaryId());
      }
    }
    errorCollector.finishStage();
  }

  private void computeWeight(Part part, HashSet<Part> visited, ErrorCollector errorCollector) {
    if (!visited.add(part)) {
      return;
    }

    // If it already has weight then the weight is that.
    if (part.weightGrams_ > 0.0) return;

    // If the part consists of sub-parts then the weight is the sum of those.
    if (part.items_ != null) {
      double weightGrams = 0.0;
      for (Item item : part.items_) {
        computeWeight(item.part_, visited, errorCollector);
        if (item.part_.weightGrams_ <= 0.0) {
          errorCollector.error("Sub-part weight missing or there is a loop: " +
              part.ids_[0] + ", " + item.part_.primaryId());
        }
        weightGrams += item.part_.weightGrams_ * item.count_;
      }
      part.weightGrams_ = weightGrams;
      return;
    }

    // If any of the similar parts contain weight, use that.
    //System.out.println(part.similar_);
    if (part.similar_ != null) {
      for (Part similarPart : part.similar_) {
        computeWeight(similarPart, visited, errorCollector);
        if (similarPart.weightGrams_ > 0.0) {
          part.weightGrams_ = similarPart.weightGrams_;
          return;
        }
      }
    }
  }

  private boolean addSimilarPart(Part part, Part similarPart, ErrorCollector errorCollector) {
    if (part.similar_ == null) {
      part.similar_ = new HashSet<Part>();
    }
    if (!part.similar_.add(similarPart)) {
      errorCollector.error(
          "Similar part already present: " + similarPart.primaryId() +
          " in part: " + part.primaryId());
      return false;
    }
    if (similarPart.similar_ == null) {
      similarPart.similar_ = new HashSet<Part>();
    }
    if (!similarPart.similar_.add(part)) {
      errorCollector.error(
          "Similar part already present: " + similarPart.primaryId() +
          " in part: " + part.primaryId());
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

  private static boolean isValidColorId(String colorId) {
    String idSpace = ItemId.idPiecesOrNull(colorId)[0];
    if (idSpace == null) return false;
    if (idSpace.equals("l")) return true;
    if (idSpace.equals("b")) return true;
    return false;
  }

  private static String[] expandPartId(String partId) {
    String[] idPieces = ItemId.idPiecesOrNull(partId);
    if (idPieces == null) return null;
    String[] expanded = PART_ID_EXPANSION.get(idPieces[0]);
    if (expanded == null) return null;
    String[] result = new String[expanded.length];
    for (int i = 0; i < expanded.length; ++i) {
      result[i] = expanded[i] + ":" + idPieces[1];
    }
    return result;
  }

  private static String translatePartIdOrError(String partId) {
    String[] expandedIds = expandPartId(partId);
    if (expandedIds == null || expandedIds.length < 1) {
      throw new AssertionError("Cannot translate: " + partId);
    }
    return expandedIds[0];
  }

  private HashMap<String, Color> colorMap_;
  private HashMap<String, Part> partMap_;

  private static PartModel model_;
  private static final HashMap<String, String[]> PART_ID_EXPANSION;

  static {
    PART_ID_EXPANSION = new HashMap<String, String[]>();
    PART_ID_EXPANSION.put("g", Util.stringArray("b", "l", "o"));
    PART_ID_EXPANSION.put("gb", Util.stringArray("b", "o"));
    PART_ID_EXPANSION.put("gl", Util.stringArray("l", "o"));
    PART_ID_EXPANSION.put("b", Util.stringArray("b"));
    PART_ID_EXPANSION.put("l", Util.stringArray("l"));
    PART_ID_EXPANSION.put("o", Util.stringArray("o"));
    PART_ID_EXPANSION.put("v", Util.stringArray("v"));

    ANY_COLOR = new Color();
    ANY_COLOR.ids_ = new String[]{"*"};
    ANY_COLOR.name_ = "Any";
  }
}