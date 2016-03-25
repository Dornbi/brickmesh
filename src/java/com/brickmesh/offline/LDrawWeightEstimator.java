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

package com.brickmesh.offline;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

// Computes an estimate for the weight of an LDraw part in grams
// based on the geometry of the part.
//
// It is based on:
// http://stackoverflow.com/questions/1406029
//
// Note that the algorithm works "perfectly" only for closed meshes,
// which is typically not the case for LDraw parts. We use a custom
// version of the most common error sources, for the rest we just
// deal with the error.
public final class LDrawWeightEstimator {
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Not enough args.");
      System.err.println("Usage: <command> <ldraw-path> <ldraw-filename> [...]\n");
      return;
    }

    LDrawWeightEstimator l = new LDrawWeightEstimator(args[0]);
    for (int i = 1; i < args.length; ++i) {
      Result r = l.partWeightGrams(args[i]);
      System.out.format("%.3f gram(s), %.3f%% error\n",
          r.weightGrams(), 100.0 * r.errorMargin());
    }
  }
  
  public static class Result {
    public Result() {
      w1_ = 0.0;
      w2_ = 0.0;
    }
    
    public Result(double w1, double w2) {
      w1_ = w1;
      w2_ = w2;
    }
    
    public double weightGrams() {
      return w1_;
    }
    
    public boolean isExact() {
      return Math.abs(w1_ - w2_) < 1e-10;
    }
    
    public double errorMargin() {
      return Math.abs(w2_ - w1_) / w1_;
    }
    
    public String toString() {
      return w1_ + ", " + w2_;
    }
    
    public void add(Result other) {
      w1_ += other.w1_;
      w2_ += other.w2_;
    }

    private double w1_;
    private double w2_;
  }
  
  public LDrawWeightEstimator(String ldrawPath) {
    includeDirs_ = new File[] {
      new File("."),
      new File("src/ldraw"),
      new File("src/ldraw-unofficial"),
      new File(ldrawPath, "PARTS"),
      new File(ldrawPath, "P" ),
    };
  }

  public Result partWeightGrams(String filename) throws IOException {
    final double ldrToCm = 0.04;
    final double absDensityGramPerCm3 = 1.052;
    final double f = ldrToCm * ldrToCm * ldrToCm * absDensityGramPerCm3;
    
    Matrix3 noop = new Matrix3();
    ArrayList<Triangle> triangles = loadTriangles(filename, noop, +1);
    Vector3 min = minCorner(triangles);
    Vector3 max = maxCorner(triangles);
    
    Matrix3 center = center(min, max);
    double v1 = Math.abs(signedVolume(center, triangles));

    Matrix3 shifted = center(min, max);
    shifted.t_ = shifted.t_.plus(max.minus(min));
    double v2 = Math.abs(signedVolume(shifted, triangles));
    return new Result(v1 * f, v2 * f);
  }

  private LineNumberReader open(String filename) throws IOException {
    filename = filename.replaceAll("\\\\", "/");
    for (File dir : includeDirs_) {
      File f = new File(dir, filename);
      //System.out.println("Trying: " + f.getPath());
      if (f.exists()) {
        //System.out.println("Using: " + f.getPath());
        return new LineNumberReader(new FileReader(f));
      }
    }
    throw new IOException("No path found for file: " + filename);
  }
  
  private ArrayList<Triangle> loadTriangles(String filename, Matrix3 m, int invertSign)
      throws IOException {
    ArrayList<Triangle> result = new ArrayList<Triangle>();
    LineNumberReader reader = open(filename);
    int localSign = 1;
    int invertLines = 0;
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      String[] tokens = line.replaceFirst("^ +", ""). split(" +");
      if (tokens.length == 0) continue;

      if (tokens[0].equals("0") && tokens.length > 1) {
        // Meta directive. We are only interested in clockwise-related ones.
        if (!parseToken(tokens, 1, reader, filename).equals("BFC")) continue;
        String bfcToken = parseToken(tokens, 2, reader, filename);
        if (bfcToken.equals("INVERTNEXT")) {
          invertLines = 1;
          continue;
        } else if (bfcToken.equals("CERTIFY")) {
          String cwToken = parseToken(tokens, 3, reader, filename);
          if (cwToken.equals("CW")) {
            localSign = +m.sign() * invertSign;
          } else if (cwToken.equals("CCW")) {
            localSign = -m.sign() * invertSign;
          }
        }
      }
  
      if (tokens[0].equals("1")) {
        // Include a sub-file.
        Vector3 t = parseVector3(tokens, 2, reader, filename);
        Vector3 x = parseVector3(tokens, 5, reader, filename);
        Vector3 y = parseVector3(tokens, 8, reader, filename);
        Vector3 z = parseVector3(tokens, 11, reader, filename);
        String subFilename = parseToken(tokens, 14, reader, filename);
        Matrix3 subM = new Matrix3(x, y, z, t);
        Matrix3 tM = m.transformMatrix(subM);
        //System.out.println("tM=" + tM.toString());
        result.addAll(loadTriangles(subFilename, tM,
            invertLines > 0 ? -invertSign : invertSign));
      } else if (tokens[0].equals("3")) {
        // Triangle.
        Vector3 v1 = parseVector3(tokens, 2, reader, filename);
        Vector3 v2 = parseVector3(tokens, 5, reader, filename);
        Vector3 v3 = parseVector3(tokens, 8, reader, filename);
        if (localSign != +1 && localSign != -1) {
          throw new AssertionError("Unknown sign in file " + filename);
        }
        result.add(m.transformTriangle(new Triangle(v1, v2, v3, localSign)));
      } else if (tokens[0].equals("4")) {
        // Quad.
        Vector3 v1 = parseVector3(tokens, 2, reader, filename);
        Vector3 v2 = parseVector3(tokens, 5, reader, filename);
        Vector3 v3 = parseVector3(tokens, 8, reader, filename);
        Vector3 v4 = parseVector3(tokens, 11, reader, filename);
        if (localSign != +1 && localSign != -1) {
          throw new AssertionError("Unknown sign in file " + filename);
        }
        result.add(m.transformTriangle(new Triangle(v1, v2, v3, localSign)));
        result.add(m.transformTriangle(new Triangle(v3, v4, v1, localSign)));
      }
      if (invertLines > 0) --invertLines;
    }
    return result;
  }
  
  private double signedVolume(Matrix3 m, ArrayList<Triangle> triangles) {
    double volume = 0.0;
    for (Triangle t : triangles) {
      Vector3 p1 = m.transformVector(t.p1_);
      Vector3 p2 = m.transformVector(t.p2_);
      Vector3 p3 = m.transformVector(t.p3_);
      //System.out.println("sign = 1");
      //System.out.println(p1);
      //System.out.println(p2);
      //System.out.println(p3);
      double v321 = p3.x_ * p2.y_ * p1.z_;
      double v231 = p2.x_ * p3.y_ * p1.z_;
      double v312 = p3.x_ * p1.y_ * p2.z_;
      double v132 = p1.x_ * p3.y_ * p2.z_;
      double v213 = p2.x_ * p1.y_ * p3.z_;
      double v123 = p1.x_ * p2.y_ * p3.z_;
      double vol = (-v321 + v231 + v312 - v132 - v213 + v123) / 6.0;
      //System.out.println(vol);
      volume += vol;
    }
    return volume;
  }

  private static Vector3 minCorner(ArrayList<Triangle> triangles) {
    double minx, miny, minz;
    minx = miny = minz = Double.MAX_VALUE;
    for (Triangle t : triangles) {
      for (Vector3 v : new Vector3[]{t.p1_, t.p2_, t.p3_}) {
        minx = Math.min(minx, v.x_);
        miny = Math.min(miny, v.y_);
        minz = Math.min(minz, v.z_);
      }
    }
    return new Vector3(minx, miny, minz);
  }

  private static Vector3 maxCorner(ArrayList<Triangle> triangles) {
    double maxx, maxy, maxz;
    maxx = maxy = maxz = Double.MIN_VALUE;
    for (Triangle t : triangles) {
      for (Vector3 v : new Vector3[]{t.p1_, t.p2_, t.p3_}) {
        maxx = Math.max(maxx, v.x_);
        maxy = Math.max(maxy, v.y_);
        maxz = Math.max(maxz, v.z_);
      }
    }
    return new Vector3(maxx, maxy, maxz);
  }

  private static Matrix3 center(Vector3 min, Vector3 max) {
    Matrix3 m = new Matrix3();
    m.t_ = new Vector3(
      (min.x_ - max.x_) / 2,
      (min.y_ - max.y_) / 2,
      (min.z_ - max.z_) / 2);
    return m;
  }

  private static String parseToken(String[] tokens, int pos,
      LineNumberReader reader, String filename) {
    if (tokens.length <= pos) {
      throw new AssertionError("Not enough tokens in " + filename +
          ", line " + reader.getLineNumber());
    }
    return tokens[pos];
  }
  
  public static Vector3 parseVector3(String[] tokens, int pos,
      LineNumberReader reader, String filename) {
    if (tokens.length <= pos + 2) {
      throw new AssertionError("Not enough tokens for vector in " + filename +
          ", line " + reader.getLineNumber());
    }
    Vector3 v = new Vector3(
      Double.parseDouble(tokens[pos]),
      Double.parseDouble(tokens[pos + 1]),
      Double.parseDouble(tokens[pos + 2]));
    return v;
  }
  
  private static class Vector3 {
    public Vector3() {
      x_ = 0.0;
      y_ = 0.0;
      z_ = 0.0;
    }
    
    public Vector3(double x, double y, double z) {
      x_ = x;
      y_ = y;
      z_ = z;
    }

    public Vector3 minus(Vector3 v) {
      return new Vector3(x_ - v.x_, y_ - v.y_, z_ - v.z_);
    }

    public Vector3 plus(Vector3 v) {
      return new Vector3(x_ + v.x_, y_ + v.y_, z_ + v.z_);
    }
    
    public Vector3 crossProduct(Vector3 v) {
      return new Vector3(
          y_ * v.z_ - z_ * v.y_,
          z_ * v.x_ - x_ * v.z_,
          x_ * v.y_ - y_ * v.x_);
    }
    
    public double dotProduct(Vector3 v) {
      return x_ * v.x_ + y_ * v.y_ + z_ * v.z_;
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("(");
      sb.append(x_);
      sb.append(",");
      sb.append(y_);
      sb.append(",");
      sb.append(z_);
      sb.append(")");
      return sb.toString();
    }
    
    public final double x_, y_, z_;
  }
  
  private static class Triangle {
    public Triangle(Vector3 p1, Vector3 p2, Vector3 p3, int sign) {
      if (sign > 0) {
        p1_ = p1;
        p2_ = p2;
        p3_ = p3;
      } else {
        p1_ = p1;
        p2_ = p3;
        p3_ = p2;
      }
    }
    
    public final Vector3 p1_;
    public final Vector3 p2_;
    public final Vector3 p3_;
  }

  private static class Matrix3 {
    public Matrix3() {
      vx_ = new Vector3(1.0, 0.0, 0.0);
      vy_ = new Vector3(0.0, 1.0, 0.0);
      vz_ = new Vector3(0.0, 0.0, 1.0);
      t_ = new Vector3();
    }

    public Matrix3(Vector3 vx, Vector3 vy, Vector3 vz, Vector3 t) {
      vx_ = vx;
      vy_ = vy;
      vz_ = vz;
      t_ = t;
    }

    public int sign() {
      Vector3 n = vy_.minus(vx_).crossProduct(vz_.minus(vx_));
      double p = n.dotProduct(vx_);
      return p > 0.0 ? +1 : -1;
    }
       
    public Vector3 transformVector(Vector3 orig) {
      return new Vector3(
          vx_.x_ * orig.x_ + vx_.y_ * orig.y_ + vx_.z_ * orig.z_ + t_.x_,
          vy_.x_ * orig.x_ + vy_.y_ * orig.y_ + vy_.z_ * orig.z_ + t_.y_,
          vz_.x_ * orig.x_ + vz_.y_ * orig.y_ + vz_.z_ * orig.z_ + t_.z_);
    }
    
    public Vector3 transformNormal(Vector3 orig) {
      return new Vector3(
          vx_.x_ * orig.x_ + vx_.y_ * orig.y_ + vx_.z_ * orig.z_,
          vy_.x_ * orig.x_ + vy_.y_ * orig.y_ + vy_.z_ * orig.z_,
          vz_.x_ * orig.x_ + vz_.y_ * orig.y_ + vz_.z_ * orig.z_);
    }

    public Triangle transformTriangle(Triangle orig) {
      return new Triangle(
          transformVector(orig.p1_),
          transformVector(orig.p2_),
          transformVector(orig.p3_),
          +1);
    }

    public Matrix3 transformMatrix(Matrix3 orig) {
      Matrix3 m = new Matrix3(
          transformNormal(orig.vx_),
          transformNormal(orig.vy_),
          transformNormal(orig.vz_),
          transformVector(orig.t_));
      return m;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("(vx=");
      sb.append(vx_.toString());
      sb.append(",vy=");
      sb.append(vy_.toString());
      sb.append(",vz=");
      sb.append(vz_.toString());
      sb.append(",t=");
      sb.append(t_.toString());
      sb.append(")");
      return sb.toString();
    }
    
    public Vector3 vx_, vy_, vz_;
    public Vector3 t_;
  }
  
  private File[] includeDirs_;
};