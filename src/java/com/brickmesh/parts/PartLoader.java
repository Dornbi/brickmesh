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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.brickmesh.util.Util;

public final class PartLoader {
  public static final class Options {
    public static Options createDefault() {
      Options options = new Options();
      options.maxNumQty_ = 1000;
      options.maxTotalQty_ = 500000;
      return options;
    }

    public static Options createUnlimited() {
      Options options = new Options();
      options.maxNumQty_ = Integer.MAX_VALUE;
      options.maxTotalQty_ = Integer.MAX_VALUE;
      return options;
    }
    
    public int maxNumQty_;
    public int maxTotalQty_;
  }
  
  public static final class Result {
    public RequiredParts parts_;
    public Map<String, Integer> unknownPartIds_;
    public Map<String, Integer> unknownColorIds_;
    public byte[] imageBytes_;
  }

  // Thrown when the input is a valid LDD file, but there is 
  // something else wrong (for example some limit has been exceeded).
  public class LoaderException extends SAXException {
    public LoaderException(String s) { super(s); }
  }
  
  public PartLoader() {
    partModel_ = PartModel.getModel();
  }

  public PartLoader(PartModel partModel) {
    partModel_ = partModel;
  }
  
  public LxfLoader createLxfLoader(Options options) {
    return new LxfLoader(options);
  }
  
  public class LxfLoader {
    public LxfLoader(Options options) {
      options_ = options;
      partCounts_ = new TreeMap<String, Integer>();
    }
    
    public void parse(InputStream input) throws
        LoaderException, IOException {
      if (result_ != null) {
        throw new AssertionError("Already has result, cannot parse more.");
      }
      ZipInputStream zis = new ZipInputStream(input);
      try {
        while (true) {
          ZipEntry zipEntry = zis.getNextEntry();
          if (zipEntry == null) {
            throw new IOException("Did not find LXFML entry in input.");
          }
          if (zipEntry.getName().equals("IMAGE100.PNG") &&
              imageBytes_ == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyStream(zis, bos);
            imageBytes_ = bos.toByteArray();
          }
          if (zipEntry.getName().equals("IMAGE100.LXFML")) {
            parseXml(zis);
            return;
          }
        }
      }
      finally {
        zis.close();  
      }
    }
    
    public Result getResult() {
      if (result_ == null) {
        result_ = new Result();
        RequiredParts parts = new RequiredParts();
        result_.parts_ = parts;
        for (Map.Entry<String, Integer> entry : partCounts_.entrySet()) {
          String[] key = entry.getKey().split("-");
          String colorId = key[0];
          String partId = key[1];
          PartModel.PartGroup group = partModel_.getPartByLegoIdOrNull(partId);
          if (group == null) {
            result_.unknownPartIds_ = incrementCount(result_.unknownPartIds_, partId);
            continue;
          }
          PartModel.Part preferredPart = partModel_.getPreferredPartByLegoId(partId);
          PartModel.Color color = preferredPart.color_;
          if (preferredPart.color_ == null) {
            color = partModel_.getColorByLegoIdOrNull(colorId);
            if (color == null) {
              result_.unknownColorIds_ = incrementCount(result_.unknownColorIds_, colorId);
              continue;
            }
          }
          int quantity = entry.getValue();
          parts.addItem(group, preferredPart, color, quantity);
        }
        result_.imageBytes_ = imageBytes_;
      }
      return result_;
    }
    
    private void parseXml(InputStream input) throws IOException, LoaderException {
      try {
        SAXParser parser = parserFactory_.newSAXParser();
    		parser.parse(input, new DefaultHandler() {
    			public void startElement(String uri, String localName, String qName,
              Attributes attributes) throws SAXException {
            if (!formatCorrect_) {
              if (qName.equals("LXFML")) {
                formatCorrect_ = true;
              } else {
                throw new SAXException("Bad LXFML format: " + qName);
              }
            } else if (qName.equals("Brick")) {
              part_ = attributes.getValue("designID");
              color_ = null;
              count_ = 0;
              ++brickDepth_;
            } else if (qName.equals("Part")) {
              if (brickDepth_ == 0) {
                // Older versions of LXF (e.g. 2.3) do not have Brick
                // elements at all, everything is attached to the Part.
                part_ = attributes.getValue("designID");
                color_ = null;
                count_ = 0;
              }
              if (color_ == null) {
                String colors = attributes.getValue("materials");
                if (colors == null) {
                  color_ = attributes.getValue("materialID");
                } else {
                  color_ = colors.split(",")[0];
                }
                if (color_ == null) {
                  throw new AssertionError("Cannot get color from LXF.");
                }
              }
            }
    			}

    			public void endElement(String uri, String localName, String qName)
              throws LoaderException {
            if (qName.equals("Brick")) {
              incrementCount(color_ + "-" + part_);
              part_ = null;
              color_ = null;
              --brickDepth_;
            }
            if (qName.equals("Part") && brickDepth_ == 0) {
              incrementCount(color_ + "-" + part_);
              part_ = null;
              color_ = null;
            }
    			}
          
          private void incrementCount(String partId) throws LoaderException {
            Integer count = partCounts_.get(partId);
            if (count == null) {
              partCounts_.put(partId, 1);
              if (partCounts_.size() > options_.maxNumQty_) {
                throw new LoaderException(String.format(
                    "Too many different parts in model (limit=%d)", options_.maxNumQty_));
              }
            } else {
              partCounts_.put(partId, count + 1);
            }
            if (++totalQty_ > options_.maxTotalQty_) {
              throw new LoaderException(String.format(
                  "Too many total parts in model (limit=%d)", options_.maxTotalQty_));
            }
          }
          
          private String part_;
          private String color_;
          private int count_;
          private int brickDepth_;
    		});
      }
      catch (ParserConfigurationException ex) {
        throw new IOException(ex);
      }
      catch (LoaderException ex) {
        throw ex;
      }
      catch (SAXException ex) {
        throw new IOException(ex);
      }
    }
    
    private Map<String, Integer> incrementCount(
        Map<String, Integer> map, String key) {
      if (map == null) {
        map = new TreeMap<String, Integer>();
      }
      Integer count = map.get(key);
      if (count == null) {
        map.put(key, 1);
      } else {
        map.put(key, count + 1);
      }
      return map;
    }
    
    private Options options_;
    private boolean formatCorrect_;
    private int totalQty_;
    private TreeMap<String, Integer> partCounts_;
    private Result result_;
    private byte[] imageBytes_;
  }
  
  private PartModel partModel_;
  private SAXParserFactory parserFactory_ = SAXParserFactory.newInstance();
}