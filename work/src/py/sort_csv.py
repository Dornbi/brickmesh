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
Puts entries in a CSV in order.

Usage: sort_part_model <input-csv1> [<input-csv2> ...]

The output is written to stdout.
"""

import re
import sys

def ReadOne(path, line_dict):
  line_count = 0
  comment_lines = []
  with open(path) as f:
    for line in f.readlines():
      line_count += 1
      if line.endswith('\n'):
        line = line[:-1]
      if not line:
        continue
      if line.startswith('#'):
        comment_lines.append(line)
        continue
      if comment_lines:
        if '#' in line_dict:
          print 'Multiple comment blocks, file %s, line %d' % (path, line_count)
          return False
        line_dict['#'] = comment_lines
        comment_lines = []
      part_id = line.split(',')[0]
      if not part_id:
        print 'Unable to extract part_id, file %s, line %d' % (path, line_count)
        return False
      if part_id in line_dict:
        print 'part_id "%s" is duplicate, file %s, line %d' % (part_id, path, line_count)
        return False
      line_dict[part_id] = [line]

  if comment_lines:
    if '#' in line_dict:
      print 'Multiple comment blocks not allowed.'
      return False
    line_dict['#'] = comment_lines

  return True


def PartIdSortKey(part_id):
  if part_id == '#':
    return 'a'
  m = re.match('([a-z]*)([0-9]+)(.*)', part_id)
  if m.group(1):
    return 'b%s%09d%s' % (m.group(1), int(m.group(2)), m.group(3))
  else:
    return 'c%09d%s' % (int(m.group(2)), m.group(3))


def PrintSorted(part_dict):
  for k in sorted(part_dict, key=PartIdSortKey):
    lines = part_dict[k]
    for line in lines:
      print line

def main():
  line_dict = {}
  for path in sys.argv[1:]:
    if not ReadOne(path, line_dict):
      return
  PrintSorted(line_dict)
    
main()  
