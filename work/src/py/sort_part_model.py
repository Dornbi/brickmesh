#!/usr/bin/env python

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
Puts entries in part-model-template.txt in order.

Usage: sort_part_model <input-model1> [<input-model2> ...]

It rearranges "blocks" in the file. The lines of the block are preserved
exactly as they are, but the blocks are reordered according to the part
id that they describe. Blocks are separated by empty lines in the input.

The output is written to stdout.
"""

import re
import sys

def ReadOne(path, part_dict):
  line_count = 0
  lines = []
  part_id = None
  comment_only = True
  with open(path) as f:
    for line in f.readlines():
      line_count += 1
      if line.endswith('\n'):
        line = line[:-1]

      if line:
        lines.append(line)
        m = re.match("  id: *\"(.*)\" *", line)
        if m and not part_id:
          part_id = m.group(1)
        if not line.startswith('#'):
          comment_only = False
      else:
        if lines:
          if comment_only:
            part_id = '#'
          if not part_id:
            print 'No part_id found, file %s, line %d' % (path, line_count)
            return False
          if part_id in part_dict:
            print 'part_id "%s" is duplicate, file %s, line %d' % (part_id, path, line_count)
            return False
          part_dict[part_id] = lines
        part_id = None
        lines = []
        comment_only = True


  if lines:
    if comment_only:
      part_id = '#'
    if not part_id:
      print 'No part_id found, file %s, line %d' % (path, line_count)
      return False
    if part_id in part_dict:
      print 'part_id "%s" is duplicate, file %s, line %d' % (part_id, path, line_count)
    part_dict[part_id] = lines

  return True


def PartIdSortKey(part_id):
  if part_id == '#':
    return 'a'
  m = re.match('([a-z]+):([a-z]*)([0-9]+)(.*)', part_id)
  if m.group(2):
    return 'b%s%09d%s:%s' % (m.group(2), int(m.group(3)), m.group(4), m.group(1))
  else:
    return 'c%09d%s:%s' % (int(m.group(3)), m.group(4), m.group(1))


def PrintSorted(part_dict):
  for k in sorted(part_dict, key=PartIdSortKey):
    lines = part_dict[k]
    for line in lines:
      print line
    print


def main():
  part_dict = {}
  for path in sys.argv[1:]:
    if not ReadOne(path, part_dict):
      return
  PrintSorted(part_dict)

main()
