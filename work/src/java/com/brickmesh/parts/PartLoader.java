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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Result(PartModel partModel) {
      items_ = new RequiredItems(partModel, 128);
      unknownItems_ = new UnknownItems();
    }

    public boolean isEmpty() {
      return items_.isEmpty() && unknownItems_.isEmpty();
    }

    public RequiredItems items_;
    public UnknownItems unknownItems_;
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
    return new LxfLoader(partModel_, options);
  }

  public WantedLoader createWantedLoader(Options options) {
    return new WantedLoader(partModel_, options);
  }

  public abstract class LoaderBase {
    public LoaderBase(PartModel partModel, Options options) {
      options_ = options;
      result_ = new Result(partModel);
    }

    public abstract void parse(InputStream input) throws IOException, LoaderException;

    public final Result getResult() {
      return result_;
    }

    protected abstract String idNamespace();

    protected final void addItem(String partId, List<String> colorIds, int count)
        throws LoaderException {
      if (result_.items_.numDifferentItems() > options_.maxNumQty_) {
        throw new LoaderException(String.format(
            "Too many different parts in model (limit=%d)", options_.maxNumQty_));
      }
      if (result_.items_.numTotalItems() > options_.maxTotalQty_) {
        throw new LoaderException(String.format(
            "Too many total parts in model (limit=%d)", options_.maxTotalQty_));
      }
      result_.items_.addItem(idNamespace(), partId, colorIds, count, result_.unknownItems_);
    }

    private Options options_;
    protected Result result_;
  }

  // Loads RequiredParts from Lego Digital Designer files.
  public class LxfLoader extends LoaderBase {
    public LxfLoader(PartModel partModel, Options options) {
      super(partModel, options);
    }

    public void parse(InputStream input) throws IOException, LoaderException {
      ZipInputStream zis = new ZipInputStream(input);
      try {
        while (true) {
          ZipEntry zipEntry = zis.getNextEntry();
          if (zipEntry == null) {
            throw new IOException("Did not find LXFML entry in input.");
          }
          if (zipEntry.getName().equals("IMAGE100.PNG") &&
              result_.imageBytes_ == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Util.copyStream(zis, bos);
            result_.imageBytes_ = bos.toByteArray();
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

    protected String idNamespace() {
      return "l";
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
              color_ = new ArrayList<String>();
              subPartsWithColor_ = 0;
              count_ = 0;
              ++brickDepth_;
            } else if (qName.equals("Part")) {
              if (brickDepth_ == 0) {
                // Older versions of LXF (e.g. 2.3) do not have Brick
                // elements at all, everything is attached to the Part.
                part_ = attributes.getValue("designID");
                color_ = new ArrayList<String>();
                subPartsWithColor_ = 0;
                count_ = 0;
              }
              String colors = attributes.getValue("materials");
              if (colors == null) {
                colors = attributes.getValue("materialID");
                if (colors == null) {
                  throw new AssertionError("Cannot get color from LXF.");
                }
                color_.add(colors);
              } else {
                if (subPartsWithColor_ == 0) {
                  color_.addAll(Arrays.asList(colors.split(",")));
                } else if (subPartsWithColor_ == 1) {
                  color_ = color_.subList(0, 1);
                  color_.add(colors.split(",")[0]);
                } else {
                  color_.add(colors.split(",")[0]);
                }
              }
              ++subPartsWithColor_;
            }
    			}

    			public void endElement(String uri, String localName, String qName)
              throws LoaderException {
            if (qName.equals("Brick")) {
              addItem(part_, color_, 1);
              part_ = null;
              color_ = null;
              --brickDepth_;
            }
            if (qName.equals("Part") && brickDepth_ == 0) {
              addItem(part_, color_, 1);
              part_ = null;
              color_ = null;
            }
    			}

          private String part_;
          private List<String> color_;
          private int subPartsWithColor_;
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
    public WantedLoader(PartModel partModel, Options options) {
      super(partModel, options);
    }

    public void parse(InputStream input) throws LoaderException, IOException {
      try {
        SAXParser parser = parserFactory_.newSAXParser();
    		parser.parse(input, new DefaultHandler() {
    			public void startElement(String uri, String localName, String qName,
              Attributes attributes) {
            if (qName.equals("ITEM")) {
              color_ = "";
              partId_ = "";
              quantity_ = "";
            }
            mode_ = qName;
    			}

          public void characters(char[] ch, int start, int length) {
            if (mode_.equals("COLOR")) {
              color_ = color_ + new String(ch, start, length);
            }
            if (mode_.equals("ITEMID")) {
              partId_ = partId_ + new String(ch, start, length);
            }
            if (mode_.equals("MINQTY")) {
              quantity_ = quantity_ + new String(ch, start, length);
            }
          }

          public void endElement(String uri, String localName, String qName)
              throws LoaderException {
            if (qName.equals("ITEM")) {
              short color = Short.parseShort(color_.trim());
              String partId = partId_.trim();
              int quantity = Integer.parseInt(quantity_.trim());
              ArrayList<String> colors = new ArrayList<String>();
              colors.add(Short.toString(color));
              addItem(partId, colors, quantity);
            }
          }

          private String mode_ = "";
          private String color_ = "";
          private String partId_ = "";
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

    protected String idNamespace() {
      return "b";
    }
  }

  private PartModel partModel_;
  private SAXParserFactory parserFactory_ = SAXParserFactory.newInstance();
}