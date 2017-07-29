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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.brickmesh.util.Log;

// Poor man's unit test framework.
public class TestCase {
  // Runs all methods on 'this' that start with 'test'.
  // Requires that the following are all public:
  // - the class itself.
  // - the zero-argument contructor.
  // - the test method.
  protected static void runAllTests(Class testCaseClass) {
    boolean hadErrors = false;
    for (Method method : testCaseClass.getDeclaredMethods()) {
      if (!method.getName().startsWith("test") ||
           method.getParameterTypes().length != 0) {
        Log.info("Skipping: " + method.getName());
        continue;
      }
      Log.info("Invoking: " + method.getName());
      try {
        TestCase instance = (TestCase)testCaseClass.newInstance();
        method.invoke(instance);
      }
      catch (InvocationTargetException e) {
        System.err.println("Error in " + method.getName() + ":");
        e.getCause().printStackTrace();
        hadErrors = true;
      }
      catch (Exception e) {
        System.err.println("Error in " + method.getName() + ":");
        e.printStackTrace();
        hadErrors = true;
      }
    }
    if (hadErrors) {
      throw new IllegalStateException("Test case had errors: " + testCaseClass.getName());
    }
  }
  
  protected static void expectTrue(boolean b) {
    if (!b) throw new AssertionError("Test failed.");
  }

  protected static void expectFalse(boolean b) {
    if (b) throw new AssertionError("Test failed.");
  }

  protected static void expectEquals(Object o1, Object o2) {
    if (o1 != null) o1 = normalize(o1);
    if (o2 != null) o2 = normalize(o2);

    if (o1 == null) {
      if (o2 == null) {
        return;
      } else {
        System.err.format("Not equal: null != %s\n", o2.toString());
        throw new AssertionError("Test failed.");
      }
    } else {
      if (o2 == null) {
        System.err.format("Not equal: %s != null\n", o1.toString());
        throw new AssertionError("Test failed.");
      }
    }

    if (o1 instanceof Double && o1 instanceof Double) {
      double d1 = (Double)o1;
      double d2 = (Double)o2;
      if (Math.abs(d1 - d2) > EPSILON) {
        System.err.format("Not equal: %s != %s\n", o1.toString(), o2.toString());
        throw new AssertionError("Test failed.");
      }
    } else if (!o1.equals(o2)) {
      System.err.format("Not equal: %s != %s\n", o1.toString(), o2.toString());
      throw new AssertionError("Test failed.");
    }
  }

  private static final Object normalize(Object o) {
    if (o instanceof Float) {
      return (double)(Float)o;
    }
    if (o instanceof Double) {
      return o;
    }
    if (o instanceof short[]) {
      return Arrays.toString((short[])o);
    }
    if (o instanceof int[]) {
      return Arrays.toString((int[])o);
    }
    if (o instanceof String[]) {
      return Arrays.toString((String[])o);
    }
    if (o instanceof HashSet) {
      return sortedToString((HashSet<?>)o);
    }
    if (o instanceof HashMap) {
      return sortedToString((HashMap<?, ?>)o);
    }
    if (o instanceof Collection) {
      return Arrays.deepToString(((Collection)o).toArray());
    }
    return o.toString();
  }

  private static <K, V> String sortedToString(Map<K, V> map) {
    TreeMap<K, V> t = new TreeMap<K, V>();
    t.putAll(map);
    return t.toString();
  }

  private static <V> String sortedToString(Set<V> set) {
    TreeSet<V> t = new TreeSet<V>();
    t.addAll(set);
    return t.toString();
  }

  private static final double EPSILON = 1e-5;
};
