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
    public static Options createLimited() {
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
  
  public WantedLoader createWantedLoader(Options options) {
    return new WantedLoader(options);
  }
  
  public abstract class LoaderBase {
    public LoaderBase(Options options) {
      options_ = options;
      partCounts_ = new TreeMap<String, Integer>();
    }

    public abstract void parse(InputStream input) throws IOException, LoaderException;

    public final Result getResult() {
      if (result_ == null) {
        result_ = new Result();
        RequiredParts parts = new RequiredParts();
        result_.parts_ = parts;
        for (Map.Entry<String, Integer> entry : partCounts_.entrySet()) {
          String[] key = entry.getKey().split("-");
          String partId = key[0];
          String colorId = key[1];
          PartModel.PartGroup group = lookupPart(partId);
          if (group == null) {
            result_.unknownPartIds_ = incrementMapCount(result_.unknownPartIds_, partId);
            continue;
          }
          PartModel.Part preferredPart = lookupPreferredPart(partId);
          PartModel.Color color = preferredPart.color_;
          if (preferredPart.color_ == null) {
            color = lookupColor(colorId);
            if (color == null) {
              result_.unknownColorIds_ = incrementMapCount(result_.unknownColorIds_, colorId);
              continue;
            }
          }
          int quantity = entry.getValue();
          parts.addItem(group, preferredPart, color, quantity);
        }
        if (imageBytes_ != null) {
          result_.imageBytes_ = imageBytes_;
        }
      }
      return result_;
    }
    
    protected abstract PartModel.PartGroup lookupPart(String partId);
    protected abstract PartModel.Part lookupPreferredPart(String partId);
    protected abstract PartModel.Color lookupColor(String colorId);

    protected final void incrementPartCount(String partId, int quantity)
        throws LoaderException {
      Integer count = partCounts_.get(partId);
      if (count == null) {
        partCounts_.put(partId, quantity);
        if (partCounts_.size() > options_.maxNumQty_) {
          throw new LoaderException(String.format(
              "Too many different parts in model (limit=%d)", options_.maxNumQty_));
        }
      } else {
        partCounts_.put(partId, count + quantity);
      }
      totalQty_ += quantity;
      if (totalQty_ > options_.maxTotalQty_) {
        throw new LoaderException(String.format(
            "Too many total parts in model (limit=%d)", options_.maxTotalQty_));
      }
    }
    
    protected final Map<String, Integer> incrementMapCount(
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

    protected Result result_;
    protected byte[] imageBytes_;
    
    private Options options_;
    private int totalQty_;
    private TreeMap<String, Integer> partCounts_;
  }
  
  // Loads RequiredParts from Lego Digital Designer files.
  public class LxfLoader extends LoaderBase {
    public LxfLoader(Options options) {
      super(options);
    }
    
    public void parse(InputStream input) throws IOException, LoaderException {
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
    
    protected PartModel.PartGroup lookupPart(String partId) {
      return partModel_.getPartByLegoIdOrNull(partId);
    }
    
    protected PartModel.Part lookupPreferredPart(String partId) {
      return partModel_.getPreferredPartByLegoIdOrNull(partId);
    }
    
    protected PartModel.Color lookupColor(String colorId) {
      return partModel_.getColorByLegoIdOrNull(colorId);
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
              incrementPartCount(part_ + "-" + color_, 1);
              part_ = null;
              color_ = null;
              --brickDepth_;
            }
            if (qName.equals("Part") && brickDepth_ == 0) {
              incrementPartCount(part_ + "-" + color_, 1);
              part_ = null;
              color_ = null;
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

    private boolean formatCorrect_;
  }

  // Loads RequiredParts from a BrickLink wanted list.
  public class WantedLoader extends LoaderBase {
    public WantedLoader(Options options) {
      super(options);
    }
    
    public void parse(InputStream input) throws LoaderException, IOException {
      try {
        SAXParser parser = parserFactory_.newSAXParser();
    		parser.parse(input, new DefaultHandler() {
    			public void startElement(String uri, String localName, String qName,
              Attributes attributes) {
            if (qName.equals("ITEM")) {
              itemId_ = "";
              color_ = "";
              quantity_ = "";
            }
            mode_ = qName;
    			}
  
          public void characters(char[] ch, int start, int length) {
            if (mode_.equals("ITEMID")) {
              itemId_ = itemId_ + new String(ch, start, length);
            }
            if (mode_.equals("COLOR")) {
              color_ = color_ + new String(ch, start, length);
            }
            if (mode_.equals("MINQTY")) {
              quantity_ = quantity_ + new String(ch, start, length);
            }
          }
  
          public void endElement(String uri, String localName, String qName)
              throws LoaderException {
            if (qName.equals("ITEM")) {
              String itemId = itemId_.trim();
              short color = Short.parseShort(color_.trim());
              int quantity = Integer.parseInt(quantity_.trim());
              incrementPartCount(itemId + "-" + color, quantity);
            }
          }
  
          private String mode_ = "";
          private String itemId_ = "";
          private String color_ = "";
          private String quantity_ = "";
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
    
    protected PartModel.PartGroup lookupPart(String partId) {
      return partModel_.getPartByIdOrNull(partId);
    }
    
    protected PartModel.Part lookupPreferredPart(String partId) {
      return partModel_.getPreferredPartByIdOrNull(partId);
    }
    
    protected PartModel.Color lookupColor(String colorId) {
      return partModel_.getColorByIdOrNull(colorId);
    }
  }
  
  private PartModel partModel_;
  private SAXParserFactory parserFactory_ = SAXParserFactory.newInstance();
}