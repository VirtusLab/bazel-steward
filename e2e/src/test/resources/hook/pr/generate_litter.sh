#!/usr/bin/env sh
for dir in rubbish trash garbage
do
  mkdir $dir
  for i in 1 2 3 4 5
  do
    echo "$i" >$dir/file_$i.txt
  done
done
