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

package com.brickmesh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public final class Util {
  public static int[] intArray(Integer... values) {
    int[] result = new int[values.length];
    for (int i = 0; i < values.length; ++i) {
      result[i] = values[i];
    }
    return result;
  }

  public static short[] shortArray(Short... values) {
    short[] result = new short[values.length];
    for (int i = 0; i < values.length; ++i) {
      result[i] = values[i];
    }
    return result;
  }
  
  public static Object[] objectArray(Object... values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("Must have at least 1 element.");
    }
    Object[] result =
        (Object[])java.lang.reflect.Array.newInstance(values[0].getClass(), values.length);
    for (int i = 0; i < values.length; ++i) {
      result[i] = values[i];
    }
    return result;
  }

  public static String[] stringArray(String... values) {
    return (String[])objectArray((Object[])values);
  }
  
  public static String joinArray(String delimiter, Object[] elements) {
    StringBuffer sb = new StringBuffer();
    boolean d = false;
    for (Object e : elements) {
      if (d) sb.append(delimiter);
      sb.append(e.toString());
      d = true;
    }
    return sb.toString();
  }

  public static String joinIterable(String delimiter, Iterable<?> elements) {
    StringBuffer sb = new StringBuffer();
    boolean d = false;
    for (Object e : elements) {
      if (d) sb.append(delimiter);
      sb.append(e.toString());
      d = true;
    }
    return sb.toString();
  }

  public static String joinStrings(String delimiter, String... values) {
    return joinArray(delimiter, stringArray(values));
  }

  public static long logPhaseTime(String phaseName, long startNanos) {
    long nanos = System.nanoTime();
    Log.perfLog("%s: %d us", phaseName, (nanos - startNanos) / 1000);
    return nanos;
  }
  
  public static long copyStream(InputStream from, OutputStream to)
      throws IOException {
    long total = 0;
    byte[] buf = new byte[2048];
    while (true) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }
  
  private Util() {}
  };