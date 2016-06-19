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

"""
Scrapes available colors and number of lots for each color from BrickLink.

To run, you need to install a few things:
pip install BeautifulSoup
pip install joblib
+ the Google Protobuf library.

In addition, you need this:
export PYTHONPATH=gen/py/src/proto
"""

import re
import urllib

from bs4 import BeautifulSoup as BS
from google.protobuf import text_format
from joblib import Parallel, delayed

import part_model_proto_pb2


def ScrapePartAvailability(part_id):
  """Fetch available colors for a part."""
  parameters = { 'P' : part_id }
  url = "http://www.bricklink.com/v2/catalog/catalogitem.page?" + urllib.urlencode(parameters)
  html = urllib.urlopen(url).read()
  soup = BS(html, 'html.parser')

  def interesting_span(tag):
    if tag.name != 'span':
      return False
    if tag.a is None:
      return False
    return tag.a.has_attr('onclick')

  available_colors = {}
  for span in soup.find_all(interesting_span):
    color_match = re.search('\( *([-0-9]+) *\)', span.a['onclick'])
    if color_match is None:
      continue
    color = int(color_match.group(1))
    if color < 0:
      continue
    if span.a.next_sibling is None:
      continue
    num_stores_match = re.search('\( *([-0-9]+) *\)', span.a.next_sibling)
    available_colors[color] = int(num_stores_match.group(1))
  return available_colors


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


def WriteAvailableParts(available_parts, path):
  with open(path, 'w') as f:
    content = text_format.MessageToString(available_parts)
    f.write(content)


def FetchOne(part_ids):
  for part_id in part_ids:
    split_id = part_id.split(':')
    if split_id[0] in ['b', 'g', 'gb']:
      print 'Fetching %s ...' % split_id[1]
      colors = ScrapePartAvailability(split_id[1])
      return {
        'part_id': split_id[1],
        'colors': colors
      }
  return None


def FetchAllAvailableParts(part_model):
  part_ids = []
  for part in part_model.part:
    part_ids.append(list(part.id))
    for decor in part.decor:
      part_ids.append(list(decor.id))

  result = Parallel(n_jobs=10)(delayed(FetchOne)(id) for id in part_ids)
  available_parts = part_model_proto_pb2.AvailableParts()
  for a in result:
    if a is not None:
      colors = a['colors']
      if not colors:
        print 'Warning: No colors for part: %s' % a['part_id']
        continue
      available_part = available_parts.part.add()
      available_part.id = 'b:%s' % a['part_id']
      for color in sorted(colors):
        color_proto = available_part.color.add()
        color_proto.color = 'b:%s' % color
        color_proto.num_lots = colors[color]
  return available_parts


part_model = ReadPartModel('src/model/part-model-template.txt')
available_parts = FetchAllAvailableParts(part_model)
WriteAvailableParts(available_parts, 'src/model/available-parts.txt')
