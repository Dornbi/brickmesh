#!/opt/local/bin/python2.7

# Copyright (c) 2016, Peter Dornbach
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above copyright
#       notice, this list of conditions and the following disclaimer in the
#       documentation and/or other materials provided with the distribution.
#     * Neither the name BrickMesh nor the names of its contributors may be used
#       to endorse or promote products derived from this software without
#       specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
# DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import glob

import part_model_proto_pb2

from google.protobuf import text_format


def LoadCsvWeights(path):
  weight_map = {}
  with open(path) as f:
    for line in f.readlines():
      line = line.strip().split('#')[0]
      if not line:
        continue
      part_id, weight_str = line.split(',')
      weight = float(weight_str)
      weight_map[part_id] = weight
  return weight_map
        

def ReadPartModel(path):
  with open(path) as f:
    content = f.read()
    part_model = part_model_proto_pb2.PartModel()
    try:
      text_format.Merge(content, part_model)
    except:
      print path
      raise
    return part_model
  
  
def WritePartModel(part_model, path):
  with open(path, "w") as f:
    content = text_format.MessageToString(part_model)
    f.write(content)


def BuildPartModel(input_path, weight_files, output_path):
  part_model = ReadPartModel(input_path)
  print len(part_model.part)
  weight_maps = []
  for weight_file in weight_files:
    weight_maps.append(LoadCsvWeights(weight_file))
  for part in part_model.part:
    for weight_map in weight_maps:
      if part.id in weight_map:
        part.weight_grams = weight_map[part.id]
        break
  WritePartModel(part_model, output_path)


BuildPartModel('src/model/part-model-template.txt',
#               ['src/model/weights-master.csv'],
               ['src/model/weights-measured.csv',
                'src/model/weights-ldraw.csv',
                'src/model/weights-no-ldraw.csv'],
               'assets/part-model.txt')
