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

// The Part model contains all known parts and relationships between them.
// It is loaded from the PartModel proto files.
public class PartModel {
  // A single color.
  public static class Color {
    // The Bricklink ID of the color.
    public String id_;
    
    // A human-readable name of the color.
    public String name_;
    
    // The LEGO ID of the color.
    public String[] legoId_;
  }

  // A single part that shops have inventory of.
  public static class Part {
    
    // For parts composed from sub-parts, this class holds info
    // about the sub-parts.
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

    // The Bricklink id of the part.    
    public String id_;
    
    // Alternate id(s) of the part or null.
    public String[] alternateId_;
    
    // The weight in grams. Must be nonzero after loading.
    public float weightGrams_;

    // If this is non-null then this part can only exist in this color.
    // This is true in the following cases:
    // 1. If a part uses the force_color_id field.
    // 2. If the Part contains a superset of the part in the PartGroup
    //    but the superset contains this part only in that color.
    public Color color_;
    
    // If this is a composite part made from other parts, this is the
    // list of all the parts that it is made from.
    public Item[] item_;
    
    public Part partialClone() {
      Part part = new Part();
      part.id_ = id_;
      part.weightGrams_ = weightGrams_;
      // Contained items and alternate ids are deliberately not cloned.
      return part;
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(id_);
      if (alternateId_ != null) {
        sb.append(" (");
        sb.append(Util.join(",", alternateId_));
        sb.append(")");
      }
      sb.append(String.format(": weight=%.3f", weightGrams_));
      if (item_ != null) {
        sb.append(" items:[");
        sb.append(Util.join(",", item_));
        sb.append("]");
      }
      return sb.toString();
    }
  }

  // A group of parts that are treated as equivalent.
  public static class PartGroup {
    public ArrayList<Part> part_ = new ArrayList<Part>();
   
    public String toString() {
      return Util.join("; ", part_);
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

  public Color getColorById(String colorId) {
    Color color = getColorByIdOrNull(colorId);
    if (color == null) {
      throw new AssertionError(String.format("Unknown color %s", colorId));
    }
    return color;
  }

  public Color getColorByIdOrNull(String colorId) {
    return colorIdMap_.get(colorId);
  }

  public Color getColorByLegoIdOrNull(String legoColorId) {
    return legoColorIdMap_.get(legoColorId);
  }

  public PartGroup getPartByIdOrNull(String partId) {
    return partMap_.get(partId);
  }

  public PartGroup getPartByLegoIdOrNull(String legoPartId) {
    String partId = legoIdMap_.get(legoPartId);
    if (partId == null) {
      partId = legoPartId;
    }
    return getPartByIdOrNull(partId);
  }
  
  public Part getPreferredPartByIdOrNull(String partId) {
    PartGroup group = partMap_.get(partId);
    for (Part part : group.part_) {
      if (part.id_.equals(partId)) {
        return part;
      }
      if (part.alternateId_ != null) {
        for (String alternateId : part.alternateId_) {
          if (alternateId.equals(partId)) {
            return part;
          }
        }
      }
    }
    return null;
  }

  public Part getPreferredPartByLegoIdOrNull(String legoPartId) {
    String partId = legoIdMap_.get(legoPartId);
    if (partId == null) {
      partId = legoPartId;
    }
    return getPreferredPartByIdOrNull(partId);
  }
  
  private PartModel(Reader colorReader, Reader partReader) {
    loadColors(colorReader);
    loadParts(partReader);
  }
  
  private void loadColors(Reader reader) {
    try {
      boolean error = false;
      BufferedReader br = new BufferedReader(reader);
      PartModelProto.ColorModel.Builder builder =
          PartModelProto.ColorModel.newBuilder();
      TextFormat.merge(br, builder);
      PartModelProto.ColorModel modelProto = builder.build();

      ArrayList<Color> colorArray = new ArrayList<Color>(modelProto.getColorCount());
      colorIdMap_ = new HashMap<String, Color>(modelProto.getColorCount());
      legoColorIdMap_ = new HashMap<String, Color>(modelProto.getColorCount());

      for (PartModelProto.Color colorProto : modelProto.getColorList()) {
        Color color = new Color();
        color.id_ = colorProto.getId();
        color.name_ = colorProto.getName();
        color.legoId_ = colorProto.getLegoIdList().toArray(
            new String[colorProto.getLegoIdCount()]);
        colorArray.add(color);
        if (colorIdMap_.put(color.id_, color) != null) {
          System.err.println("Duplicate color id: " + color.id_);
          error = true;
        }
        for (String legoId : color.legoId_) {
          if (legoColorIdMap_.put(legoId, color) != null) {
            System.err.println("Duplicate color lego id: " + color.legoId_);
            error = true;
          }
        }
      }
      if (error) {
        throw new AssertionError("Duplicate color id(s) found");
      }

      color_ = colorArray.toArray(new Color[colorArray.size()]);
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
      
      partMap_ = new HashMap<String, PartGroup>(modelProto.getPartCount());
      legoIdMap_ = new HashMap<String, String>(modelProto.getPartCount() / 10);
      
      // This map contains items that are similar. Similar items are put into
      // the same PartGroup and are freely replaceable other parts in the same
      // group.
      HashMap<String, String> similarMap = new HashMap<String, String>(
          modelProto.getPartCount());
      
      // Contains generic relations. This is basically the same as a similar
      // relation except for weight: the weight of generic items is derived
      // from the more specific items.
      HashMap<String, ArrayList<String>> genericMap =
          new HashMap<String, ArrayList<String>>(
          modelProto.getPartCount() / 4);
      
      // Contains kindof relations. This is similar to the generic relation
      // but is asymmetric: the more generic 'kindof' item can be replaced
      // by the less generic one but not the other way around.
      HashMap<String, ArrayList<String>> kindofMap =
          new HashMap<String, ArrayList<String>>(
          modelProto.getPartCount() / 4);

      // Stage 1: load all parts as a distinct group.
      loadPartProto(modelProto, similarMap, genericMap, kindofMap);
      Log.info("partMap_.size()=" + partMap_.size());

      // Stage 2: Populate contained parts. This is a separate stage to make
      // sure that all parts are loaded.
      populateContainedParts(modelProto);

      // Stage 3: Compute weights.
      computeAllWeights(genericMap, kindofMap);
      
      // Stage 4: Merge all similar groups into the same PartGroup.
      mergeSimilarGroups(similarMap, genericMap);
      HashSet<PartGroup> uniqueGroups = new HashSet<PartGroup>(partMap_.values());
      Log.info("uniqueGroups.size()=" + uniqueGroups.size());

      // Stage 5: Add all alternate and contained ids.
      addAlternateIds(uniqueGroups);
      Log.info("partMap_.size()=" + partMap_.size());
      
      // Stage 6: Prefer Lego ids.
      preferLegoIds();
      
      // Test the result.
      checkInvariants();
    }
    catch (IOException e) {
      throw new AssertionError(e);
    }
  }
  
  private void loadPartProto(PartModelProto.PartModel modelProto,
                             HashMap<String, String> similarMap,
                             HashMap<String, ArrayList<String>> genericMap,
                             HashMap<String, ArrayList<String>> kindofMap) {
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      Part part = new Part();
      part.id_ = partProto.getId();
      if (partProto.hasForceColorId()) {
        part.color_ = getColorById(partProto.getForceColorId());
      }
      if (partProto.getAlternateIdCount() > 0) {
        part.alternateId_ = partProto.getAlternateIdList().toArray(
            new String[partProto.getAlternateIdCount()]);
      }
      part.weightGrams_ = partProto.getWeightGrams();
      for (String legoId : partProto.getLegoIdList()) {
        legoIdMap_.put(legoId, part.id_);
      }
      for (String similarId : partProto.getSimilarIdList()) {
        if (part.id_.equals(similarId)) {
          throw new AssertionError("Part similar to self: " + similarId);
        }
        similarMap.put(part.id_, similarId);
      }
      if (partProto.hasGenericId()) {
        if (part.id_.equals(partProto.getGenericId())) {
          throw new AssertionError("Part generic to self: " + partProto.getGenericId());
        }
        ArrayList<String> list = genericMap.get(partProto.getGenericId());
        if (list == null) {
          list = new ArrayList<String>();
          genericMap.put(partProto.getGenericId(), list);
        }
        list.add(part.id_);
      }
      if (partProto.hasKindofId()) {
        if (part.id_.equals(partProto.getKindofId())) {
          throw new AssertionError("Part kindof to self: " + partProto.getKindofId());
        }
        ArrayList<String> list = kindofMap.get(partProto.getKindofId());
        if (list == null) {
          list = new ArrayList<String>();
          kindofMap.put(partProto.getKindofId(), list);
        }
        list.add(part.id_);
      }
      PartGroup group = new PartGroup();
      group.part_.add(part);
      if (partMap_.put(part.id_, group) != null) {
        throw new AssertionError("Duplicate id: " + part.id_);
      }
    }
  }
  
  private void populateContainedParts(PartModelProto.PartModel modelProto) {
    for (PartModelProto.Part partProto : modelProto.getPartList()) {
      if (partProto.getItemCount() == 0) continue;
      Part containingPart = partMap_.get(partProto.getId()).part_.get(0);
      if (containingPart.weightGrams_ > 0.0f) {
        throw new AssertionError("Composite item has weight: " + partProto.getId());
      }
      Part.Item[] items = new Part.Item[partProto.getItemCount()];
      for (int i = 0 ; i < partProto.getItemCount(); ++i) {
        Part.Item item = new Part.Item();
        item.part_ = partMap_.get(partProto.getItem(i).getId()).part_.get(0);
        if (item.part_ == containingPart) {
          throw new AssertionError("Item contains self: " + partProto.getId());
        }
        if (partProto.getItem(i).hasColor()) {
          item.color_ = getColorById(partProto.getItem(i).getColor());
        }
        item.count_ = partProto.getItem(i).getCount();
        items[i] = item;
      }
      containingPart.item_ = items;
    }
  }

  private void computeAllWeights(HashMap<String, ArrayList<String>> genericMap,
                                 HashMap<String, ArrayList<String>> kindofMap) {
    preVerifyWeights(genericMap, kindofMap);
    for (PartGroup group : partMap_.values()) {
      for (Part part : group.part_) {
        computeWeight(part, genericMap, kindofMap);
      }
    }
  }

  private void preVerifyWeights(HashMap<String, ArrayList<String>> genericMap,
                                HashMap<String, ArrayList<String>> kindofMap) {
    for (PartGroup group : partMap_.values()) {
      for (Part part : group.part_) {
        if (part.item_ != null ||
            genericMap.containsKey(part.id_) ||
            kindofMap.containsKey(part.id_)) {
          if (part.weightGrams_ > 0.0f) {
            throw new AssertionError("Part must not contain weight: " + part.id_);
          }
        } else {
          if (part.weightGrams_ <= 0.0f) {
            throw new AssertionError("Part must contain weight: " + part.id_);
          }
        }
      }
    }                              
  }
  
  private void computeWeight(Part part,
                             HashMap<String, ArrayList<String>> genericMap,
                             HashMap<String, ArrayList<String>> kindofMap) {
    if (part.weightGrams_ > 0.0f) return;
    float weightGrams = 0.0f;
    if (part.item_ != null) {
      for (Part.Item item : part.item_) {
        computeWeight(item.part_, genericMap, kindofMap);
        weightGrams += item.part_.weightGrams_;
      }
      part.weightGrams_ = weightGrams;
      return;
    }
    ArrayList<String> specificIds = genericMap.get(part.id_);
    if (specificIds == null) {
      specificIds = kindofMap.get(part.id_);
    }
    if (specificIds == null) {
      throw new AssertionError("No way to compute weight: " + part.id_);
    }
    for (String specificId : specificIds) {
      Part specificPart = partMap_.get(specificId).part_.get(0);
      computeWeight(specificPart, genericMap, kindofMap);
      weightGrams += specificPart.weightGrams_;
    }
    part.weightGrams_ = weightGrams / specificIds.size();
  }

  private void mergeSimilarGroups(HashMap<String, String> similarMap,
                                  HashMap<String, ArrayList<String>> genericMap) {
    int max = 0;
    String maxId = "";
    for (Map.Entry<String, String> entry : similarMap.entrySet()) {
      int size = mergeGroups(entry.getKey(), entry.getValue());
      if (size > max) {
        max = size;
        maxId = entry.getKey();
      }
    }
    for (Map.Entry<String, ArrayList<String>> entry : genericMap.entrySet()) {
      for (String other : entry.getValue()) {
        int size = mergeGroups(entry.getKey(), other);
        if (size > max) {
          max = size;
          maxId = entry.getKey();
        }
      }
    }
    Log.info("maxId=" + maxId + " max=" + max);
  }
  
  private int mergeGroups(String group1Id, String group2Id) {
    PartGroup group1 = partMap_.get(group1Id);
    if (group1 == null) {
      throw new AssertionError("Unknown group to merge: " + group1Id);
    }
    PartGroup group2 = partMap_.get(group2Id);
    if (group2 == null) {
      throw new AssertionError("Unknown similar id: " + group2Id);
    }
    if (group1 == group2) {
      throw new AssertionError("Cannot merge a group into itself: " +
          group1Id + ", " + group2Id);
    }
    group1.part_.addAll(group2.part_);
    for (Part part : group1.part_) {
      partMap_.put(part.id_, group1);
    }
    return group1.part_.size();
  }

  private void populateKindOfGroups(HashMap<String, String> kindofMap) {
    for (Map.Entry<String, String> entry : kindofMap.entrySet()) {
      PartGroup group1 = partMap_.get(entry.getKey());
      if (group1 == null) {
        throw new AssertionError("Unknown kindof id: " + entry.getKey());
      }
      PartGroup group2 = partMap_.get(entry.getValue());
      if (group2 == null) {
        throw new AssertionError("Unknown kindof id: " + entry.getValue());
      }
      group2.part_.addAll(group1.part_);
    }
  }
  
  private void addAlternateIds(HashSet<PartGroup> uniqueGroups) {
    for (PartGroup group : uniqueGroups) {
      for (Part part : group.part_) {
        if (part.alternateId_ != null) {
          for (String alternateId : part.alternateId_) {
            if (partMap_.put(alternateId, group) != null) {
              throw new AssertionError("Duplicate alternate id: " + alternateId);
            }
          }
        }
        if (part.item_ != null) {
          for (Part.Item item : part.item_) {
            if (item.part_.item_ != null) {
              throw new AssertionError(
                  "Double containment hierarchy not allowed: " + part.id_);
            }
            PartGroup itemGroup = partMap_.get(item.part_.id_);
            Part itemPart = part.partialClone();
            itemPart.color_ = item.color_;
            itemGroup.part_.add(itemPart); 
          }
        }
      }
    }
  }
  
  private void preferLegoIds() {
    for (String legoId : legoIdMap_.values()) {
      PartGroup group = partMap_.get(legoId);
      for (int i = 0; i < group.part_.size(); ++i) {
        if (i > 0 && group.part_.get(i).id_.equals(legoId)) {
          Part swap = group.part_.get(i);
          group.part_.set(i, group.part_.get(0));
          group.part_.set(0, swap);
        }
      }
    }
  }
  
  private void checkInvariants() {
    for (Map.Entry<String, PartGroup> entry : partMap_.entrySet()) {
      String partId = entry.getKey();

      // Test that the key occurs in the PartGroup.
      if (getPreferredPartByIdOrNull(partId) == null) {
        throw new AssertionError("Part not found: " + partId);
      }
      
      for (Part part : entry.getValue().part_) {
        if (part.weightGrams_ == 0.0f) {
          throw new AssertionError("Zero weight: " + part.id_);
        }
      }
    }
  }
  
  private Color[] color_;
  private HashMap<String, Color> colorIdMap_;
  private HashMap<String, Color> legoColorIdMap_;

  private Part[] part_;
  private PartGroup[] group_;
  private HashMap<String, PartGroup> partMap_;
  private HashMap<String, String> legoIdMap_;

  private static PartModel model_;
}